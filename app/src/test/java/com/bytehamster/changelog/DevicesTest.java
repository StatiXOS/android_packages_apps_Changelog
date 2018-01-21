package com.bytehamster.changelog;

import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

public class DevicesTest {

    public DevicesTest() {
        // Needed for tests to pass
    }

    @Test
    public void testDeviceDefinitionsValid() {
        InputStream is = DevicesTest.class.getClassLoader().getResourceAsStream("projects.xml");
        ArrayList<Map<String, Object>> devicesList = Devices.parseDefinitions(is, "");

        assertNotNull(devicesList);
    }

    @Test
    public void testDeviceDefinitionsFilter() {
        InputStream is = DevicesTest.class.getClassLoader().getResourceAsStream("projects.xml");
        ArrayList<Map<String, Object>> devicesList = Devices.parseDefinitions(is, "this-is-not-a-device");
        assertNotNull(devicesList);
        assertTrue(devicesList.isEmpty());
    }
}
