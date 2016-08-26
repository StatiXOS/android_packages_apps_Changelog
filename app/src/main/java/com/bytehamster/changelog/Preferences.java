package com.bytehamster.changelog;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.bytehamster.lib.MaterialDialog.MaterialDialog;

public class Preferences extends PreferenceActivity {
    private int aboutClick = 0;

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
                aboutClick++;
                if(aboutClick == 10) {
                    MaterialDialog alert = new MaterialDialog(getBaseContext(), R.color.color_primary);

                    final EditText edittext = new EditText(getBaseContext());
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Preferences.this);
                    edittext.setText(prefs.getString("server_url", Main.DEFAULT_GERRIT_URL));

                    alert.setView(edittext);
                    alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Preferences.this);
                            prefs.edit().putString("server_url", edittext.getText().toString()).apply();
                        }
                    });

                    alert.setNegativeButton(android.R.string.cancel, null);
                    alert.show();
                }
                return false;
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
