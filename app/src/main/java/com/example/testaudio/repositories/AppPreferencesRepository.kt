package com.example.testaudio.repositories

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.testaudio.services.authenticator.IAMTokenResponse
import com.example.testaudio.services.authenticator.ServiceStaticCredentials
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.lang.Exception

class AppPreferencesRepository (
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val TARGET_LANGUAGE = stringPreferencesKey("target_language")
        val SOURCE_LANGUAGE = stringPreferencesKey("translate_language")
        val IAM_TOKEN = stringPreferencesKey("iam_token")
        val NO_SPEECH_FRAGMENTS_TO_SPEECH_PAUSE = stringPreferencesKey("no_speech_fragments_to_speech_pause")
        val NO_SPEECH_FRAGMENTS_TO_WORD_GAP = stringPreferencesKey("no_speech_fragments_to_word_gap")
        private const val TAG = "AppPreferencesRepo"
    }


    val getPreferences: Flow<Preferences> =  dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }

    val getTranslateLanguage: Flow<String> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[SOURCE_LANGUAGE] ?: ""
        }

    val getTargetLanguage: Flow<String> =  dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[TARGET_LANGUAGE] ?: ""
        }

    val getIAMToken: Flow<IAMTokenResponse> =  dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            val tokenJson = preferences[IAM_TOKEN] ?: return@map IAMTokenResponse("", "")
            Gson().fromJson(tokenJson, IAMTokenResponse::class.java)
        }

    suspend fun saveSourceLanguage(sourceLanguage: String) {
        dataStore.edit { preferences ->
            preferences[SOURCE_LANGUAGE] = sourceLanguage
        }
    }

    suspend fun saveTargetLanguage(targetLanguage: String) {
        dataStore.edit { preferences ->
            preferences[TARGET_LANGUAGE] = targetLanguage
        }
    }

    suspend fun saveIAMToken(iamToken: IAMTokenResponse) {
        dataStore.edit { preferences ->
            preferences[IAM_TOKEN] = Gson().toJson(iamToken)
        }
    }

    suspend fun saveNoSpeechFragmentsToSpeechPause(value: Int) {
        dataStore.edit { preferences ->
            preferences[NO_SPEECH_FRAGMENTS_TO_SPEECH_PAUSE] = value.toString()
        }
    }

    suspend fun saveNoSpeechFragmentsToWordGap(value: Int) {
        dataStore.edit { preferences ->
            preferences[NO_SPEECH_FRAGMENTS_TO_WORD_GAP] = value.toString()
        }
    }
}