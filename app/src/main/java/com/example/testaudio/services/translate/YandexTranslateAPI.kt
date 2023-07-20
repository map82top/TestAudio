package com.example.testaudio.services.translate

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface YandexTranslateAPI {
    @Headers("Content-Type: application/json")
    @POST("translate/v2/translate")
    fun translate(@Header("Authorization") authorizationToken: String,
                          @Body translateData: TranslateRequest
    ): Call<TranslateResponse>?
}