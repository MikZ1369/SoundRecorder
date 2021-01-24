package mik.example.sound_recorder

import android.content.Context
import android.preference.PreferenceManager


object MySharedPreferences {
    private const val PREF_HIGH_QUALITY = "pref_high_quality"
    fun setPrefHighQuality(context: Context?, isEnabled: Boolean) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = preferences.edit()
        editor.putBoolean(PREF_HIGH_QUALITY, isEnabled)
        editor.apply()
    }

    @JvmStatic
    fun getPrefHighQuality(context: Context?): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getBoolean(PREF_HIGH_QUALITY, false)
    }
}