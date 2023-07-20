package com.example.testaudio.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.animation.core.FloatTweenSpec
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.testaudio.TestAudioApplication
import com.example.testaudio.common.Languages
import com.example.testaudio.repositories.AppPreferencesRepository
import com.example.testaudio.services.authenticator.IAMTokenRequest
import com.example.testaudio.services.authenticator.IAMTokenRequestCallbackHandler
import com.example.testaudio.services.authenticator.IAMTokenResponse
import com.example.testaudio.services.authenticator.ServiceStaticCredentials
import com.example.testaudio.services.authenticator.YandexAuthenticationAPI
import com.example.testaudio.services.speechrecognition.SpeechRecognitionCallbackHandler
import com.example.testaudio.services.speechrecognition.YandexSpeechAPI
import com.example.testaudio.services.translate.YandexTranslateAPI
import com.google.gson.Gson
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.konovalov.vad.silero.Vad
import com.konovalov.vad.silero.VadSilero
import com.konovalov.vad.silero.config.Mode
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.RequestBody
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemReader
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Collections
import java.util.Date
import java.util.Locale


class AudioTranslatorViewModel(
    private val application: TestAudioApplication
) : ViewModel(), ServiceManager {

    private val _uiState = MutableStateFlow(AudioTranslatorUIState())
    val uiState: StateFlow<AudioTranslatorUIState> = _uiState.asStateFlow()
    val preferencesState: StateFlow<AudioTranslatorPreferencesState>
    private var audioRecorder: AudioRecord? = null
    private var isRecording: Boolean = false
    private var recordingThread: Thread? = null
    private var bufferSizeInBytes: Int = 0
    private var yandexAuthenticationAPI: YandexAuthenticationAPI
    private var yandexSpeechAPI: YandexSpeechAPI
    private var yandexTranslateAPI: YandexTranslateAPI
    private var textToSpeech: TextToSpeech
    private var appPreferencesRepository: AppPreferencesRepository
    private var translator: Translator? = null
    private var speechAnalyzer: SpeechAnalyzer

    init {
        yandexAuthenticationAPI = createYandexAuthenticationAPI()
        yandexSpeechAPI = createYandexSpeechAPI()
        yandexTranslateAPI = createTranslateSpeechAPI()
        textToSpeech = TextToSpeech(application, this::onInit)
        appPreferencesRepository = application.appPreferencesRepository
        preferencesState = appPreferencesRepository.getPreferences.map { preferences ->
            val iamTokenJsonString = preferences[AppPreferencesRepository.IAM_TOKEN]
            var iamToken = IAMTokenResponse("", "")
            if (iamTokenJsonString != null) {
                iamToken = Gson().fromJson(iamTokenJsonString, IAMTokenResponse::class.java)
            }
            if (iamToken.iamToken.isEmpty() || isOldToken(iamToken)) {
                requestIAMToken()
            }

            val authorizedKey = getAuthorizedKey()
            if (!uiState.value.isLoaded) {
                updateLoadedState(true)
            }
            AudioTranslatorPreferencesState(
                sourceLanguage = Languages.valueOf(preferences[AppPreferencesRepository.SOURCE_LANGUAGE] ?: Languages.EN.name),
                targetLanguage = Languages.valueOf(preferences[AppPreferencesRepository.TARGET_LANGUAGE] ?: Languages.RU.name),
                iamToken = iamToken,
                folderId = authorizedKey?.folderId ?: "",
                noSpeechFragmentsToSpeechPause = preferences[AppPreferencesRepository.NO_SPEECH_FRAGMENTS_TO_SPEECH_PAUSE]?.toInt() ?: 15,
                noSpeechFragmentsToWordGap = preferences[AppPreferencesRepository.NO_SPEECH_FRAGMENTS_TO_WORD_GAP]?.toInt() ?: 5,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AudioTranslatorPreferencesState()
        )
        speechAnalyzer = SpeechAnalyzer(preferencesState, application)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as TestAudioApplication)
                AudioTranslatorViewModel(application)
            }
        }
    }

    private fun requestIAMToken() {
        val authorizedKey = getAuthorizedKey() ?: return
        try {
            val privateKey = encryptPrivateKey(authorizedKey.privateKey)
            val requestTime = Instant.now()
            val jwtToken: String = Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setHeaderParam("kid", authorizedKey.keyId)
                .setIssuer(authorizedKey.serviceAccountId)
                .setAudience("https://iam.api.cloud.yandex.net/iam/v1/tokens")
                .setIssuedAt(Date.from(requestTime))
                .setExpiration(Date.from(requestTime.plusSeconds(360)))
                .signWith(privateKey, SignatureAlgorithm.PS256)
                .compact()

            val requestIAMToken = yandexAuthenticationAPI.requestIAMToken(IAMTokenRequest(jwtToken))
            requestIAMToken?.enqueue(IAMTokenRequestCallbackHandler(this))
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun getAuthorizedKey(): ServiceStaticCredentials? {
        var authorizedKeyJsonString: String?
        try {
            val inputStream = application.assets.open("services_static_credentials.json")
            authorizedKeyJsonString = inputStream.bufferedReader().use{it.readText()}
        } catch (ex: Exception) {
            ex.printStackTrace()
            return null
        }
        return Gson().fromJson(authorizedKeyJsonString, ServiceStaticCredentials::class.java)
    }

    private fun encryptPrivateKey(privateKey: String): PrivateKey {
        var privateKeyPem: PemObject
        val keyStream = ByteArrayInputStream(privateKey.toByteArray())
        PemReader(InputStreamReader(keyStream)).use { reader ->
            privateKeyPem = reader.readPemObject()
        }

        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateKeyPem.getContent()))
    }

    private fun isOldToken(iamToken: IAMTokenResponse): Boolean {
        if (iamToken.expiresAt.isEmpty()) {
            return true
        }
        val expiresAt = Instant.parse(iamToken.expiresAt)
        val requestNewTime = expiresAt.minusSeconds(60 * 60 * 10)
        return requestNewTime.isBefore(Instant.now())
    }

    fun selectSourceLanguage(sourceLanguage: String) {
        viewModelScope.launch {
            appPreferencesRepository.saveSourceLanguage(sourceLanguage)
        }
    }

    fun selectTargetLanguage(targetLanguage: String) {
        viewModelScope.launch {
            appPreferencesRepository.saveTargetLanguage(targetLanguage)
        }
    }

    fun changeNoSpeechFragmentsToSpeechPause(value: Float) {
        viewModelScope.launch {
            appPreferencesRepository.saveNoSpeechFragmentsToSpeechPause(value.toInt())
        }
        speechAnalyzer.updateNoSpeechFragmentsToSpeechPause(value.toInt())
    }

    fun changeNoSpeechFragmentsToWordGap(value: Float) {
        viewModelScope.launch {
            appPreferencesRepository.saveNoSpeechFragmentsToWordGap(value.toInt())
        }
        speechAnalyzer.updateNoSpeechFragmentsToWordGap(value.toInt())
    }

    private fun createYandexAuthenticationAPI() : YandexAuthenticationAPI {
        val retrofitServiceBuilder = Retrofit.Builder()
            .baseUrl("https://iam.api.cloud.yandex.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofitServiceBuilder.create(YandexAuthenticationAPI::class.java)
    }

    private fun createYandexSpeechAPI() : YandexSpeechAPI {
        val retrofitServiceBuilder = Retrofit.Builder()
            .baseUrl("https://stt.api.cloud.yandex.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofitServiceBuilder.create(YandexSpeechAPI::class.java)
    }

    private fun createTranslateSpeechAPI() : YandexTranslateAPI {
        val retrofitServiceBuilder = Retrofit.Builder()
            .baseUrl("https://translate.api.cloud.yandex.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofitServiceBuilder.create(YandexTranslateAPI::class.java)
    }

    private fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language not supported!")
            }
        }
    }

    fun handleChangeTranslatingStateClick() {
        val isActiveNewState = !uiState.value.isActive
        updateAppState(isActiveNewState)
        if (isActiveNewState) {
            startTranslating()
        } else {
            stopTranslating()
        }
    }

    private fun startTranslating() {
        val sampleRateInHz = preferencesState.value.recognitionSampleRate.value
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(convertToTranslateLanguage(preferencesState.value.sourceLanguage))
            .setTargetLanguage(convertToTranslateLanguage(preferencesState.value.targetLanguage))
            .build()
        translator = Translation.getClient(options)
//
        var conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        translator!!.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {

            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
            }

        try {
            bufferSizeInBytes = preferencesState.value.recognitionFrameSize.value * 2
            if (ActivityCompat.checkSelfPermission(application, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                audioRecorder = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    sampleRateInHz,
                    channelConfig,
                    audioFormat,
                    bufferSizeInBytes
                )
            }

            audioRecorder!!.startRecording()
            isRecording = true
            recordingThread = Thread({ audioDataWorker() }, "AudioRecorder Thread")
            recordingThread!!.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun convertToTranslateLanguage(language: Languages): String {
        return when(language) {
            Languages.EN -> TranslateLanguage.ENGLISH
            Languages.RU -> TranslateLanguage.RUSSIAN
            else -> {
                throw Exception("Unknown language type")
            }
        }
    }

    private fun audioDataWorker() {
        val audioFragment = ByteArray(bufferSizeInBytes)
        speechAnalyzer.updateNoSpeechFragmentsToSpeechPause(preferencesState.value.noSpeechFragmentsToSpeechPause)
        speechAnalyzer.updateNoSpeechFragmentsToWordGap(preferencesState.value.noSpeechFragmentsToWordGap)

//        val audioTrack = AudioTrack(
//            AudioManager.STREAM_MUSIC,
//            preferencesState.value.recognitionSampleRate.value,
//            AudioFormat.CHANNEL_OUT_MONO,
//            AudioFormat.ENCODING_PCM_16BIT,
//            bufferSizeInBytes,
//            AudioTrack.MODE_STREAM
//        )

//        audioTrack.play()

        try {
            while (isRecording) {
                val bytesRead = audioRecorder!!.read(audioFragment, 0, bufferSizeInBytes);
                if (bytesRead == AudioRecord.ERROR_BAD_VALUE || bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                    isRecording = false
                    break
                }

                val audioData = speechAnalyzer.processNewAudioFragment(audioFragment)

                if (audioData != null) {
//                    audioTrack.write(audioData, 0, audioData.size)
                    val iamToken = preferencesState.value.iamToken.iamToken
                    val authorizationToken = "Bearer $iamToken"
                    val folderId = preferencesState.value.folderId
                    val sourceLanguageCode = preferencesState.value.sourceLanguage.speechRecognitionCode
                    val requestAudioData = RequestBody.create(MediaType.parse("audio/*"), audioData)
                    val speechRecognition = yandexSpeechAPI.speechRecognition(
                        authorizationToken,
                        "general",
                        sourceLanguageCode,
                        "lpcm",
                        folderId,
                        "16000",
                        requestAudioData
                    )
                    speechRecognition?.enqueue(SpeechRecognitionCallbackHandler(this))
                }
            }
        } catch (e: Exception) {
            val a = e.stackTraceToString()
            e.printStackTrace()
        } finally {
            speechAnalyzer.destroy()
        }
    }

    private fun stopTranslating() {
        translator?.close()
        audioRecorder?.stop()
        audioRecorder?.release()
        isRecording = false
    }


    fun updateRecordAudioGranted(permissionGranted: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                isAllRequiredPermissionsGranted = permissionGranted
            )
        }
    }

    fun updateAppState(isActive: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                isActive = isActive
            )
        }
    }

    private fun updateLoadedState(isLoaded: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                isLoaded = isLoaded
            )
        }
    }

    override fun getYandexAuthenticationAPI(): YandexAuthenticationAPI {
        return yandexAuthenticationAPI
    }

    override fun getYandexSpeechAPI(): YandexSpeechAPI {
        return yandexSpeechAPI
    }

    override fun getYandexTranslateAPI(): YandexTranslateAPI {
        return yandexTranslateAPI
    }

    override fun getTextToSpeechService(): TextToSpeech {
       return textToSpeech
    }

    override fun getAppPreferencesState(): AudioTranslatorPreferencesState {
        return preferencesState.value
    }

    override fun getTranslator(): Translator {
        return translator!!
    }

    override fun saveIAMToken(iamToken: IAMTokenResponse) {
        viewModelScope.launch {
            appPreferencesRepository.saveIAMToken(iamToken)
        }
    }
}

