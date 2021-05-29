package com.github.olegkunitsyn.openpdfreader;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.github.olegkunitsyn.openpdfreader.databinding.ActivityAboutBinding;

import static com.github.olegkunitsyn.openpdfreader.MainActivity.PREF_NIGHT;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PREF_NIGHT, false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        com.github.olegkunitsyn.openpdfreader.databinding.ActivityAboutBinding viewBinding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        viewBinding.versionTextView.setText(getAppVersion());
    }

    public void sourceCode(View v) {
        startActivity(getIntent("https://github.com/OlegKunitsyn/openpdfreader"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    private String getAppVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return "0.0.0";
    }

    private static Intent getIntent(String url) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    }
}