package com.bytehamster.changelog;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import android.app.Dialog;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.widget.AbsListView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends AppCompatActivity {

    public static final String                   DEFAULT_GERRIT_URL   = "https://gerrit.omnirom.org/";
    public static final String                   DEFAULT_BRANCH       = "android-6.0";
    public static final int                      MAX_CHANGES          = 200;
    public static final int                      MAX_CHANGES_FETCH    = 800;  // Max changes to be fetched
    public static final int                      MAX_CHANGES_DB       = 1500; // Max changes to be loaded from DB
    public static final SimpleDateFormat         mDateFormat          = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
    public static final SimpleDateFormat         mDateDayFormat       = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.US);

    private final ArrayList<Map<String, Object>> mChangesList         = new ArrayList<Map<String, Object>>();
    private final ArrayList<Map<String, Object>> mDevicesList         = new ArrayList<Map<String, Object>>();
    private final List<HashMap<String, Object>>  mWatchedList         = new ArrayList<HashMap<String, Object>>();

    private ListView                             mListView            = null;
    private Activity                             mActivity            = null;
    private SwipeRefreshLayout                   swipeContainer       = null;
    private SharedPreferences                    mSharedPreferences   = null;
    private String                               mDeviceFilterKeyword = "";
    private String                               mLastDate            = "";
    private boolean                              mIsLoading           = false;
    private boolean                              mJustStarted         = true;
    private Document                             mWatchedDoc          = null;
    private ChangeAdapter                        mChangeAdapter       = null;
    private int                                  mChangesCount        = 0;
    private String                               GERRIT_URL           = "https://gerrit.omnirom.org/";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mActivity = this;
        getSupportActionBar().setTitle(R.string.changelog);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        GERRIT_URL = mSharedPreferences.getString("server_url", DEFAULT_GERRIT_URL);
        mChangeAdapter = new ChangeAdapter(mActivity, mChangesList, GERRIT_URL);
        mListView = (ListView) findViewById(android.R.id.list);

        mListView.setAdapter(mChangeAdapter);
        mListView.setOnItemClickListener(MainListClickListener);
        mListView.setOnItemLongClickListener(MainListLongClickListener);
        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                load();
            }
        });
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) { }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int topRowVerticalPosition =
                        (mListView == null || mListView.getChildCount() == 0) ?
                                0 : mListView.getChildAt(0).getTop();
                swipeContainer.setEnabled(firstVisibleItem == 0 && topRowVerticalPosition >= 0);
            }
        });

        if (mSharedPreferences.getString("branch", DEFAULT_BRANCH).equals("All")) {
            mSharedPreferences.edit().putString("branch", "").commit();
        }

		load();
		checkAlerts();
	}

    @Override
    public void onResume() {
        super.onResume();
        GERRIT_URL = mSharedPreferences.getString("server_url", DEFAULT_GERRIT_URL);

        if(mJustStarted) {
            mJustStarted = false;
        } else {
            load();
        }
    }

    private void checkAlerts(){

        if (! mSharedPreferences.getBoolean("warning_displayed", false)) {
            AlertDialog.Builder d = new AlertDialog.Builder(mActivity);
            d.setCancelable(false);
            d.setTitle(R.string.first_warning);
            d.setMessage(Html.fromHtml(getResources().getString(R.string.first_warning_message)));
            d.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mSharedPreferences.edit().putBoolean("warning_displayed", true).apply();
                }
            });
            d.show();
        }

        if (!Build.DISPLAY.contains("omni") && !mSharedPreferences.getBoolean("openApp", false)) {
            AlertDialog.Builder d = new AlertDialog.Builder(mActivity);
            d.setCancelable(false);
            d.setTitle(R.string.not_supported);
            d.setMessage(Html.fromHtml(getResources().getString(R.string.not_supported_content)));
            d.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            d.setPositiveButton(R.string.open, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mSharedPreferences.edit().putBoolean("openApp", true).apply();
                }
            });
            d.show();
        }
    }

    private void load() {

        if (mIsLoading) return;
        mIsLoading = true;

        new Thread() {
            public void run() {
                
                if (!mChangesList.isEmpty()) mChangesList.clear();
                
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mChangeAdapter.clear();
                        findViewById(R.id.progress).setVisibility(View.VISIBLE);
                        ((TextView) findViewById(android.R.id.empty)).setText("");
                        getSupportActionBar().setTitle(getResources().getString(R.string.changelog));
                    }
                });


                ChangeLoader loader = new ChangeLoader(mActivity, mSharedPreferences, GERRIT_URL);
                ChangeFilter filter = new ChangeFilter(mSharedPreferences);

                List<Change> changes;
                try {
                    changes = loader.loadAll();
                } catch (ChangeLoader.LoadException e) {
                    Dialogs.usingCacheAlert(mActivity, GERRIT_URL, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            load();
                        }
                    });
                    changes = loader.getCached();
                }

                mChangesCount = 0;
                mLastDate = "-";
                int change_size = changes.size();
                for(int i = 0;i<change_size;i++) {
                    Change currentChange = changes.get(i);
                    if (filter.isHidden(currentChange)) {
                        continue;
                    }

                    if (!mLastDate.equals(currentChange.dateDay)) {
                        Map<String, Object> new_item = new HashMap<String, Object>();
                        new_item.put("title", currentChange.dateDay);
                        new_item.put("type", Change.TYPE_HEADER);
                        mChangesList.add(new_item);
                        mLastDate = currentChange.dateDay;
                    }
                    mChangesList.add(currentChange.getHashMap(mActivity));
                    mChangesCount++;

                    if(mChangesCount >= MAX_CHANGES) {
                        break;
                    }
                }

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideProgress();
                        ((TextView) findViewById(android.R.id.empty)).setText(R.string.no_changes);
                        mChangeAdapter.update(mChangesList);
                        if(mChangesCount >= MAX_CHANGES) {
                            getSupportActionBar().setTitle(getResources().getString(R.string.changelog) + " (" + MAX_CHANGES + "+)");
                        } else {
                            getSupportActionBar().setTitle(getResources().getString(R.string.changelog) + " (" + mChangesCount + ")");
                        }
                        mIsLoading = false;
                        swipeContainer.setRefreshing(false);
                    }
                });
            }
        }.start();
    }

    void hideProgress(){
        AlphaAnimation alphaAnimation = new AlphaAnimation(1, 0);
        alphaAnimation.setDuration(300);
        findViewById(R.id.progress).startAnimation(alphaAnimation);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.progress).setVisibility(View.GONE);
            }
          }, 280);
        
        final AlphaAnimation alphaAnimation2 = new AlphaAnimation(0, 1);
        alphaAnimation2.setDuration(300);
        mListView.startAnimation(alphaAnimation2);
        
        findViewById(android.R.id.empty).setVisibility(View.GONE);
        if(mChangesList.isEmpty()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    final AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
                    alphaAnimation.setDuration(500);
                    findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
                    findViewById(android.R.id.empty).startAnimation(alphaAnimation);
                }
              }, 280);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_info:
    
                String msg = getResources().getString(R.string.info_content);
                msg = msg.replace("%buildtime", mDateFormat.format(Build.TIME));
                msg = msg.replace("%lastrefresh", mDateFormat.format(mSharedPreferences.getLong("lastRefresh", 0)));

                AlertDialog.Builder d = new AlertDialog.Builder(mActivity);
                d.setCancelable(true);
                d.setTitle(R.string.info);
                d.setMessage(Html.fromHtml(msg));
                d.setPositiveButton(R.string.ok, null);
                d.show().setCanceledOnTouchOutside(true);
                return true;
            case R.id.action_refresh:
                load();
                return true;
            case R.id.action_filter:
                filter();
                return true;
            case R.id.action_settings:
                Intent i = new Intent(this, Preferences.class);
                startActivity(i);
                return true;
            case R.id.action_feedback:
                AlertDialog.Builder d2 = new AlertDialog.Builder(mActivity);
                d2.setCancelable(true);
                d2.setTitle(R.string.feedback);
                d2.setMessage(R.string.feedback_warning);
                d2.setNegativeButton(R.string.cancel, null);
                d2.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        External.feedbackMail(mActivity, "OmniROM Changelog: Feedback", "");
                    }
                });
                d2.show().setCanceledOnTouchOutside(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
	    }
    }
    
    void filter() {

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db;
            db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(mSharedPreferences.getString("watched_devices", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><devicesList></devicesList>")));
            mWatchedDoc = db.parse(is);
            mWatchedDoc.getDocumentElement().normalize();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        final AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setCancelable(true);
        b.setTitle(R.string.filter);
        final View root = View.inflate(this, R.layout.dialog_filter, null);
        b.setView(root);


        b.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                load();
            }
        });
        b.setPositiveButton(R.string.ok, null);

        final Dialog d = b.create();

        d.setCanceledOnTouchOutside(true);

        ((CheckBox) root.findViewById(R.id.translations)).setChecked(mSharedPreferences.getBoolean("translations", true));
        ((CheckBox) root.findViewById(R.id.show_twrp)).setChecked(mSharedPreferences.getBoolean("show_twrp", true));


        final EditText branch = (EditText) root.findViewById(R.id.branch);
        branch.setText(mSharedPreferences.getString("branch", DEFAULT_BRANCH));
        branch.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                mSharedPreferences.edit().putString("branch", s.toString()).apply();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
        
        
        if (mSharedPreferences.getBoolean("display_all", true)) {
            root.findViewById(R.id.devices_listview).setVisibility(View.GONE);
            root.findViewById(R.id.add_device).setVisibility(View.GONE);
            ((CheckBox) root.findViewById(R.id.all_devices)).setChecked(true);
        } else {
            ((CheckBox) root.findViewById(R.id.all_devices)).setChecked(false);
            load_device_list(((ListView) root.findViewById(R.id.devices_listview)));
        }

        ((ListView) root.findViewById(R.id.devices_listview)).setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int pos, long id) {
                mWatchedDoc.getDocumentElement().removeChild((Element) mWatchedList.get(pos).get("device_element"));
                mSharedPreferences.edit().putString("watched_devices", StringTools.XmlToString(mActivity, mWatchedDoc)).apply();
                load_device_list(((ListView) root.findViewById(R.id.devices_listview)));
            }
        });
        ((CheckBox) root.findViewById(R.id.translations)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSharedPreferences.edit().putBoolean("translations", isChecked).apply();
            }
        });
        ((CheckBox) root.findViewById(R.id.show_twrp)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSharedPreferences.edit().putBoolean("show_twrp", isChecked).apply();
            }
        });
        ((CheckBox) root.findViewById(R.id.all_devices)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSharedPreferences.edit().putBoolean("display_all", true).apply();
                    root.findViewById(R.id.devices_listview).setVisibility(View.GONE);
                    root.findViewById(R.id.add_device).setVisibility(View.GONE);
                } else {
                    mSharedPreferences.edit().putBoolean("display_all", false).apply();
                    root.findViewById(R.id.devices_listview).setVisibility(View.VISIBLE);
                    root.findViewById(R.id.add_device).setVisibility(View.VISIBLE);
                    if (mSharedPreferences.getString("watched_devices", "").equals("")) {
                        mSharedPreferences.edit().putString("watched_devices", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><devicesList></devicesList>").apply();
                    }
                    load_device_list(((ListView) root.findViewById(R.id.devices_listview)));
                }
            }
        });
        root.findViewById(R.id.add_device).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                add_device(((ListView) root.findViewById(R.id.devices_listview)));
            }
        });
        d.show();
    }

    void add_device(final ListView v) {

        mDeviceFilterKeyword = "";

        final AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setCancelable(true);
        b.setTitle(R.string.add_device);
        b.setPositiveButton(R.string.cancel, null);
        final View root = View.inflate(this, R.layout.dialog_add_device, null);
        b.setView(root);

        final Dialog d = b.create();
        d.setCanceledOnTouchOutside(true);


        ((EditText)root.findViewById(R.id.search_value)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    mDeviceFilterKeyword = ((EditText) root.findViewById(R.id.search_value)).getText().toString().trim();
                    load_all_device_list(((ListView) root.findViewById(R.id.devices_listview)));
                }
                return false;
            }
        });

        root.findViewById(R.id.report_missing_device).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                External.feedbackMail(mActivity, "OmniROM Changelog: Device request", "Please add my device to the filter list.\n" + Build.MANUFACTURER + " | " + Build.MODEL + " | " + Build.DEVICE + "\n");
            }
        });
        root.findViewById(R.id.search_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mDeviceFilterKeyword = ((EditText) root.findViewById(R.id.search_value)).getText().toString().trim();
                load_all_device_list(((ListView) root.findViewById(R.id.devices_listview)));
            }
        });
        ((ListView) root.findViewById(R.id.devices_listview)).setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int pos, long id) {
                Node new_node = mWatchedDoc.importNode((Element) mDevicesList.get(pos).get("device_element"), true);
                mWatchedDoc.getDocumentElement().appendChild(new_node);
                mSharedPreferences.edit().putString("watched_devices", StringTools.XmlToString(mActivity, mWatchedDoc)).apply();

                d.dismiss();
                load_device_list(v);
            }
        });

        d.show();

        load_all_device_list(((ListView) root.findViewById(R.id.devices_listview)));

    }

    void load_all_device_list(ListView listView) {
        if (!mDevicesList.isEmpty()) mDevicesList.clear();

        HashMap<String, Object> AddItemMap;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        Document doc = null;
        try {
            db = dbf.newDocumentBuilder();
            doc = db.parse(getAssets().open("projects.xml"));
            doc.getDocumentElement().normalize();
        }
        catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(mActivity, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        if (doc != null) {

            NodeList oemList = doc.getDocumentElement().getChildNodes();
            for (int i = 0; i < oemList.getLength(); i++) {
                if (oemList.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
                Element oem = (Element) oemList.item(i);

                String oemName = oem.getAttribute("name");
                NodeList deviceList = oem.getChildNodes();
                for (int j = 0; j < deviceList.getLength(); j++) {
                    if (deviceList.item(j).getNodeType() != Node.ELEMENT_NODE) continue;

                    Element device = (Element) deviceList.item(j);
                    NodeList properties = device.getChildNodes();
                    AddItemMap = new HashMap<String, Object>();
                    AddItemMap.put("device_element", device);

                    for (int k = 0; k < properties.getLength(); k++) {
                        if (properties.item(k).getNodeType() != Node.ELEMENT_NODE) continue;
                        Element property = (Element) properties.item(k);

                        if (property.getNodeName().equals("name")) AddItemMap.put("name", oemName + " " + property.getTextContent());
                        if (property.getNodeName().equals("code")) AddItemMap.put("code", property.getTextContent().toLowerCase(Locale.getDefault()));

                    }

                    if (mDeviceFilterKeyword.equals("")) {
                        mDevicesList.add(AddItemMap);
                    } else {
                        if (((String) AddItemMap.get("name")).toLowerCase(Locale.getDefault()).contains(mDeviceFilterKeyword.toLowerCase(Locale.getDefault()))
                                || ((String) AddItemMap.get("code")).toLowerCase(Locale.getDefault()).contains(mDeviceFilterKeyword.toLowerCase(Locale.getDefault()))) {
                            mDevicesList.add(AddItemMap);
                        }
                    }
                }
            }
        } else {
            AddItemMap = new HashMap<String, Object>();
            AddItemMap.put("name", "Fatal error. Contact developer.");
            mDevicesList.add(AddItemMap);
        }

        Collections.sort(mDevicesList, new sortComparator());

        SimpleAdapter sAdapter = new SimpleAdapter(mActivity, mDevicesList, R.layout.list_entry_device, new String[] { "name", "code", "code" }, new int[] { R.id.name, R.id.code, R.id.aside });
        sAdapter.setViewBinder(all_devices_view_binder);
        listView.setAdapter(sAdapter);
    }

    private final SimpleAdapter.ViewBinder all_devices_view_binder = new SimpleAdapter.ViewBinder() {
         @Override
         public boolean setViewValue(final View view, Object data, final String textRepresentation) {
             switch (view.getId()) {
             case R.id.aside:
                 if (Build.DEVICE.toLowerCase(Locale.getDefault()).equals(textRepresentation) || 
                         Build.MODEL.toLowerCase(Locale.getDefault()).replace("gt-", "").equals(textRepresentation)) {
                     ((TextView) view).setText(R.string.this_device);
                 } else ((TextView) view).setText("");
                 return true;
             }
             return false;
         }
    };

    void load_device_list(final ListView listView) {
        if(mWatchedDoc == null) {
            // Not loaded. Try again later.
            new Handler().postDelayed( new Runnable() {
                public void run() {
                    load_device_list(listView);
                }
            }, 500);
            return;
        }
        if (!mWatchedList.isEmpty()) mWatchedList.clear();

        HashMap<String, Object> AddItemMap;

        NodeList devicesList = mWatchedDoc.getDocumentElement().getChildNodes();
        for (int i = 0; i < devicesList.getLength(); i++) {
            if (devicesList.item(i).getNodeType() != Node.ELEMENT_NODE) continue;

            Element device = (Element) devicesList.item(i);
            AddItemMap = new HashMap<String, Object>();
            NodeList properties = device.getChildNodes();
            AddItemMap.put("device_element", device);

            for (int k = 0; k < properties.getLength(); k++) {
                if (properties.item(k).getNodeType() != Node.ELEMENT_NODE) continue;
                Element property = (Element) properties.item(k);

                if (property.getNodeName().equals("name")) AddItemMap.put("name", property.getTextContent());

            }
            mWatchedList.add(AddItemMap);
        }

        Collections.sort(mWatchedList, new sortComparator());

        SimpleAdapter sAdapter = new SimpleAdapter(mActivity, mWatchedList, android.R.layout.simple_list_item_1,
                new String[] { "name" }, new int[] { android.R.id.text1 });

        listView.setAdapter(sAdapter);
    }

    private final AdapterView.OnItemLongClickListener MainListLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
            if ((Integer) mChangesList.get(position).get("type") == Change.TYPE_ITEM) {
                AlertDialog.Builder d = new AlertDialog.Builder(mActivity);
                d.setCancelable(true);
                d.setTitle(R.string.change);
                d.setMessage((String) mChangesList.get(position).get("title"));
                d.setNegativeButton(R.string.cancel, null);
                d.setPositiveButton("Gerrit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri uri = Uri.parse(GERRIT_URL + "#/c/" + mChangesList.get(position).get("number"));
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                    }
                });
                d.show().setCanceledOnTouchOutside(true);
            }

            return true;
        }
    };

    private final AdapterView.OnItemClickListener MainListClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
            if ((Integer) mChangesList.get(position).get("type") == Change.TYPE_ITEM) {

                if (mSharedPreferences.getString("list_action", "popup").equals("popup")) {
                    Dialogs.changeDetails(mActivity, mChangesList.get(position), GERRIT_URL);
                } else {
                    final TextView info = (TextView) view.findViewById(R.id.info);
                    final View buttons = view.findViewById(R.id.buttons);

                    if (info.getVisibility() == View.GONE) {
                        info.setVisibility(View.VISIBLE);
                        buttons.setVisibility(View.VISIBLE);
                        mChangesList.get(position).put("visibility", View.VISIBLE);

                        AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
                        alphaAnimation.setDuration(500);
                        info.startAnimation(alphaAnimation);
                        buttons.startAnimation(alphaAnimation);
                    } else {
                        AlphaAnimation alphaAnimation = new AlphaAnimation(1, 0);
                        alphaAnimation.setDuration(300);
                        info.startAnimation(alphaAnimation);
                        buttons.startAnimation(alphaAnimation);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                info.setVisibility(View.GONE);
                                buttons.setVisibility(View.GONE);
                                mChangesList.get(position).put("visibility", View.GONE);
                            }
                        }, 300);

                    }
                }
            }
        }
    };

    private class sortComparator implements Comparator<Map<String, Object>> {
        @Override
        public int compare(Map<String, Object> m1, Map<String, Object> m2) {
            return ((String) m1.get("name")).compareToIgnoreCase((String) m2.get("name"));
        }
    }

}
