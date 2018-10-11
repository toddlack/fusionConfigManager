package com.sas.itq.search.configManager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * TODO: Add class javadoc
 * User: snoctl
 * Date: 9/18/2017
 * Time: 9:13 AM
 * To change this template use File | Settings | File Templates.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PropertiesUtil {
    Map<String, Object> propertiesMap;

    public Object get(String propertyName) {
        return propertiesMap.get(propertyName);
    }

    public void set(String propertyName, Object value) {
        propertiesMap.put(propertyName, value);
    }

    public void put(String propertyName, Object value) {
        set(propertyName, value);
    }
}
