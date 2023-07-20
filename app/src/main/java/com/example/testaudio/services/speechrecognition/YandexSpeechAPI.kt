package com.example.testaudio.services.speechrecognition

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query


interface YandexSpeechAPI {
    @POST("speech/v1/stt:recognize")
    fun speechRecognition(@Header("Authorization") authorizationToken: String,
                          @Query("topic") topic: String,
                          @Query("lang") language: String,
                          @Query("format") format: String,
                          @Query("folderId") folderId: String,
                          @Query("sampleRateHertz") sampleRateHertz: String,
                          @Body audioData: RequestBody
    ): Call<SpeechRecognitionResponse>?

}