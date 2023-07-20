package com.example.testaudio.services.authenticator

import com.example.testaudio.services.speechrecognition.SpeechRecognitionResponse
import com.example.testaudio.services.translate.TranslateRequest
import com.example.testaudio.services.translate.TranslateResponse
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface YandexAuthenticationAPI {
    @Headers("Content-Type: application/json")
    @POST("iam/v1/tokens")
    fun requestIAMToken(@Body request: IAMTokenRequest): Call<IAMTokenResponse>?
}