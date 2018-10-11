package com.sas.itq.search.configManager;

/**
 *  Index profile
 */
public class IndexProfile extends FusionProfile{
    public IndexProfile() {
        super();
    }

    @Override
    public String getProfileType() {
        return FusionObject.INDEX_PROFILES;
    }
}
