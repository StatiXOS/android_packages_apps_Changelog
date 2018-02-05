package com.bytehamster.changelog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;
import android.widget.Toast;

public class PreferenceFragment extends android.preference.PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        final Preference serverlist = findPreference("server_url");
        serverlist.setSummary(prefs.getString("server_url", Main.DEFAULT_GERRIT_URL));
        serverlist.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return false;
            }
        });
        serverlist.setSelectable(false);

        final ListPreference listaction = (ListPreference) findPreference("list_action");
        listaction.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                setListPreference(getContext(), listaction);
                if (listaction.getDialog().getWindow() != null) {
                    listaction.getDialog().setCancelable(false);
                }
                return false;
            }
        });

        setListPreference(getContext(),listaction);

        findPreference("clear_cache").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                clearCache();
                getActivity().onBackPressed();
                return false;
            }
        });

        findPreference("about").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setCancelable(false);
                alert.setTitle(R.string.about);
                alert.setMessage(Html.fromHtml(getString(R.string.about_message)));
                alert.setPositiveButton(android.R.string.ok, null);
                Dialog d = alert.show();
                ((TextView) d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                return true;
            }
        });
    }

    protected static void setListPreference(Context context, ListPreference listactionlist) {
        CharSequence[] gridchangerlist_entries = { context.getResources().getString(R.string.gerrit_commit_style_popup), context.getResources().getString(R.string.gerrit_commit_style_expand) };
        CharSequence[] gridchangerlist_entryValues = {"1" , "2"};
        listactionlist.setTitle(R.string.list_action);
        listactionlist.setSummary(R.string.list_action_sum);
        listactionlist.setDialogTitle(R.string.list_action);
        listactionlist.setEntries(gridchangerlist_entries);
        listactionlist.setNegativeButtonText(R.string.ok);
        listactionlist.setKey("list_action");
        listactionlist.setDefaultValue("2");
        listactionlist.setEntryValues(gridchangerlist_entryValues);
    }

    private void clearCache() {
        Toast.makeText(getContext(), R.string.cleared_cache, Toast.LENGTH_LONG).show();
        ChangeCacheDatabase database = new ChangeCacheDatabase(getContext());
        database.clearCache();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().putLong("cache_lastrefresh", 0).apply();
    }
}