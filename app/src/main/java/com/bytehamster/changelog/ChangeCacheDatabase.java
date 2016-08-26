package com.bytehamster.changelog;

import java.util.ArrayList;
import java.util.List;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class ChangeCacheDatabase extends SQLiteOpenHelper {
    private static final int    DATABASE_VERSION = 1;
    private static final String DATABASE_NAME    = "ChangelogDatabase";
    private static final String TABLE_CACHE      = "cache";

    private static final String KEY_PRIMARY = "prim_key";
    private static final String KEY_ID      = "id";
    private static final String KEY_BRANCH  = "branch";
    private static final String KEY_NUMBER  = "number";
    private static final String KEY_PROJECT = "project";
    private static final String KEY_DATE    = "date";
    private static final String KEY_OWNER   = "owner";
    private static final String KEY_TITLE   = "title";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_LASTMOD = "lastmod";
    private static final String KEY_ADD1    = "add1";
    private static final String KEY_ADD2    = "add2";
    private static final String KEY_ADD3    = "add3";
    private static final String KEY_ADD4    = "add4";

    public ChangeCacheDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_SHORTCUTS_TABLE = "CREATE TABLE " + TABLE_CACHE + "(" +
                KEY_PRIMARY + " INTEGER PRIMARY KEY," +
                KEY_ID + " TEXT," +
                KEY_BRANCH + " TEXT," +
                KEY_NUMBER + " TEXT," +
                KEY_PROJECT + " TEXT," +
                KEY_DATE + " INTEGER," +
                KEY_OWNER + " TEXT," +
                KEY_TITLE + " TEXT," +
                KEY_MESSAGE + " TEXT," +
                KEY_LASTMOD + " INTEGER," +
                KEY_ADD1 + " TEXT," +
                KEY_ADD2 + " TEXT," +
                KEY_ADD3 + " TEXT," +
                KEY_ADD4 + " TEXT" + ")";
        db.execSQL(CREATE_SHORTCUTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CACHE);
        onCreate(db);
    }

    public void clearCache() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CACHE);
        onCreate(db);
        db.close();
    }

    public void addChange(Change change) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_ID, change.id);
        values.put(KEY_BRANCH, change.branch);
        values.put(KEY_NUMBER, change.number);
        values.put(KEY_PROJECT, change.project);
        values.put(KEY_DATE, change.date);
        values.put(KEY_OWNER, change.owner);
        values.put(KEY_TITLE, change.title);
        values.put(KEY_MESSAGE, change.message);
        values.put(KEY_LASTMOD, change.lastModified);
        values.put(KEY_ADD1, "");
        values.put(KEY_ADD2, "");
        values.put(KEY_ADD3, "");
        values.put(KEY_ADD4, "");

        db.insert(TABLE_CACHE, null, values);
        db.close();
    }

    public List<Change> getChanges( ) {
        List<Change> changeList = new ArrayList<Change>();
        String selectQuery = "SELECT  * FROM " + TABLE_CACHE + " ORDER BY " + KEY_LASTMOD + " DESC," + KEY_ID
                + " LIMIT " + Main.MAX_CHANGES_DB;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Change newChange = new Change();
                // cursor.getString(0) is primary key
                newChange.id = cursor.getString(1);
                newChange.branch = cursor.getString(2);
                newChange.number = cursor.getString(3);
                newChange.project = cursor.getString(4);
                newChange.date = Long.valueOf(cursor.getString(5));
                newChange.owner = cursor.getString(6);
                newChange.title = cursor.getString(7);
                newChange.message = cursor.getString(8);
                newChange.lastModified = Long.valueOf(cursor.getString(9));
                newChange.calculateDate();

                changeList.add(newChange);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return changeList;
    }

}