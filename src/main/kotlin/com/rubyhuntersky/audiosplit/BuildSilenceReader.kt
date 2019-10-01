package com.rubyhuntersky.audiosplit

import java.io.File
import java.io.Reader

object BuildSilenceReader {
    fun from(mediaFile: File): Reader {
        val command = arrayOf(
            "ffmpeg",
            "-i", mediaFile.canonicalPath,
            "-af", "silencedetect=noise=0.05:d=0.420",
            "-f", "null",
            "-"
        )
        return Runtime.getRuntime().exec(command).errorStream.reader()
    }
}