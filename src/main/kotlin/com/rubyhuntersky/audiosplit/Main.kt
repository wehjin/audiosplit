package com.rubyhuntersky.audiosplit

import com.beust.klaxon.json
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.io.File

fun main(args: Array<String>) {
    val name = "audiosplit"
    val action = args.getOrNull(0)
    if (action == null) {
        println("Example usage:\n  $name <html/json/clips> example.mp3")
        return
    }
    val mediaFile = File(args[1])
    val silenceTime = args.getOrNull(2)?.toDoubleOrNull() ?: 0.420
    println("Silence time: $silenceTime seconds")
    val sentences = sentences(mediaFile, silenceTime)
    when (action) {
        "clips" -> extractClipsToSplitsDir(sentences, mediaFile)
        "html" -> extractHtml(sentences, mediaFile)
        "json" -> extractJson(sentences, mediaFile)
        else -> TODO(action)
    }
    println("Min Duration: ${sentences.map { it.duration }.min().toPrint()}")
    println("Max Duration: ${sentences.map { it.duration }.max().toPrint()}")
    println("Count: ${sentences.size}")
}

private fun sentences(mediaFile: File, duration: Double): List<Sentence> {
    return FindSentences.from(silenceReader(mediaFile, duration))
}

const val playerScript = """
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
"""

private fun extractJson(sentences: List<Sentence>, mediaFile: File) {
    val jsonArray = json {
        array(
            sentences.mapIndexed { index, sentence ->
                obj(
                    "index" to index,
                    "description" to "",
                    "start" to index,
                    "end" to index,
                    "start-time" to sentence.start.roundToSixDecimals(),
                    "end-time" to sentence.end.roundToSixDecimals(),
                    "duration" to sentence.duration.roundToSixDecimals(),
                    "kana" to "phrase$index",
                    "speaker" to "unknown",
                    "phrase" to index
                )
            }
        )
    }
    val json = jsonArray.toJsonString(prettyPrint = true)
    File("${mediaFile.name}.json").writeText(json)
}

private fun extractHtml(sentences: List<Sentence>, mediaFile: File) {
    val html = StringBuilder().appendHTML().html {
        body {
            h2 { +mediaFile.name }
            audio { id = "player"; src = mediaFile.name; controls = true }
            script {
                unsafe { raw(playerScript.trimIndent()) }
            }
            sentences.forEachIndexed { index, sentence ->
                val sentenceJson = json {
                    obj(
                        "index" to index,
                        "start" to sentence.start.roundToSixDecimals(),
                        "end" to sentence.end.roundToSixDecimals(),
                        "duration" to sentence.duration.roundToSixDecimals()
                    )
                }
                div {
                    p {
                        a {
                            href = index.toHref()
                            onClick = "play($index, ${sentence.start}, ${sentence.end})"
                            +sentenceJson.toJsonString(prettyPrint = true)
                        }
                    }
                }
            }
        }
    }.toString()
    File("${mediaFile.name}.html").writeText(html)
}

private fun Int.toHref() = String.format("#sentence%03d", this)
private fun Double.roundToSixDecimals() = String.format("%.6f", this).toDouble()

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
            "-metadata", "title=\"com.rubyhuntersky.audiosplit.Sentence $index\"",
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
