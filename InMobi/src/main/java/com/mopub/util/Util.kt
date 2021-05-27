package com.mopub.util

import android.content.Context
import java.io.IOException
import java.io.InputStream

fun Context.loadJSONFromAsset(fileName: String): ByteArray? {
    return try {
        val inputStream: InputStream = assets.open(fileName)
        val size: Int = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        buffer
//        String(buffer, Charset.forName("UTF-8"))
    } catch (ex: IOException) {
        ex.printStackTrace()
        null
    }
}