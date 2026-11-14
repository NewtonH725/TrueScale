package com.example.truescale.utils

import android.app.LocaleManager as AndroidLocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleManager {

    private const val PREFS_NAME = "true_scale_prefs"
    private const val KEY_LANGUAGE = "app_language"

    const val LANG_SYSTEM = "system"
    const val LANG_IT = "it"
    const val LANG_EN = "en"

    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
    }

    fun setAppLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val androidLocaleManager = context.getSystemService(AndroidLocaleManager::class.java)
            if (androidLocaleManager != null) {
                if (languageCode == LANG_SYSTEM) {
                    androidLocaleManager.applicationLocales = LocaleList.getEmptyLocaleList()
                } else {
                    androidLocaleManager.applicationLocales = LocaleList.forLanguageTags(languageCode)
                }
                return
            }
        }

        // Fallback for context configuration
        val targetLocale = if (languageCode == LANG_SYSTEM) {
            Locale.getDefault()
        } else {
            Locale.forLanguageTag(languageCode)
        }
        Locale.setDefault(targetLocale)
        val config = context.resources.configuration
        config.setLocale(targetLocale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun applySavedLanguage(context: Context) {
        val savedLang = getSavedLanguage(context)
        setAppLanguage(context, savedLang)
    }
}
