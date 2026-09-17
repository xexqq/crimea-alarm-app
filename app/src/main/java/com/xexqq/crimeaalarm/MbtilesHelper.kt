package com.xexqq.crimeaalarm

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object MbtilesHelper {

    fun getMbtilesPath(context: Context): String {
        val outFile = File(context.filesDir, "crimea.mbtiles")

        if (!outFile.exists()) {
            context.assets.open("crimea.mbtiles").use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        }

        return outFile.absolutePath
    }
}
