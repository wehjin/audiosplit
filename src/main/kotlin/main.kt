import java.io.File


fun main(args: Array<String>) {
    val action = args[0]
    val audioFile = File(args[1])
    val silenceFile = File(args[2])
    val sentences = FindSentences.from(silenceFile)
    when (action) {
        "extract" -> {
            val outDir = File("splits").apply {
                deleteRecursively()
                mkdirs()
            }
            extractClips(sentences, audioFile, outDir)
        }
    }
    println("Min Duration: ${sentences.map { it.duration }.min().toPrint()}")
    println("Max Duration: ${sentences.map { it.duration }.max().toPrint()}")
    println("Count: ${sentences.size}")
}

private fun extractClips(sentences: List<Sentence>, mediaFile: File, clipDir: File) {
    val commands = sentences.mapIndexed { index, sentence ->
        val sentenceFilename = String.format("sentence%03d.mp3", index)
        val outFileSpec = File(clipDir, sentenceFilename)
        arrayOf(
            "ffmpeg",
            "-i", mediaFile.canonicalPath,
            "-metadata", "title=\"Sentence $index\"",
            "-ss", "${sentence.start}",
            "-to", " ${sentence.end}",
            "-c", "copy", outFileSpec.path
        )
    }
    commands.forEach { commandArray ->
        println(commandArray)
        val process = Runtime.getRuntime().exec(commandArray)
        process.errorStream.use {
            println(it.reader().readText())
        }
    }
}

private fun Double?.toPrint(): String = String.format("%.04fs", this)

data class Sentence(
    val start: Double,
    val end: Double
) {
    val duration
        get() = end - start
}

sealed class Partial {
    object None : Partial()
    data class Started(val start: Double) : Partial()
}