class SpeechAnalyzer(
    private val preferencesState: StateFlow<AudioTranslatorPreferencesState>,
    private val application: TestAudioApplication
) {
    private val statistic: SpeechStatistic
    private val silenceDetector: VadSilero
    private var speechBuffer: SpeechBuffer
    private val maxAudioBufferLen: Int
    private val activatePauseOnNearestWordLen: Int


    init {
        val bytesPerSecond = preferencesState.value.recognitionSampleRate.value
        maxAudioBufferLen = bytesPerSecond * 30
        activatePauseOnNearestWordLen = bytesPerSecond* 10

        speechBuffer = SpeechBuffer(maxAudioBufferLen)
        silenceDetector = Vad.builder()
            .setContext(application)
            .setSampleRate(preferencesState.value.recognitionSampleRate)
            .setFrameSize(preferencesState.value.recognitionFrameSize)
            .setMode(Mode.VERY_AGGRESSIVE)
            .setSilenceDurationMs(100)
            .build()

        statistic = SpeechStatistic(
            preferencesState.value.noSpeechFragmentsToWordGap,
            preferencesState.value.noSpeechFragmentsToSpeechPause
        )
    }

    fun processNewAudioFragment(audioFragment: ByteArray): ByteArray? {
        var speechFragment: ByteArray? = null
        var shortStageArray = ShortArray(preferencesState.value.recognitionFrameSize.value)
        ByteBuffer.wrap(audioFragment).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortStageArray)
        val isSpeech = silenceDetector.isSpeech(shortStageArray)

        if (speechBuffer.getFreeSpace() < audioFragment.size) {
            Log.println(Log.INFO, "Sound Recognition", "EMERGENCY CLEANING OF BUFFER $statistic")
            speechFragment = speechBuffer.getSpeech()
            statistic.clearAll()
        }

        if (speechBuffer.getFilledSpaced() > activatePauseOnNearestWordLen && statistic.hasWordGap()) {
            Log.println(Log.INFO, "Sound Recognition", "WORD GAP $statistic")
            speechFragment = speechBuffer.getSpeech()
            statistic.clearAll()
        }

        if (isSpeech) {
            if (statistic.noSpeechFragmentsSequence != 0) {
                statistic.clearNoSpeechFragmentsCounter()
            }
            speechBuffer.addNewFragment(audioFragment)
            statistic.newSpeechFragment()

        } else {
            // save gap between words
            if (statistic.speechFragments != 0 && statistic.noSpeechFragmentsSequence < 6) {
                speechBuffer.addNewFragment(audioFragment)
            }
            statistic.newNoSpeechFragment()
            if (statistic.hasSpeechPause() && !speechBuffer.isEmpty()) {
                if (statistic.noSpeechFragmentsSequence == speechBuffer.totalFragments) {
                    statistic.clearAll()
                } else {
                    speechFragment = speechBuffer.getSpeech()
                    Log.println(Log.INFO, "Sound Recognition", "SPEECH PAUSE $statistic")
                    statistic.clearAll()
                }
            }
        }

        return if (speechFragment != null && speechFragment.size > 20) {
            speechFragment
        } else {
            null
        }
    }

    fun updateNoSpeechFragmentsToSpeechPause(newValue: Int) {
        statistic.maxNoSpeechFragmentsToSpeechPause = newValue
    }

    fun updateNoSpeechFragmentsToWordGap(newValue: Int) {
        statistic.maxNoSpeechFragmentsToWordGap = newValue
    }

    fun destroy() {
        silenceDetector.close()
    }
}

