package com.sas.itq.search;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to match the spreadsheet used to set landing pages in Fusion
 */
class LandingPage {


    /**
     * Constants to help with finding the right columns to use
     */
    public static final String KEYWORD = "keyword";
    public static final String MODE = "mode";
    public static final String URL = "url";
    public static final String TITLE = "title";
    /**
     * Map of columns for the simplified format to allow users to specifyg landing page rules via spreadsheet
     */
    public static final Map<String, Integer> SIMPLE_COLUMNS;
    /**
     * Map of columns in "master" spreadsheet
     */
    public static final Map<String, Integer> FULL_COLUMNS;


    static {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put(KEYWORD, 0);
        map1.put(URL, 1);
        map1.put(TITLE, 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put(KEYWORD, 0);
        /* in spreadsheet, the column header for "mode" may be "Match Criteria" */
        map2.put(MODE, 1);
        map2.put(URL, 2);
        map2.put(TITLE, 3);

        SIMPLE_COLUMNS = Collections.unmodifiableMap(map1);
        FULL_COLUMNS = Collections.unmodifiableMap(map2);

    }

    /*Landing page specific*/
    String keyword;
    String url;
    String mode;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
