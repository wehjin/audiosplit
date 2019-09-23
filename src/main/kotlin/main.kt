import java.io.File


fun main(args: Array<String>) {
    val audioFile = File(args[0])
    val silenceFile = File(args[1])
    val sentences = FindSentences.from(silenceFile)
    val outDir = File("splits").apply {
        deleteRecursively()
        mkdirs()
    }
    val commands = sentences.mapIndexed { index, sentence ->
        val sentenceFilename = String.format("sentence%03d.mp3", index)
        val outFileSpec = File(outDir, sentenceFilename)
        arrayOf(
            "ffmpeg",
            "-i", audioFile.canonicalPath,
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
    println("Min Duration: ${sentences.map { it.duration }.min().toPrint()}")
    println("Max Duration: ${sentences.map { it.duration }.max().toPrint()}")
    println("Count: ${sentences.size}")
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
