package com.atlassian.confluence.extra.jira.handlers;

import junit.framework.TestCase;

public class TestVersionComparision extends TestCase {
    public void testCompareEqual() {
        Version v1 = new Version("5.1.1");
        Version v2 = new Version("5.1.1");
        assertTrue(v1.compareTo(v2) == 0);
    }
    
    public void testCompareMainVsSubVersion() {
        Version v1 = new Version("5.1");
        Version v2 = new Version("5.1.1");
        assertTrue(v1.compareTo(v2) < 0);
    }

    public void testCompareMainVersions() {
        Version v1 = new Version("5.1");
        Version v2 = new Version("5.2");
        assertTrue(v1.compareTo(v2) < 0);
    }

    public void testCompareMinorVersions() {
        Version v1 = new Version("5.0.1");
        Version v2 = new Version("5.0.2");
        assertTrue(v1.compareTo(v2) < 0);
        
        v1 = new Version("5.0.1");
        v2 = new Version("5.1.1");
        assertTrue(v1.compareTo(v2) < 0);
    }

}
