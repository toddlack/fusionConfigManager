package com.sas.itq.search.configManager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * manage the Job entity
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Job implements IdentifiableString {

    private String resource;
    private Boolean enabled;

    private Map<String, Object> extra = new HashMap<>();

    @Override
    public String generateFileName() {
        //resources typically have a "type:"name. Strip off the prefix.
        String[] processed = getResource().split(":");
        return processed[processed.length - 1]+JSON;
    }
    @Override
    public String generateFileName(String parentDir) {
        StringBuilder sb= new StringBuilder(parentDir).append(generateFileName());
        return sb.toString();
    }


    /**
     * Return a string that is the PATH segment for a get or PUT. Sometimes it is ID , sometimes name
     */
    @Override
    public String getPathSegmentName() {
        return getResource();
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }
}
