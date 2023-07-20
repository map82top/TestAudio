package com.example.testaudio.services.authenticator

import com.google.gson.annotations.SerializedName

class IAMTokenRequest(
    @field:SerializedName("jwt") var jwt: String,
)