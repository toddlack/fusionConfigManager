package com.sas.itq.search.configManager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Schedule object from Fusion
 * {
 *   "resource" : "datasource:c360_invoice",
 *   "enabled" : true,
 *   "triggers" : [ {
 *     "type" : "cron",
 *     "enabled" : true,
 *     "expression" : "0 0 6 ? * MON-FRI",
 *     "type" : "cron"
 *   } ],
 *   "default" : true
 * }
 * User: snoctl
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Schedule implements IdentifiableString {
    String resource;
    Boolean enabled;
    @JsonProperty("default")
    Boolean isDefault;
    List<Trigger> triggers;

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

    public Boolean getDefault() {
        return isDefault;
    }

    public void setDefault(Boolean aDefault) {
        isDefault = aDefault;
    }

    public List<Trigger> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<Trigger> triggers) {
        this.triggers = triggers;
    }

    @Override
    public String generateFileName() {
        StringBuilder sb = new StringBuilder(getResource().replace(":","-"))
                .append(JSON);
        return sb.toString();
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

    public String getId() {
        return getResource();
    }
}
