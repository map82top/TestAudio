package com.example.testaudio.services.authenticator

import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.testaudio.ui.ServiceManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.coroutines.launch

class IAMTokenRequestCallbackHandler(_serviceManager: ServiceManager) : Callback<IAMTokenResponse?> {
    private val serviceManager: ServiceManager

    init {
        serviceManager = _serviceManager
    }

    override fun onResponse(
        call: Call<IAMTokenResponse?>,
        response: Response<IAMTokenResponse?>
    ) {
        if (response.isSuccessful) {
            val iamToken: IAMTokenResponse = response.body() ?: return
            serviceManager.saveIAMToken(iamToken)
        } else {
            try {
                Log.e("Error happened ", response.errorBody()!!.string())
            } catch (e: java.lang.Exception) {
                Log.e("Error happened when try to get error body ", e.toString())
            }
        }
    }

    override fun onFailure(call: Call<IAMTokenResponse?>, t: Throwable) {
        Log.e("Call request IAM token FAILED ", t.toString())
        call.cancel()
    }
}