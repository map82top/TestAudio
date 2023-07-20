package com.example.testaudio.common

enum class Languages(val displayName: String, val speechRecognitionCode: String, val translateCode: String) {
    DE("German", "de-DE", "de"),
    EN("English", "en-US", "en"),
    ES("Spanish", "es-ES", "es"),
    FI("Finland","fi-FI", "fi"),
    FR("French","fr-Fr", "fr"),
    HE("Hebrew","he-HE", "he"),
    IT("Italian", "it-IT", "it"),
    KK("Kazakh", "kk-KZ", "kk"),
    NL("Netherlandish", "nl-NL", "nl"),
    PL("Polish", "pl-PL", "pl"),
    PT("Portugal", "pt-PT", "pt"),
    RU("Russian", "ru-Ru", "ru"),
    SV("Swedish", "sv-SE", "sv"),
    TR("Turkish", "tr-TR", "tr"),
    UZ("Uzbek", "uz-UZ", "uz")
}