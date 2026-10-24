package com.awras.mono.features.tools.travel

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

object TranslationManager {
    fun translate(
        sourceLang: String,
        targetLang: String,
        text: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit,
        onDownloadStarted: () -> Unit
    ) {
        if (text.isBlank()) {
            onSuccess("")
            return
        }

        if (sourceLang == targetLang) {
            onSuccess(text)
            return
        }

        try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build()

            val translator = Translation.getClient(options)
            val conditions = DownloadConditions.Builder().build()

            onDownloadStarted()
            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    translator.translate(text)
                        .addOnSuccessListener { result ->
                            onSuccess(result)
                            translator.close()
                        }
                        .addOnFailureListener { e ->
                            onError(e)
                            translator.close()
                        }
                }
                .addOnFailureListener { e ->
                    onError(e)
                    translator.close()
                }
        } catch (e: Exception) {
            onError(e)
        }
    }
}