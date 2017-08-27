package com.bytehamster.changelog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class Preferences extends PreferenceActivity {
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);

        if(getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Preferences.this);
        findPreference("server_url").setSummary(prefs.getString("server_url", Main.DEFAULT_GERRIT_URL));
        findPreference("server_url").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                findPreference("server_url").setSummary("" + newValue);
                clearCache();
                return true;
            }
        });

        findPreference("clear_cache").setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                clearCache();
                return false;
            }
        });

        findPreference("about").setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder alert = new AlertDialog.Builder(Preferences.this);
                alert.setTitle(R.string.about);
                alert.setMessage(Html.fromHtml(getString(R.string.about_message)));
                alert.setPositiveButton(android.R.string.ok, null);
                Dialog d = alert.show();
                ((TextView) d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                return true;
            }
        });
    }

    private void clearCache() {
        Toast.makeText(Preferences.this, R.string.cleared_cache, Toast.LENGTH_LONG).show();
        ChangeCacheDatabase database = new ChangeCacheDatabase(Preferences.this);
        database.clearCache();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Preferences.this);
        prefs.edit().putLong("cache_lastrefresh", 0).apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home || item.getItemId() == 0) {
            finish();
        }
        return true;
    }
}
