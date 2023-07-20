package com.example.testaudio.services.translate

import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.testaudio.ui.ServiceManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TranslateCallbackHandler(_serviceManager: ServiceManager) : Callback<TranslateResponse?> {
    private val serviceManager: ServiceManager

    init {
        serviceManager = _serviceManager
    }

    override fun onResponse(
        call: Call<TranslateResponse?>,
        response: Response<TranslateResponse?>
    ) {
        if (response.isSuccessful) {
            val translateResponse: TranslateResponse? = response.body()
            if (translateResponse != null && translateResponse.translations.size == 1) {
                val translateResult = translateResponse.translations[0]
                val speechService = serviceManager.getTextToSpeechService()
                speechService.speak(translateResult.text, TextToSpeech.QUEUE_FLUSH, null, "")
            }
        } else {
            try {
                Log.e("Error happened ", response.errorBody()!!.string())
            } catch (e: java.lang.Exception) {
                Log.e("Error happened when try to get error body ", e.toString())
            }
        }
    }

    override fun onFailure(call: Call<TranslateResponse?>, t: Throwable) {
        Log.e("Call translate FAILED ", t.toString())
        call.cancel()
    }
}