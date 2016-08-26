package com.bytehamster.changelog;

import android.content.SharedPreferences;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

class ChangeFilter {
    private final List<String> mUseProjectsList = new ArrayList<String>();

    private final boolean displayAll;
    private final boolean translations;
    private final boolean twrp;
    private final String branch;
    private final SharedPreferences prefs;

    public ChangeFilter(SharedPreferences prefs) {
        this.prefs = prefs;
        displayAll = prefs.getBoolean("display_all", true);
        translations = prefs.getBoolean("translations", true);
        twrp = prefs.getBoolean("show_twrp", true);
        branch = prefs.getString("branch", Main.DEFAULT_BRANCH);
        refreshWatchedDevices();
    }

    boolean isHidden(Change c) {
        boolean hidden = false;

        if (!displayAll) {
            if (c.project.startsWith("android_device_"))   hidden = true;
            if (c.project.startsWith("android_hardware_")) hidden = true;
            if (c.project.startsWith("android_kernel_"))   hidden = true;
            if (mUseProjectsList.contains(c.project))      hidden = false;
        }
        if (!translations) {
            if (c.message.contains("translation"))  hidden = true;
            if (c.message.contains("localisation")) hidden = true;
        }
        if (!twrp) {
            if (c.project.equals("android_bootable_recovery")) hidden = true;
        }
        if (!branch.equals("")) {
            if (! c.branch.startsWith(branch)) hidden = true;
        }

        return hidden;
    }

    void refreshWatchedDevices() {
        String watchedDevices = prefs.getString("watched_devices", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><devicesList></devicesList>");
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db;
            db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(watchedDevices));
            Document mWatchedDoc = db.parse(is);
            mWatchedDoc.getDocumentElement().normalize();

            if (!mUseProjectsList.isEmpty()) mUseProjectsList.clear();

            if (! displayAll) {
                mUseProjectsList.add("android_vendor_omni");
                NodeList gitList = mWatchedDoc.getDocumentElement().getElementsByTagName("git");
                for (int i = 0; i < gitList.getLength(); i++) {
                    if (gitList.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
                    mUseProjectsList.add(((Element) gitList.item(i)).getAttribute("name"));
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
