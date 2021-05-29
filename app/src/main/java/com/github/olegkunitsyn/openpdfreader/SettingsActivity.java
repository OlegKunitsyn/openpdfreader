package com.github.olegkunitsyn.openpdfreader;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.github.olegkunitsyn.openpdfreader.databinding.ActivitySettingsBinding;

import static com.github.olegkunitsyn.openpdfreader.MainActivity.PREF_NIGHT;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean(PREF_NIGHT, false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        prefs.registerOnSharedPreferenceChangeListener((updated, key) -> {
            if (key.equals(PREF_NIGHT)) {
                if (updated.getBoolean(PREF_NIGHT, false)) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
            }
        });

        ActivitySettingsBinding viewBinding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.preferences, new SettingsFragment())
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }
}
