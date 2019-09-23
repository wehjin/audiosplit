import java.io.File


fun main(args: Array<String>) {
    val audioFile = File(args[0])
    val silenceFile = File(args[1])
    val sentences = FindSentences.from(silenceFile)
    val commands = sentences.mapIndexed { index, sentence ->
        val sentenceFilename = String.format("sentence%03d", index)
        val printDuration = sentence.duration.toPrint()
        "ffmpeg -i ${audioFile.canonicalPath} -ss ${sentence.start} -to ${sentence.end} -c copy $sentenceFilename.mp3 # $printDuration"
    }
    commands.forEach(::println)
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
