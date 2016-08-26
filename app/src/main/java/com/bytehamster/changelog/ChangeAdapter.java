package com.bytehamster.changelog;

import java.util.ArrayList;
import java.util.Map;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

class ChangeAdapter extends BaseAdapter {

    private final Activity                 mActivity;
    private final LayoutInflater           mInflater;

    private ArrayList<Map<String, Object>> mArrayList;
    private SharedPreferences              mSharedPreferences = null;
    private String gerrit_url = "";

    public ChangeAdapter(Activity a, ArrayList<Map<String, Object>> arrayList, String gerrit_url) {
        this.gerrit_url = gerrit_url;
        mArrayList = arrayList;
        mActivity = a;
        mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(a);
    }

    @SuppressWarnings("unchecked")
    public void update(ArrayList<Map<String, Object>> arrayList) {
        mArrayList = (ArrayList<Map<String, Object>>) arrayList.clone();
        this.notifyDataSetChanged();
    }

    public void clear() {
        if (!mArrayList.isEmpty()) mArrayList.clear();
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = null;

        if (mArrayList.size() >= position) {

            if (convertView == null) {
                switch (((Integer) mArrayList.get(position).get("type"))) {
                case Change.TYPE_ITEM:
                    view = mInflater.inflate(R.layout.list_entry, parent, false);
                    view.setTag(Change.TYPE_ITEM);
                    break;
                case Change.TYPE_HEADER:
                    view = mInflater.inflate(R.layout.list_header, parent, false);
                    view.setTag(Change.TYPE_HEADER);
                    break;
                }
            } else if (!convertView.getTag().equals(mArrayList.get(position).get("type"))) {
                switch (((Integer) mArrayList.get(position).get("type"))) {
                case Change.TYPE_ITEM:
                    view = mInflater.inflate(R.layout.list_entry, parent, false);
                    view.setTag(Change.TYPE_ITEM);
                    view.findViewById(R.id.info).setTag(View.GONE);
                    break;
                case Change.TYPE_HEADER:
                    view = mInflater.inflate(R.layout.list_header, parent, false);
                    view.setTag(Change.TYPE_HEADER);
                    break;
                }
            } else view = convertView;

            switch (((Integer) mArrayList.get(position).get("type"))) {
            case Change.TYPE_ITEM:
                ((TextView) view.findViewById(R.id.title)).setText((String) mArrayList.get(position).get("title"));
                ((TextView) view.findViewById(R.id.secondline)).setText((String) mArrayList.get(position).get("secondline"));
                ((TextView) view.findViewById(R.id.info)).setText((String) mArrayList.get(position).get("expand"));

                int visibility = (Integer) mArrayList.get(position).get("visibility");
                //noinspection ResourceType
                view.findViewById(R.id.info).setVisibility(visibility);
                //noinspection ResourceType
                view.findViewById(R.id.buttons).setVisibility(visibility);

                if ((Boolean) mArrayList.get(position).get("is_new") && mSharedPreferences.getBoolean("animate_new", true)) {
                    view.findViewById(R.id.is_new).setVisibility(View.VISIBLE);
                } else {
                    view.findViewById(R.id.is_new).setVisibility(View.GONE);
                }

                view.findViewById(R.id.open_gerrit).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Uri uri = Uri.parse(gerrit_url + "#/c/" + mArrayList.get(position).get("number"));
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        mActivity.startActivity(intent);
                    }
                });

                break;
            case Change.TYPE_HEADER:
                ((TextView) view.findViewById(R.id.title)).setText((String) mArrayList.get(position).get("title"));
                break;
            }
        } else {
            view = mInflater.inflate(R.layout.list_entry, parent, false);
        }

        return view;
    }
}
