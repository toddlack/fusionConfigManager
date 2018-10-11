package com.sas.itq.search.configManager;

/**
 * Query profile
 */
public class QueryProfile extends FusionProfile {

    public QueryProfile() {
        super();
    }

    @Override
    public String getProfileType() {
        return FusionObject.QUERY_PROFILES;
    }
}

