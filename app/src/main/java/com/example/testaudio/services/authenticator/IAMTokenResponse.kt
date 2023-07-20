package com.example.testaudio.services.authenticator;

import com.google.gson.annotations.SerializedName;

class IAMTokenResponse(
    @field:SerializedName("iamToken") var iamToken: String,
    @field:SerializedName("expiresAt") var expiresAt: String
)