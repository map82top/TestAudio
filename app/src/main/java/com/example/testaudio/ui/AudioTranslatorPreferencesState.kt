package com.example.testaudio.ui

import com.example.testaudio.common.Languages
import com.example.testaudio.services.authenticator.IAMTokenResponse
import com.konovalov.vad.silero.config.FrameSize
import com.konovalov.vad.silero.config.SampleRate

data class AudioTranslatorPreferencesState(
    val sourceLanguage: Languages = Languages.EN,
    val targetLanguage: Languages = Languages.RU,
    val folderId: String = "",
    val iamToken: IAMTokenResponse = IAMTokenResponse("", ""),
    val recognitionFrameSize: FrameSize = FrameSize.FRAME_SIZE_512,
    val recognitionSampleRate: SampleRate = SampleRate.SAMPLE_RATE_16K,
    val noSpeechFragmentsToSpeechPause: Int = 15,
    val noSpeechFragmentsToWordGap: Int = 5,
)