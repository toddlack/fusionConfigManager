package com.sas.itq.search.configManager;

import java.util.Map;

/**
 *  Index profile
 */
public class IndexProfile extends FusionProfile{
    public IndexProfile() {
        super();
    }

    public IndexProfile(Map<String, String> initVals) {
        super(initVals);
    }

    @Override
    public  String getProfileType() {
        return FusionObject.INDEX_PROFILES;
    }
}
