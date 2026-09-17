import sqlite3
import math
import requests
import time
import os

MIN_LON, MAX_LON = 32.1, 37.0
MIN_LAT, MAX_LAT = 43.85, 46.5

MIN_ZOOM = 7
MAX_ZOOM = 12

OUTPUT_FILE = "crimea.mbtiles"
USER_AGENT = "CrimeaAlarmApp/1.0 (personal use, low volume)"


def deg2num(lat_deg, lon_deg, zoom):
    lat_rad = math.radians(lat_deg)
    n = 2.0 ** zoom
    xtile = int((lon_deg + 180.0) / 360.0 * n)
    ytile = int((1.0 - math.asinh(math.tan(lat_rad)) / math.pi) / 2.0 * n)
    return xtile, ytile


def setup_db(path):
    is_new = not os.path.exists(path)
    conn = sqlite3.connect(path)
    cursor = conn.cursor()

    if is_new:
        cursor.execute("CREATE TABLE metadata (name TEXT, value TEXT)")
        cursor.execute("""
            CREATE TABLE tiles (
                zoom_level INTEGER,
                tile_column INTEGER,
                tile_row INTEGER,
                tile_data BLOB
            )
        """)
        cursor.execute("CREATE UNIQUE INDEX tile_index ON tiles (zoom_level, tile_column, tile_row)")

        metadata = [
            ("name", "Crimea"),
            ("type", "baselayer"),
            ("version", "1.0"),
            ("description", "Crimea offline map"),
            ("format", "png"),
            ("bounds", f"{MIN_LON},{MIN_LAT},{MAX_LON},{MAX_LAT}"),
            ("minzoom", str(MIN_ZOOM)),
            ("maxzoom", str(MAX_ZOOM)),
        ]
        cursor.executemany("INSERT INTO metadata VALUES (?, ?)", metadata)
        conn.commit()
    return conn


def tile_exists(cursor, z, x, tms_y):
    cursor.execute(
        "SELECT 1 FROM tiles WHERE zoom_level=? AND tile_column=? AND tile_row=?",
        (z, x, tms_y)
    )
    return cursor.fetchone() is not None


def download_tile(z, x, y, retries=3):
    subdomain = ["a", "b", "c"][x % 3]
    url = f"https://{subdomain}.tile.openstreetmap.org/{z}/{x}/{y}.png"
    headers = {"User-Agent": USER_AGENT}

    for attempt in range(retries):
        try:
            resp = requests.get(url, headers=headers, timeout=15)
            if resp.status_code == 200:
                return resp.content
            return None
        except Exception as e:
            if attempt < retries - 1:
                time.sleep(2)
                continue
            print(f"  Не удалось скачать {z}/{x}/{y}: {e}")
            return None


def main():
    conn = setup_db(OUTPUT_FILE)
    cursor = conn.cursor()

    total_downloaded = 0
    total_skipped = 0
    total_failed = 0

    for zoom in range(MIN_ZOOM, MAX_ZOOM + 1):
        x_min, y_max = deg2num(MIN_LAT, MIN_LON, zoom)
        x_max, y_min = deg2num(MAX_LAT, MAX_LON, zoom)

        tiles_at_zoom = (x_max - x_min + 1) * (y_max - y_min + 1)
        print(f"Zoom {zoom}: {tiles_at_zoom} тайлов (x: {x_min}-{x_max}, y: {y_min}-{y_max})")

        for x in range(x_min, x_max + 1):
            for y in range(y_min, y_max + 1):
                tms_y = (2 ** zoom - 1) - y

                if tile_exists(cursor, zoom, x, tms_y):
                    total_skipped += 1
                    continue

                data = download_tile(zoom, x, y)
                if data:
                    cursor.execute(
                        "INSERT INTO tiles (zoom_level, tile_column, tile_row, tile_data) VALUES (?, ?, ?, ?)",
                        (zoom, x, tms_y, data)
                    )
                    total_downloaded += 1
                else:
                    total_failed += 1

                if (total_downloaded + total_skipped) % 20 == 0:
                    conn.commit()
                    print(f"Скачано: {total_downloaded}, пропущено (уже было): {total_skipped}, ошибок: {total_failed}")

                time.sleep(0.3)

    conn.commit()
    conn.close()
    print(f"\nГотово! Скачано новых: {total_downloaded}, пропущено: {total_skipped}, ошибок: {total_failed}")
    print(f"Файл сохранён: {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
