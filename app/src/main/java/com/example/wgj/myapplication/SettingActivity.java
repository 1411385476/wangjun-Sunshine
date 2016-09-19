package com.example.wgj.myapplication;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;


public class SettingActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);


    }

    private void bindPreferenceSummaryToValue(Preference preference){
        onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences
                (preference.getContext()).getString(preference.getKey(), ""));
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String value = newValue.toString();
        if(preference instanceof ListPreference){
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(value);
            if(prefIndex > 0){
                preference.setSummary(value);
            }

        }else {
            preference.setSummary(value);
        }
        return false;
    }
}