class SpeechStatistic(
    var maxNoSpeechFragmentsToWordGap: Int = 5,
    var maxNoSpeechFragmentsToSpeechPause: Int = 15
) {
    var speechFragments: Int = 0
        private set

    var noSpeechFragmentsSequence: Int = 0
        private set

    var noSpeechSequences = ArrayList<Int>()
        private set

    fun newSpeechFragment() {
        speechFragments++
    }

    fun newNoSpeechFragment() {
        noSpeechFragmentsSequence++
    }

    fun clearAll() {
        speechFragments = 0
        noSpeechFragmentsSequence = 0
        noSpeechSequences.clear()
    }

    fun clearNoSpeechFragmentsCounter() {
        noSpeechSequences.add(noSpeechFragmentsSequence)
        noSpeechFragmentsSequence = 0
    }

    fun hasWordGap(): Boolean {
        return maxNoSpeechFragmentsToWordGap < noSpeechFragmentsSequence
    }

    fun hasSpeechPause(): Boolean {
        return maxNoSpeechFragmentsToSpeechPause < noSpeechFragmentsSequence
    }

    override fun toString(): String {
        return StringBuilder("Speech Statistic ")
            .append("Speech fragments: $speechFragments ")
            .append("Last no-speech fragment length: $$noSpeechFragmentsSequence ")
            .append("All no-speech sequences $noSpeechSequences ")
            .append("No-speech median ${noSpeechSequences.average()} ")
            .toString()
    }
}

class SpeechBuffer(private val maxAudioBufferLen: Int) {
    private var speechBuffer = ByteBuffer.allocate(maxAudioBufferLen)
    var totalFragments = 0
        private set

    fun getSpeech(): ByteArray {
        val fragmentLen = speechBuffer.position()
        val speechFragment = ByteArray(fragmentLen)
        speechBuffer.rewind()
        speechBuffer.get(speechFragment, 0, fragmentLen)
        speechBuffer.clear()
        totalFragments = 0
        return speechFragment
    }

    fun addNewFragment(audioFragment: ByteArray) {
        speechBuffer.put(audioFragment)
        totalFragments++
    }

    fun clear() {
        speechBuffer.clear()
        totalFragments = 0
    }

    fun getFilledSpaced(): Int {
        return speechBuffer.position()
    }

    fun getFreeSpace(): Int {
        return maxAudioBufferLen - speechBuffer.position()
    }

    fun isEmpty(): Boolean {
        return speechBuffer.position() == 0
    }

}