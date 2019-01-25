package com.bytehamster.changelog;

import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.view.View;
import android.widget.ListView;

public class PreferenceFragment extends android.preference.PreferenceFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View rootView = getView();
        ListView list;
        if (rootView != null) {
            list = rootView.findViewById(android.R.id.list);
            list.setDivider(null);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);

        final ListPreference listaction = (ListPreference) findPreference("list_action");
        listaction.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                setListPreference(getContext(), listaction);
                if (listaction.getDialog().getWindow() != null) {
                    listaction.getDialog().setCancelable(true);
                }
                return false;
            }
        });

        setListPreference(getContext(),listaction);
    }

    protected static void setListPreference(Context context, ListPreference listactionlist) {
        CharSequence[] gridchangerlist_entries = { context.getResources().getString(R.string.gerrit_commit_style_popup), context.getResources().getString(R.string.gerrit_commit_style_expand) };
        CharSequence[] gridchangerlist_entryValues = {"popup" , "expand"};
        listactionlist.setTitle(R.string.list_action);
        listactionlist.setSummary(R.string.list_action_sum);
        listactionlist.setDialogTitle(R.string.list_action);
        listactionlist.setEntries(gridchangerlist_entries);
        listactionlist.setNegativeButtonText(R.string.ok);
        listactionlist.setKey("list_action");
        listactionlist.setDefaultValue("popup");
        listactionlist.setEntryValues(gridchangerlist_entryValues);
    }
}