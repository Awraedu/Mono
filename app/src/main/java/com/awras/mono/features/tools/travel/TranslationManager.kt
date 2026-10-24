package com.awras.mono.features.tools.travel

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale

data class LangItem(
    val code: String,
    val name: String,
    val nativeName: String = "",
    val isDownloaded: Boolean = false
)

object TranslationManager {

    private val modelManager = RemoteModelManager.getInstance()

    // Explicitly mapped display names for ML Kit Translate languages
    private val KNOWN_LANGUAGES = mapOf(
        "af" to Pair("Afrikaans", "Afrikaans"),
        "sq" to Pair("Albanian", "Shqip"),
        "am" to Pair("Amharic", "አማርኛ"),
        "ar" to Pair("Arabic", "العربية"),
        "hy" to Pair("Armenian", "Հայերեն"),
        "az" to Pair("Azerbaijani", "Azərbaycan"),
        "eu" to Pair("Basque", "Euskara"),
        "be" to Pair("Belarusian", "Беларуская"),
        "bn" to Pair("Bengali", "বাংলা"),
        "bg" to Pair("Bulgarian", "Български"),
        "ca" to Pair("Catalan", "Català"),
        "zh" to Pair("Chinese", "中文"),
        "hr" to Pair("Croatian", "Hrvatski"),
        "cs" to Pair("Czech", "Čeština"),
        "da" to Pair("Danish", "Dansk"),
        "nl" to Pair("Dutch", "Nederlands"),
        "en" to Pair("English", "English"),
        "eo" to Pair("Esperanto", "Esperanto"),
        "et" to Pair("Estonian", "Eesti"),
        "fi" to Pair("Finnish", "Suomi"),
        "fr" to Pair("French", "Français"),
        "gl" to Pair("Galician", "Galego"),
        "ka" to Pair("Georgian", "ქართული"),
        "de" to Pair("German", "Deutsch"),
        "el" to Pair("Greek", "Ελληνικά"),
        "gu" to Pair("Gujarati", "ગુજરાતી"),
        "ht" to Pair("Haitian Creole", "Kreyòl Ayisyen"),
        "he" to Pair("Hebrew", "עברית"),
        "hi" to Pair("Hindi", "हिन्दी"),
        "hu" to Pair("Hungarian", "Magyar"),
        "is" to Pair("Icelandic", "Íslenska"),
        "id" to Pair("Indonesian", "Bahasa Indonesia"),
        "ga" to Pair("Irish", "Gaeilge"),
        "it" to Pair("Italian", "Italiano"),
        "ja" to Pair("Japanese", "日本語"),
        "kn" to Pair("Kannada", "ಕನ್ನಡ"),
        "ko" to Pair("Korean", "한국어"),
        "lv" to Pair("Latvian", "Latviešu"),
        "lt" to Pair("Lithuanian", "Lietuvių"),
        "mk" to Pair("Macedonian", "Македонски"),
        "ms" to Pair("Malay", "Bahasa Melayu"),
        "mt" to Pair("Maltese", "Malti"),
        "mr" to Pair("Marathi", "मराठी"),
        "no" to Pair("Norwegian", "Norsk"),
        "fa" to Pair("Persian", "فارسی"),
        "pl" to Pair("Polish", "Polski"),
        "pt" to Pair("Portuguese", "Português"),
        "ro" to Pair("Romanian", "Română"),
        "ru" to Pair("Russian", "Русский"),
        "sk" to Pair("Slovak", "Slovenčina"),
        "sl" to Pair("Slovenian", "Slovenščina"),
        "es" to Pair("Spanish", "Español"),
        "sw" to Pair("Swahili", "Kiswahili"),
        "sv" to Pair("Swedish", "Svenska"),
        "tl" to Pair("Tagalog", "Tagalog"),
        "ta" to Pair("Tamil", "தமிழ்"),
        "te" to Pair("Telugu", "తెలుగు"),
        "th" to Pair("Thai", "ไทย"),
        "tr" to Pair("Turkish", "Türkçe"),
        "uk" to Pair("Ukrainian", "Українська"),
        "ur" to Pair("Urdu", "اردو"),
        "vi" to Pair("Vietnamese", "Tiếng Việt"),
        "cy" to Pair("Welsh", "Cymraeg")
    )

    /**
     * Returns ONLY official ML Kit languages that have valid model files on Google ML Kit servers.
     */
    fun getAllSupportedLanguages(downloadedCodes: Set<String> = emptySet()): List<LangItem> {
        val mlKitCodes = TranslateLanguage.getAllLanguages().toSet()

        return mlKitCodes.map { code ->
            val knownInfo = KNOWN_LANGUAGES[code]
            val displayName: String
            val nativeName: String

            if (knownInfo != null) {
                displayName = knownInfo.first
                nativeName = if (knownInfo.second.equals(knownInfo.first, ignoreCase = true)) "" else knownInfo.second
            } else {
                val locale = Locale.forLanguageTag(code)
                displayName = locale.getDisplayName(Locale.ENGLISH).ifBlank { code }
                val nativeStr = locale.getDisplayName(locale)
                nativeName = if (nativeStr.equals(displayName, ignoreCase = true)) "" else nativeStr
            }

            LangItem(
                code = code,
                name = displayName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() },
                nativeName = nativeName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() },
                isDownloaded = downloadedCodes.contains(code) || code == TranslateLanguage.ENGLISH
            )
        }.sortedBy { it.name }
    }

    fun getDownloadedLanguageCodes(
        onSuccess: (Set<String>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { models ->
                val codes = models.map { it.language }.toSet()
                onSuccess(codes)
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    /**
     * Downloads translation model using ML Kit Translator client to ensure model pair registration.
     */
    fun downloadModel(
        languageCode: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(languageCode)
                .build()
            val translator = Translation.getClient(options)
            val conditions = DownloadConditions.Builder().build()

            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    translator.close()
                    onSuccess()
                }
                .addOnFailureListener {
                    // Fallback to RemoteModelManager download
                    val model = TranslateRemoteModel.Builder(languageCode).build()
                    modelManager.download(model, conditions)
                        .addOnSuccessListener {
                            translator.close()
                            onSuccess()
                        }
                        .addOnFailureListener { err ->
                            translator.close()
                            onError(err)
                        }
                }
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun deleteModel(
        languageCode: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val model = TranslateRemoteModel.Builder(languageCode).build()
        modelManager.deleteDownloadedModel(model)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    fun translate(
        sourceLang: String,
        targetLang: String,
        text: String,
        onDownloadStarted: () -> Unit = {},
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
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
