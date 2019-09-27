import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.io.File

fun main(args: Array<String>) {
    val action = args[0]
    val mediaFile = File(args[1])
    val silenceReader = BuildSilenceReader.from(mediaFile)
    val sentences = FindSentences.from(silenceReader)
    when (action) {
        "clips" -> extractClipsToSplitsDir(sentences, mediaFile)
        "html" -> extractHtml(sentences, mediaFile)
        else -> TODO(action)
    }
    println("Min Duration: ${sentences.map { it.duration }.min().toPrint()}")
    println("Max Duration: ${sentences.map { it.duration }.max().toPrint()}")
    println("Count: ${sentences.size}")
}

val playerScript = """
    const player = document.getElementById('player');
    function play(index, startTime, endTime) {
        player.pause();
        player.currentTime = startTime;
        player.ontimeupdate = function() {
            let time = player.currentTime;
            if (time > endTime) {
                player.pause();
                player.currentTime = startTime;
            }
        };
        player.play();
        console.log('play', index);
    }
""".trimIndent()

private fun extractHtml(sentences: List<Sentence>, mediaFile: File) {
    val html = StringBuilder().appendHTML().html {
        body {
            h2 { +mediaFile.name }
            audio { id = "player"; src = mediaFile.name; controls = true }
            script {
                unsafe { raw(playerScript) }
            }
            sentences.forEachIndexed { index, sentence ->
                div {
                    p {
                        a {
                            href = index.toHref()
                            onClick = "play($index, ${sentence.start}, ${sentence.end})"
                            +"{ index = $index, start = ${sentence.start.format()}, end = ${sentence.end.format()}, duration = ${sentence.duration.format()} }"
                        }
                    }
                }
            }
        }
    }.toString()
    File("index.html").writeText(html)
}

private fun Int.toHref() = String.format("#sentence%03d", this)
private fun Double.format() = String.format("%.3f", this)

private fun extractClipsToSplitsDir(sentences: List<Sentence>, mediaFile: File) {
    val outDir = File("splits").apply {
        deleteRecursively()
        mkdirs()
    }
    extractClips(sentences, mediaFile, outDir)
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
