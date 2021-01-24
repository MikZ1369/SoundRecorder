package mik.example.sound_recorder.fragments

import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.Preference.OnPreferenceChangeListener
import android.preference.Preference.OnPreferenceClickListener
import android.preference.PreferenceFragment
import mik.example.sound_recorder.MySharedPreferences
import mik.example.sound_recorder.R
import mik.example.sound_recorder.activities.SettingsActivity


class SettingsFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
        val highQualityPref =
            findPreference(resources.getString(R.string.pref_high_quality_key)) as CheckBoxPreference
        highQualityPref.isChecked = MySharedPreferences.getPrefHighQuality(activity)
        highQualityPref.onPreferenceChangeListener =
            OnPreferenceChangeListener { preference, newValue ->
                MySharedPreferences.setPrefHighQuality(activity, newValue as Boolean)
                true
            }
    }
}