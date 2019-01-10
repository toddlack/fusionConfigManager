package com.sas.itq.search.configManager;

import java.util.Map;

/**
 *  Index profile
 */
public class IndexProfile extends FusionProfile{
    public IndexProfile() {
        super(INDEX_PROFILE);
    }

    public IndexProfile(Map<String, String> initVals) {
        super(initVals,INDEX_PROFILE);
    }

    @Override
    public  String getProfileType() {
        return FusionObject.INDEX_PROFILES;
    }
}
