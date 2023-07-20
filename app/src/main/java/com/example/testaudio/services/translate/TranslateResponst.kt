package com.example.testaudio.services.translate

import com.google.gson.annotations.SerializedName

class TranslateResult(
    @field:SerializedName("text") var text: String,
    @field:SerializedName("detectedLanguageCode") var detectedLanguageCode: String
)

class TranslateResponse {
    @field:SerializedName("translations")
    lateinit var translations: List<TranslateResult>
}