package com.example.testaudio.ui.theme

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testaudio.common.Languages
import com.example.testaudio.ui.AudioTranslatorViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTranslatorScreen(
    viewModel: AudioTranslatorViewModel = viewModel(
        factory = AudioTranslatorViewModel.Factory
    )
) {
    val buttonModifier: Modifier = Modifier
        .width(200.dp)
        .padding(8.dp)

    val uiState by viewModel.uiState.collectAsState()
    val preferencesState by viewModel.preferencesState.collectAsState()

    val permissionsRequired = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE
    )
    val askPermissions = arrayListOf<String>()
    val permissionsLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) { permissionsMap ->
            // if the map does NOT contain false,
            // all the permissions are granted
            viewModel.updateRecordAudioGranted(!permissionsMap.containsValue(false))
        }

    for (permission in permissionsRequired) {
        if (ActivityCompat.checkSelfPermission(LocalContext.current, permission) == PackageManager.PERMISSION_DENIED) {
            askPermissions.add(permission)
        }
    }
    viewModel.updateRecordAudioGranted(askPermissions.isEmpty())

    var isSourceLanguageSelectorExpanded by remember {
        mutableStateOf(false)
    }

    var isTargetLanguageSelectorExpanded by remember {
        mutableStateOf(false)
    }

    var sliderPosition by remember { mutableStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExposedDropdownMenuBox(
                expanded = isSourceLanguageSelectorExpanded,
                onExpandedChange = { isSourceLanguageSelectorExpanded = it },
                modifier = Modifier.width(150.dp)
            ) {
                TextField(
                    value = preferencesState.sourceLanguage.displayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSourceLanguageSelectorExpanded)
                    },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier.menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = isSourceLanguageSelectorExpanded,
                    onDismissRequest = { isSourceLanguageSelectorExpanded = false }
                ) {
                    Languages.values().map {
                        DropdownMenuItem(
                            text = {
                                Text(text =  it.displayName)
                            },
                            onClick = {
                                viewModel.selectSourceLanguage(it.name)
                                isSourceLanguageSelectorExpanded = false
                            }
                        )
                    }
                }
            }

            Text(
                text=">",
                fontSize = 28.sp,
            )

            ExposedDropdownMenuBox(
                expanded = isTargetLanguageSelectorExpanded,
                onExpandedChange = { isTargetLanguageSelectorExpanded = it },
                modifier = Modifier.width(150.dp)
            ) {
                TextField(
                    value = preferencesState.targetLanguage.displayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTargetLanguageSelectorExpanded)
                    },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier.menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = isTargetLanguageSelectorExpanded,
                    onDismissRequest = { isTargetLanguageSelectorExpanded = false }
                ) {
                    Languages.values().map {
                        DropdownMenuItem(
                            text = {
                                Text(text =  it.displayName)
                            },
                            onClick = {
                                viewModel.selectTargetLanguage(it.name)
                                isTargetLanguageSelectorExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Text(text = "No speech fragments to speech pause: ${preferencesState.noSpeechFragmentsToSpeechPause}")

        Slider(
            value = preferencesState.noSpeechFragmentsToSpeechPause.toFloat(),
            valueRange = 5f..45f,
            steps = 40,
            onValueChange = { viewModel.changeNoSpeechFragmentsToSpeechPause(it) }
        )

        Text(text = "No speech fragments to word gap: ${preferencesState.noSpeechFragmentsToWordGap}")

        Slider(
            value = preferencesState.noSpeechFragmentsToWordGap.toFloat(),
            valueRange = 1f..20f,
            steps = 20,
            onValueChange = { viewModel.changeNoSpeechFragmentsToWordGap(it) }
        )

        Button(
            onClick = {
                if (!uiState.isAllRequiredPermissionsGranted) {
                    permissionsLauncher.launch(askPermissions.toTypedArray())
                }
                viewModel.handleChangeTranslatingStateClick()
            },
            modifier = buttonModifier,
            enabled = uiState.isLoaded
        ) {
            Text(
                text = uiState.getTranslateButtonText(),
                fontSize = 19.sp,
            )
        }
    }
}


//@Composable
//fun makeToast(text: String) {
//    Toast.makeText(
//        LocalContext.current,
//        text,
//        Toast.LENGTH_SHORT
//    ).show()
//}

@Preview(showBackground = true)
@Composable
fun AudioTranslatorScreenPreview() {
    TestAudioTheme {
        AudioTranslatorScreen()
    }
}