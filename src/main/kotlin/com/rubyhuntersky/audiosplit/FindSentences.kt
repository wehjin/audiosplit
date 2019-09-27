package com.rubyhuntersky.audiosplit

import java.io.Reader
import kotlin.math.max

object FindSentences {
    private const val silenceEndLabel = "silence_end: "
    private const val silenceStartLabel = "silence_start: "
    private const val fuzz = 0.15

    fun from(reader: Reader): List<Sentence> {
        val (_, sentences) = reader.useLines { lines ->
            lines.fold(initial = init(), operation = FindSentences::update)
        }
        return sentences
    }

    private fun init() = Pair(Partial.None as Partial, emptyList<Sentence>())
    private fun update(progress: Pair<Partial, List<Sentence>>, line: String): Pair<Partial, List<Sentence>> {
        val (partial, sentences) = progress
        return when (partial) {
            is Partial.None -> {
                val silenceEndIndex = line.indexOf(silenceEndLabel)
                if (silenceEndIndex < 0) {
                    Pair(partial, sentences)
                } else {
                    val remaining = line.substring(silenceEndIndex + silenceEndLabel.length)
                    val parts = remaining.split(" ")
                    val endOfSilence = parts[0].trim().toDouble()
                    Pair(Partial.Started(max(endOfSilence - fuzz, 0.0)), sentences)
                }
            }
            is Partial.Started -> {
                val silenceStartIndex = line.indexOf(silenceStartLabel)
                if (silenceStartIndex < 0) {
                    Pair(partial, sentences)
                } else {
                    val remaining = line.substring(silenceStartIndex + silenceStartLabel.length)
                    val startOfSilence = remaining.trim().toDouble()
                    Pair(
                        Partial.None, sentences + Sentence(
                            partial.start,
                            startOfSilence + fuzz
                        )
                    )
                }
            }
        }
    }
}