package com.bytehamster.changelog;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

class ChangeLoader {

    private final        SharedPreferences preferences;
    private final        ChangeCacheDatabase db;
    private long         firstCachedDate = 0;
    private int          DownloadAtOnce = 1;
    private List<Change> cached = null;
    String gerrit_url = "";

    public ChangeLoader(Context c, SharedPreferences prefs, String gerrit_url) {
        db = new ChangeCacheDatabase(c);
        preferences = prefs;
        this.gerrit_url = gerrit_url;
    }

    public List<Change> loadAll() throws LoadException {
        List<Change> changes = new ArrayList<Change>();
        cached  = loadCached();  // Load first to get firstCachedID and see if change is cached
        if(cached.isEmpty()) {
            DownloadAtOnce = 25; // Faster when not using so many connections
        } else {
            firstCachedDate = cached.get(0).lastModified;
            DownloadAtOnce = 10;
        }
        changes.addAll(loadNew());
        changes.addAll(cached);
        Collections.sort(changes, new Comparator<Change>() {
            @Override
            public int compare(Change c1, Change c2) {
                return - Long.compare(c1.date,c2.date); // Minus = ASC
            }
        });
        return changes;
    }

    public List<Change> loadCached() {
        if(Build.TIME != preferences.getLong("cachedBuildTime",0)) {
            preferences.edit().putLong("cachedBuildTime",Build.TIME).apply();
            db.clearCache();
        }
        return db.getChanges();
    }

    public List<Change> getCached() {
        Collections.sort(cached, new Comparator<Change>() {
            @Override
            public int compare(Change c1, Change c2) {
                return - Long.compare(c1.date,c2.date); // Minus = ASC
            }
        });
        return cached;
    }

    private List<Change> loadNew() throws LoadException {
        List<Change> changes = new ArrayList<Change>();

        try {
            int skipEntries = 0;

            loader:while(skipEntries<Main.MAX_CHANGES_FETCH) {
                String query_url = gerrit_url + "changes/?q=status:merged&pp=0&o=CURRENT_REVISION"
                        + "&o=CURRENT_COMMIT&o=MESSAGES&o=DETAILED_ACCOUNTS&n=" + DownloadAtOnce;
                query_url = query_url + "&S=" + skipEntries;

                String res = httpRequest(query_url);
                res = res.replace(")]}'\n", ""); // Gerrit uses malformed JSON
                JSONArray result = new JSONArray(res);

                int size = result.length();
                for(int i = 0;i<size;i++) {
                    Change c = parseResult((JSONObject)result.get(i));
                    if(c != null) {
                        if(firstCachedDate >= c.lastModified) {
                            break loader;
                        } else if(c.lastModified < Build.TIME) {
                            break loader;
                        } else {
                            if(c.date > Build.TIME && !inCache(c)) {
                                changes.add(c);
                                db.addChange(c);
                            }
                        }
                    }
                    skipEntries++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new LoadException();
        }
        preferences.edit().putLong("lastRefresh",System.currentTimeMillis()).apply();
        return changes;
    }

    private String httpRequest(String query_url) throws LoadException {
        String res = null;
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(query_url).openConnection();
            res = StringTools.StreamToString(con.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(res == null) { // retry
            try {
                HttpURLConnection con = (HttpURLConnection) new URL(query_url).openConnection();
                res = StringTools.StreamToString(con.getInputStream());
            } catch (IOException e) {
                throw new LoadException();
            }
        }
        return res;
    }

    private boolean inCache(Change c) {
        if(cached != null) {
            int size = cached.size();
            for(int i = 0;i<size;i++) {
                if(cached.get(i).id.equals(c.id)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Change parseResult(JSONObject mCurrentObject) {
        try {

            Date mDate = Main.mDateFormat.parse(mCurrentObject.getString("updated"));

            Change newChange = new Change();

            JSONObject mJSONTemp = (JSONObject) mCurrentObject.get("revisions");   // Revisions
            mJSONTemp = (JSONObject) mJSONTemp.get(mCurrentObject.getString("current_revision")); // Current revision
            mJSONTemp = (JSONObject) mJSONTemp.get("commit");           // Current commit
            newChange.message = mJSONTemp.getString("message");// Commit message


            newChange.title = mCurrentObject.getString("subject");
            newChange.owner = ((JSONObject) mCurrentObject.get("owner")).getString("name");
            newChange.lastModified = mDate.getTime();

            JSONArray messages = (JSONArray) mCurrentObject.get("messages");
            int length = messages.length();
            for(int i=0;i<length;i++) {
                String comment = ((JSONObject)messages.get(i)).getString("message");
                if (comment.startsWith("Change has been successfully merged into the git repository")) {
                    mDate = Main.mDateFormat.parse(((JSONObject)messages.get(i)).getString("date"));
                    break;
                } else if (comment.startsWith("Change has been successfully rebased as")) {
                    mDate = Main.mDateFormat.parse(((JSONObject)messages.get(i)).getString("date"));
                    break;
                }
            }
            newChange.date = mDate.getTime();

            String pr = mCurrentObject.getString("project");
            newChange.project = pr.substring(pr.lastIndexOf('/') + 1);
            newChange.number = mCurrentObject.getString("_number");
            newChange.branch = mCurrentObject.getString("branch");
            newChange.id = mCurrentObject.getString("change_id");

            if(cached != null && cached.size() != 0) {
                newChange.isNew = true;
            }
            newChange.calculateDate();

            return newChange;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    class LoadException extends Exception{
        // Throwed when network error occurs
    }
}
