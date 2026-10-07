package com.gatelms.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.ai.config.AiProperties
import com.gatelms.ai.exception.AiPayloadTooLargeException
import com.gatelms.ai.service.WhisperTranscriptionService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.client.RestClient
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WhisperTranscriptionServiceTest {

    private lateinit var aiProperties: AiProperties
    private lateinit var objectMapper: ObjectMapper
    private lateinit var service: WhisperTranscriptionService

    @BeforeEach
    fun setup() {
        aiProperties = AiProperties()
        aiProperties.providers.groq.apiKey = "mock-groq-key"
        aiProperties.whisper.localEndpoint = "http://localhost:8178/inference"
        objectMapper = ObjectMapper()
        service = WhisperTranscriptionService(aiProperties, objectMapper, RestClient.builder())
    }

    private fun generateWavBytes(durationSeconds: Double, sampleRate: Int = 16000, amplitude: Short = 5000): ByteArray {
        val totalSamples = (durationSeconds * sampleRate).toInt()
        val dataSize = totalSamples * 2
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)

        buffer.put("RIFF".toByteArray())
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16) // Subchunk1Size (16 for PCM)
        buffer.putShort(1) // AudioFormat (1 for PCM)
        buffer.putShort(1) // NumChannels (1 mono)
        buffer.putInt(sampleRate) // SampleRate
        buffer.putInt(sampleRate * 2) // ByteRate
        buffer.putShort(2) // BlockAlign
        buffer.putShort(16) // BitsPerSample
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)

        for (i in 0 until totalSamples) {
            val sample = if (i % 2 == 0) amplitude else (-amplitude).toShort()
            buffer.putShort(sample)
        }

        return buffer.array()
    }

    @Test
    fun `audio fragment gate rejects audio shorter than 1_2 seconds`() {
        val shortWav = generateWavBytes(durationSeconds = 0.5)
        val file = MockMultipartFile("file", "short.wav", "audio/wav", shortWav)

        val result = service.transcribeAudio(file)
        assertEquals("TOO_SHORT", result.status)
        assertTrue(result.message!!.contains("too short"))
        assertEquals("", result.text)
    }

    @Test
    fun `audio fragment gate rejects completely silent audio below RMS floor`() {
        val silentWav = generateWavBytes(durationSeconds = 2.0, amplitude = 0)
        val file = MockMultipartFile("file", "silent.wav", "audio/wav", silentWav)

        val result = service.transcribeAudio(file)
        assertEquals("TOO_SHORT", result.status)
        assertTrue(result.message!!.contains("too short"))
    }

    @Test
    fun `audio fragment gate rejects tiny webm files below 12000 bytes`() {
        val tinyWebm = ByteArray(500) { 1 }
        val file = MockMultipartFile("file", "tiny.webm", "audio/webm", tinyWebm)

        val result = service.transcribeAudio(file)
        assertEquals("TOO_SHORT", result.status)
    }

    @Test
    fun `valid audio fragment passes gating check`() {
        val validWav = generateWavBytes(durationSeconds = 1.5, amplitude = 4000)
        val gateCheck = service.checkAudioFragment(validWav)
        assertNull(gateCheck, "Valid WAV audio should pass the gate check")
    }

    @Test
    fun `audio fragment gate rejects audio exceeding 120 seconds ceiling with AiPayloadTooLargeException`() {
        val oversizedWav = generateWavBytes(durationSeconds = 125.0, amplitude = 4000)
        assertThrows(AiPayloadTooLargeException::class.java) {
            service.checkAudioFragment(oversizedWav)
        }
    }
}
