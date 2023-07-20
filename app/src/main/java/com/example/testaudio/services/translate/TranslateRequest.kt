package com.example.testaudio.services.translate

import com.google.gson.annotations.SerializedName

class TranslateRequest(
    @field:SerializedName("targetLanguageCode") var targetLanguageCode: String,
    @field:SerializedName("sourceLanguageCode") var sourceLanguageCode: String,
    @field:SerializedName("folderId") var folderId: String,
    @field:SerializedName("texts") var texts: List<String>,
)