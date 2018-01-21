package com.bytehamster.changelog;

import android.content.Context;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Devices {
    public static ArrayList<Map<String, Object>> loadDefinitions(Context context, String filter) {
        try {
            return parseDefinitions(context.getAssets().open("projects.xml"), filter);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    static ArrayList<Map<String, Object>> parseDefinitions(InputStream is, String filter) {
        final ArrayList<Map<String, Object>> mDevicesList = new ArrayList<>();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        Document doc;
        try {
            db = dbf.newDocumentBuilder();
            doc = db.parse(is);
            doc.getDocumentElement().normalize();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }

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
                HashMap<String, Object> AddItemMap = new HashMap<>();
                AddItemMap.put("device_element", device);

                for (int k = 0; k < properties.getLength(); k++) {
                    if (properties.item(k).getNodeType() != Node.ELEMENT_NODE) continue;
                    Element property = (Element) properties.item(k);

                    if (property.getNodeName().equals("name")) AddItemMap.put("name", oemName + " " + property.getTextContent());
                    if (property.getNodeName().equals("code")) AddItemMap.put("code", property.getTextContent().toLowerCase(Locale.getDefault()));

                }

                if (filter.equals("")) {
                    mDevicesList.add(AddItemMap);
                } else {
                    if (((String) AddItemMap.get("name")).toLowerCase(Locale.getDefault()).contains(filter.toLowerCase(Locale.getDefault()))
                            || ((String) AddItemMap.get("code")).toLowerCase(Locale.getDefault()).contains(filter.toLowerCase(Locale.getDefault()))) {
                        mDevicesList.add(AddItemMap);
                    }
                }
            }
        }

        Collections.sort(mDevicesList, new Devices.Comparator());
        return mDevicesList;
    }


    public static class Comparator implements java.util.Comparator<Map<String, Object>> {
        @Override
        public int compare(Map<String, Object> m1, Map<String, Object> m2) {
            return ((String) m1.get("name")).compareToIgnoreCase((String) m2.get("name"));
        }
    }

}
