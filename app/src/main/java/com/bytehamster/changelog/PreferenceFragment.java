package com.bytehamster.changelog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class PreferenceFragment extends android.preference.PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        findPreference("server_url").setSummary(prefs.getString("server_url", Main.DEFAULT_GERRIT_URL));
        findPreference("server_url").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setTitle(R.string.server_url);
                final View urlDialogView = View.inflate(getContext(), R.layout.dialog_select_url, null);
                final EditText editText = urlDialogView.findViewById(R.id.gerrit_url_edit);
                final RadioButton radioCustom = urlDialogView.findViewById(R.id.gerrit_button_custom);
                final RadioButton radioOmnirom = urlDialogView.findViewById(R.id.gerrit_button_omnirom);
                final RadioButton radioLineageos = urlDialogView.findViewById(R.id.gerrit_button_lineageos);

                String url = prefs.getString("server_url", Main.DEFAULT_GERRIT_URL);

                if (url.equals(getString(R.string.gerrit_url_lineageos))) {
                    radioLineageos.setChecked(true);
                } else if (url.equals(getString(R.string.gerrit_url_omnirom))) {
                    radioOmnirom.setChecked(true);
                } else {
                    radioCustom.setChecked(true);
                    editText.setVisibility(View.VISIBLE);
                    editText.setText(url);
                }

                radioCustom.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        editText.setVisibility(radioCustom.isChecked() ? View.VISIBLE : View.GONE);
                        editText.setText(prefs.getString("server_url", Main.DEFAULT_GERRIT_URL));
                    }
                });
                alert.setView(urlDialogView);
                alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String serverUrl;
                        if (radioOmnirom.isChecked()) {
                            serverUrl = getString(R.string.gerrit_url_omnirom);
                        } else if (radioLineageos.isChecked()) {
                            serverUrl = getString(R.string.gerrit_url_lineageos);
                        } else {
                            serverUrl = editText.getText().toString();
                        }
                        prefs.edit().putString("server_url", serverUrl).apply();

                        findPreference("server_url").setSummary(serverUrl);
                        clearCache();
                    }
                });
                alert.setNegativeButton(android.R.string.cancel, null);
                alert.show();
                return true;
            }
        });

        findPreference("clear_cache").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                clearCache();
                return false;
            }
        });

        findPreference("about").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
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
        Toast.makeText(getContext(), R.string.cleared_cache, Toast.LENGTH_LONG).show();
        ChangeCacheDatabase database = new ChangeCacheDatabase(getContext());
        database.clearCache();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().putLong("cache_lastrefresh", 0).apply();
    }
}