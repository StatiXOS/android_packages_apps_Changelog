package com.bytehamster.changelog;

import android.content.Context;
import android.view.View;
import java.util.HashMap;

class Change {
    public static final int TYPE_ITEM   = 0;
    public static final int TYPE_HEADER = 1;
    //public static final int                      TYPE_MAX_COUNT       = 2;

    public String id       = "";
    public String branch   = "";
    public String number   = ""; // Website link
    public String project  = "";
    public String dateFull = "";
    public String dateDay  = "";
    public String owner    = "";
    public String title    = "";
    public String message  = "";
    public long   date     = 0;
    public long   lastModified  = 0;
    public boolean isNew   = false;


    HashMap<String, Object> getHashMap(Context c) {
        HashMap<String, Object> new_item = new HashMap<String, Object>();
        new_item.put("title", title);
        new_item.put("secondline", c.getResources().getString(R.string.owner_and_date)
                .replace("%o", owner)
                .replace("%d", dateFull));
        new_item.put("owner", owner );
        new_item.put("dateFull", dateFull);
        new_item.put("project", project);
        new_item.put("number", number);
        new_item.put("type", TYPE_ITEM);
        new_item.put("branch", branch);
        new_item.put("change_id", id);
        new_item.put("is_new", isNew);
        new_item.put("message", message);
        new_item.put("expand", c.getResources().getString(R.string.expanded_message)
                .replace("%project", project)
                .replace("%message", message)
                .replace("%branch", branch ));
        new_item.put("visibility", View.GONE);
        return new_item;
    }

    void calculateDate() {
        dateFull = Main.mDateFormat.format(date);
        dateDay  = Main.mDateDayFormat.format(date);
    }
}
