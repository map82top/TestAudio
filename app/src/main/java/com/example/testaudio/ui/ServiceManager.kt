package com.example.testaudio.ui

import android.speech.tts.TextToSpeech
import com.example.testaudio.repositories.AppPreferencesRepository
import com.example.testaudio.services.authenticator.IAMTokenResponse
import com.example.testaudio.services.authenticator.YandexAuthenticationAPI
import com.example.testaudio.services.speechrecognition.YandexSpeechAPI
import com.example.testaudio.services.translate.YandexTranslateAPI
import com.google.mlkit.nl.translate.Translator

interface ServiceManager {
    fun getYandexAuthenticationAPI(): YandexAuthenticationAPI
    fun getYandexSpeechAPI(): YandexSpeechAPI
    fun getYandexTranslateAPI(): YandexTranslateAPI
    fun getTextToSpeechService(): TextToSpeech
    fun saveIAMToken(iamToken: IAMTokenResponse)
    fun getAppPreferencesState(): AudioTranslatorPreferencesState
    fun getTranslator(): Translator
}