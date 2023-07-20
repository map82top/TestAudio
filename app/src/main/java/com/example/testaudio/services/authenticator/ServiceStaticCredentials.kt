package com.example.testaudio.services.authenticator

import com.google.gson.annotations.SerializedName

data class ServiceStaticCredentials(
    @field:SerializedName("id") var keyId: String = "",
    @field:SerializedName("service_account_id") var serviceAccountId: String = "",
    @field:SerializedName("created_at") var createdAt: String = "",
    @field:SerializedName("key_algorithm") var keyAlgorithm: String = "",
    @field:SerializedName("public_key") var publicKey: String = "",
    @field:SerializedName("private_key") var privateKey: String = "",
    @field:SerializedName("folder_id") var folderId: String = ""
)