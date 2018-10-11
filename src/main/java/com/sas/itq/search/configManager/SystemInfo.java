package com.sas.itq.search.configManager;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Represent the system installation info. Useful to get version and server information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemInfo implements IdentifiableString {
    String defaultVal="defaultInfo";

    public SystemInfo() {
    }

    public SystemInfo(Map<String, Object> properties) {
        this.properties = properties;
    }

    public SystemInfo(String initial) {
        String test="done";
    }

    Map<String, Object> properties = new HashMap<>();

    public Object get(String propname) {
        return properties.get(propname);
    }
    public Object get(String propname,String defaultVal) {
        return properties.getOrDefault(propname,defaultVal);
    }

    @JsonAnyGetter
    public Map<String, Object> properties() {
        return properties;
    }

    @JsonAnySetter
    public Object set(String name, Object value) {
        return properties.put(name, value);
    }
    @Override
    public String generateFileName() {
        return get("name",defaultVal).toString();
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
        return get("name",defaultVal).toString();
    }
}
