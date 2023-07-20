package com.example.testaudio.services.speechrecognition

import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.testaudio.services.translate.TranslateCallbackHandler
import com.example.testaudio.services.translate.TranslateRequest
import com.example.testaudio.ui.ServiceManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SpeechRecognitionCallbackHandler(_serviceManager: ServiceManager) : Callback<SpeechRecognitionResponse?> {
    private val serviceManager: ServiceManager

    init {
        serviceManager = _serviceManager
    }

    override fun onResponse(
        call: Call<SpeechRecognitionResponse?>,
        response: Response<SpeechRecognitionResponse?>
    ) {
        if (response.isSuccessful) {
            val recognitionResponse: SpeechRecognitionResponse? = response.body()
            if (recognitionResponse != null) {
                Log.println(Log.INFO, "Sound Recognition", "Response received. Size: ${recognitionResponse!!.result.trim()}")
            } else {
                Log.println(Log.INFO, "Sound Recognition", "Response is empty.")
            }

            if (recognitionResponse != null && recognitionResponse.result.trim().isNotEmpty()) {
//                val translateService = serviceManager.getYandexTranslateAPI()
//                val appPreferencesState = serviceManager.getAppPreferencesState()
//                val iamToken = appPreferencesState.iamToken.iamToken
//                val authorizationToken = "Bearer $iamToken"
//                val translate = translateService.translate(
//                    authorizationToken,
//                    TranslateRequest(
//                        appPreferencesState.targetLanguage.translateCode,
//                        appPreferencesState.sourceLanguage.translateCode,
//                        appPreferencesState.folderId,
//                        listOf(recognitionResponse.result)
//                    )
//                )
//                translate?.enqueue(TranslateCallbackHandler(serviceManager))

                fun translateWorker() {
                    val translator = serviceManager.getTranslator()
                    translator.translate(recognitionResponse.result)
                    .addOnSuccessListener { translatedText ->
                        Log.println(Log.INFO, "Sound Recognition", "TranslatedText $translatedText")
                        val speechService = serviceManager.getTextToSpeechService()
                        speechService.speak(translatedText, TextToSpeech.QUEUE_FLUSH, null, "")
                    }
                    .addOnFailureListener { exception ->
                        exception.printStackTrace()
                    }
                }

                val recordingThread = Thread({ translateWorker() }, "Translator Thread")
                recordingThread.start()
            }
        } else {
            try {
                Log.e("Error happened ", response.errorBody()!!.string())
            } catch (e: java.lang.Exception) {
                Log.e("Error happened when try to get error body ", e.toString())
            }
        }
    }

    override fun onFailure(call: Call<SpeechRecognitionResponse?>, t: Throwable) {
        call.cancel()
        Log.e("Call speech recognition FAILED ", t.toString())
    }
}