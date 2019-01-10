package com.sas.itq.search.configManager;

import java.util.Map;

/**
 * Query profile
 */
public class QueryProfile extends FusionProfile {

    public QueryProfile(Map<String,String> initVals) {
        super(initVals,FusionProfile.QUERY_PROFILE);
    }
    public QueryProfile() {
        super(FusionProfile.QUERY_PROFILE);
    }

    @Override
    public String getProfileType() {
        return FusionObject.QUERY_PROFILES;
    }
}

