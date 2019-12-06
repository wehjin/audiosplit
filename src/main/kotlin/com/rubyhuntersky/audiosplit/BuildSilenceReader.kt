package com.rubyhuntersky.audiosplit

import java.io.File
import java.io.InputStreamReader
import java.io.Reader

fun silenceReader(mediaFile: File, duration: Double): InputStreamReader {
    val command = arrayOf(
        "ffmpeg",
        "-i", mediaFile.canonicalPath,
        "-af", "silencedetect=noise=0.05:d=$duration",
        "-f", "null",
        "-"
    )
    return Runtime.getRuntime().exec(command).errorStream.reader()
}
