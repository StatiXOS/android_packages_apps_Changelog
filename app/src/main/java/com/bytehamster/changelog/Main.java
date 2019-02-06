package com.bytehamster.changelog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Typeface;
import android.view.ViewGroup;
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
import android.text.Editable;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class Main extends Activity {

    public static final String DEFAULT_GERRIT_URL = "https://review.statixos.me/";
    public static final String DEFAULT_BRANCH = "";
    public static final int MAX_CHANGES = 2000;
    public static final int MAX_CHANGES_FETCH = 2000;
    public static final int MAX_CHANGES_DB = 2000;
    public static final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
    public static final SimpleDateFormat mDateDayFormat = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.US);

    private final ArrayList<Map<String, Object>> mChangesList = new ArrayList<>();
    private final List<HashMap<String, Object>> mWatchedList = new ArrayList<>();
    private ArrayList<Map<String, Object>> mDevicesList = new ArrayList<>();

    private ListView mListView = null;
    private Activity mActivity = null;
    private SwipeRefresh swipeContainer = null;
    private SharedPreferences mSharedPreferences = null;
    private String mDeviceFilterKeyword = "";
    private String mLastDate = "";
    private boolean mIsLoading = false;
    private boolean mJustStarted = true;
    private Document mWatchedDoc = null;
    private ChangeAdapter mChangeAdapter       = null;
    private int mChangesCount = 0;
    private String GERRIT_URL = DEFAULT_GERRIT_URL;

    boolean itemClicked = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mActivity = this;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        GERRIT_URL = mSharedPreferences.getString("server_url", DEFAULT_GERRIT_URL);
        mChangeAdapter = new ChangeAdapter(mActivity, mChangesList, GERRIT_URL);
        mListView = findViewById(android.R.id.list);

        mListView.setAdapter(mChangeAdapter);
        mListView.setOnItemClickListener(MainListClickListener);
        mListView.setOnItemLongClickListener(MainListLongClickListener);
        swipeContainer = findViewById(R.id.swipe_container);
        swipeContainer.setOnRefreshListener(new SwipeRefresh.OnRefreshListener() {
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
            mSharedPreferences.edit().putString("branch", "").apply();
        }
        load();
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
                        ((TextView) findViewById(android.R.id.empty)).setText("");
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
                final int change_size = changes.size();
                for (Change currentChange : changes) {
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

                    if (mChangesCount >= MAX_CHANGES) {
                        break;
                    }
                }

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (change_size == 0) {
                            ((TextView) findViewById(android.R.id.empty)).setText(R.string.no_changes);
                        }
                        mChangeAdapter.update(mChangesList);
                        mIsLoading = false;
                        swipeContainer.setRefreshing(false);
                    }
                });
            }
        }.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.changelog_main, menu);
        menu.findItem(R.id.action_filter).setIcon(R.drawable.menu_filter).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_filter:
                filter();
                itemClicked = true;
                return true;
            case R.id.action_settings:
                Intent i = new Intent(this, Preferences.class);
                startActivity(i);
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
        b.setCancelable(false);
        b.setTitle(R.string.filter);
        final View root = View.inflate(this, R.layout.dialog_filter, null);
        b.setView(root);

        b.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (!itemClicked) {
                    load();
                }
            }
        });
        b.setPositiveButton(R.string.ok, null);

        final Dialog d = b.create();

        d.setCanceledOnTouchOutside(true);
        d.setCancelable(true);

        ((CheckBox) root.findViewById(R.id.translations)).setChecked(mSharedPreferences.getBoolean("translations", true));

        final EditText branch = root.findViewById(R.id.branch);
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
            loadDeviceList(((ListView) root.findViewById(R.id.devices_listview)));
        }

        ((ListView) root.findViewById(R.id.devices_listview)).setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int pos, long id) {
                mWatchedDoc.getDocumentElement().removeChild((Element) mWatchedList.get(pos).get("device_element"));
                mSharedPreferences.edit().putString("watched_devices", StringTools.XmlToString(mActivity, mWatchedDoc)).apply();
                loadDeviceList(((ListView) root.findViewById(R.id.devices_listview)));
            }
        });
        ((CheckBox) root.findViewById(R.id.translations)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSharedPreferences.edit().putBoolean("translations", isChecked).apply();
                itemClicked = false;
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
                    loadDeviceList(((ListView) root.findViewById(R.id.devices_listview)));
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
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                    mDeviceFilterKeyword = ((EditText) root.findViewById(R.id.search_value)).getText().toString().trim();
                    loadAllDeviceList(((ListView) root.findViewById(R.id.devices_listview)));
                    itemClicked = false;
                }
                return false;
            }
        });

        root.findViewById(R.id.search_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mDeviceFilterKeyword = ((EditText) root.findViewById(R.id.search_value)).getText().toString().trim();
                loadAllDeviceList(((ListView) root.findViewById(R.id.devices_listview)));
                itemClicked = false;
            }
        });
        ((ListView) root.findViewById(R.id.devices_listview)).setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int pos, long id) {
                Node new_node = mWatchedDoc.importNode((Element) mDevicesList.get(pos).get("device_element"), true);
                mWatchedDoc.getDocumentElement().appendChild(new_node);
                mSharedPreferences.edit().putString("watched_devices", StringTools.XmlToString(mActivity, mWatchedDoc)).apply();

                d.dismiss();
                loadDeviceList(v);
                itemClicked = false;
            }
        });

        d.show();

        loadAllDeviceList(((ListView) root.findViewById(R.id.devices_listview)));

    }

    private void loadAllDeviceList(ListView listView) {
        mDevicesList = Devices.loadDefinitions(this, mDeviceFilterKeyword);

        if (mDevicesList == null) {
            Toast.makeText(this, "Error while loading devices", Toast.LENGTH_LONG).show();
            return;
        }

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

    private void loadDeviceList(final ListView listView) {
        if(mWatchedDoc == null) {
            // Not loaded. Try again later.
            new Handler().postDelayed( new Runnable() {
                public void run() {
                    loadDeviceList(listView);
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
            AddItemMap = new HashMap<>();
            NodeList properties = device.getChildNodes();
            AddItemMap.put("device_element", device);

            for (int k = 0; k < properties.getLength(); k++) {
                if (properties.item(k).getNodeType() != Node.ELEMENT_NODE) continue;
                Element property = (Element) properties.item(k);

                if (property.getNodeName().equals("name")) AddItemMap.put("name", property.getTextContent());

            }
            mWatchedList.add(AddItemMap);
        }

        Collections.sort(mWatchedList, new Devices.Comparator());

        SimpleAdapter sAdapter = new SimpleAdapter(mActivity, mWatchedList, android.R.layout.simple_list_item_1,
                new String[] { "name" }, new int[] { android.R.id.text1 });

        listView.setAdapter(sAdapter);
    }

    private final AdapterView.OnItemLongClickListener MainListLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
            if ((Integer) mChangesList.get(position).get("type") == Change.TYPE_ITEM) {
                AlertDialog.Builder d = new AlertDialog.Builder(mActivity);
                d.setCancelable(false);
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
                    final TextView info = view.findViewById(R.id.info);
                    final TextView title = view.findViewById(R.id.title);

                    final View buttons = view.findViewById(R.id.buttons);

                    final LinearLayout cardlayout = view.findViewById(R.id.cardlayout);

                    Typeface boldTypeface = Typeface.defaultFromStyle(Typeface.BOLD);

                    if (info.getVisibility() == View.GONE) {
                        info.setVisibility(View.VISIBLE);
                        buttons.setVisibility(View.VISIBLE);
                        mChangesList.get(position).put("visibility", View.VISIBLE);
                        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) cardlayout.getLayoutParams();
                        params.bottomMargin = 20;
                        title.setTypeface(boldTypeface);
                        AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
                        info.startAnimation(alphaAnimation);
                        buttons.startAnimation(alphaAnimation);
                    } else {
                        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) cardlayout.getLayoutParams();
                        params.bottomMargin = 0;
                        AlphaAnimation alphaAnimation = new AlphaAnimation(1, 0);
                        info.startAnimation(alphaAnimation);
                        buttons.startAnimation(alphaAnimation);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                info.setVisibility(View.GONE);
                                buttons.setVisibility(View.GONE);
                                mChangesList.get(position).put("visibility", View.GONE);
                                Typeface normalTypeface = Typeface.defaultFromStyle(Typeface.NORMAL);
                                title.setTypeface(normalTypeface);
                            }
                        }, 10);

                    }
                }
            }
        }
    };
}
