package me.yuriisoft.buildnotify.mobile.network.error

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ErrorMappingTest {

    @Test
    fun returnsFirstMatchingRecognizer() {
        val recognizer = ErrorRecognizer { throwable ->
            if (throwable is IllegalStateException) ConnectionErrorReason.Refused("matched")
            else null
        }
        val mapping = ErrorMapping(listOf(recognizer))

        val result = mapping.map(IllegalStateException("boom"))

        assertIs<ConnectionErrorReason.Refused>(result)
        assertEquals("matched", result.message)
    }

    @Test
    fun skipsNonMatchingRecognizersAndUsesFirstMatch() {
        val never = ErrorRecognizer { null }
        val match = ErrorRecognizer { ConnectionErrorReason.Timeout("found") }
        val mapping = ErrorMapping(listOf(never, match))

        val result = mapping.map(RuntimeException("any"))

        assertIs<ConnectionErrorReason.Timeout>(result)
        assertEquals("found", result.message)
    }

    @Test
    fun returnsUnknownWhenNoRecognizerMatches() {
        val mapping = ErrorMapping(listOf(ErrorRecognizer { null }))

        val result = mapping.map(RuntimeException("something failed"))

        assertIs<ConnectionErrorReason.Unknown>(result)
        assertEquals("something failed", result.message)
    }

    @Test
    fun returnsUnknownWithDefaultMessageWhenThrowableHasNoMessage() {
        val mapping = ErrorMapping(emptyList())

        val result = mapping.map(RuntimeException())

        assertIs<ConnectionErrorReason.Unknown>(result)
        assertEquals("Connection failed", result.message)
    }

    @Test
    fun firstMatchWinsWhenMultipleRecognizersCouldMatch() {
        val first = ErrorRecognizer { ConnectionErrorReason.Refused("first") }
        val second = ErrorRecognizer { ConnectionErrorReason.Lost("second") }
        val mapping = ErrorMapping(listOf(first, second))

        val result = mapping.map(RuntimeException())

        assertIs<ConnectionErrorReason.Refused>(result)
        assertEquals("first", result.message)
    }

    @Test
    fun emptyRecognizerListFallsBackToUnknown() {
        val mapping = ErrorMapping(emptyList())

        val result = mapping.map(IllegalArgumentException("bad arg"))

        assertIs<ConnectionErrorReason.Unknown>(result)
        assertEquals("bad arg", result.message)
    }
}
