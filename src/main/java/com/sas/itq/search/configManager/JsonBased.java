package com.sas.itq.search.configManager;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic class for handling json based api returns.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonBased {
    Map<String, Object> properties=new HashMap<>();

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
}
