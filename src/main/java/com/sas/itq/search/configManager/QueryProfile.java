package com.sas.itq.search.configManager;

import java.util.Map;

/**
 * Query profile
 */
public class QueryProfile extends FusionProfile {

    public QueryProfile(Map<String,String> initVals) {
        super(initVals);
        //this.setSearchHandler(SEARCH_HANDLER); for v4
    }
    public QueryProfile() {
        super();
    }

    @Override
    public String getProfileType() {
        return FusionObject.QUERY_PROFILES;
    }
}

