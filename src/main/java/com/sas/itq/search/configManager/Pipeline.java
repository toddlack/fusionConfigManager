package com.sas.itq.search.configManager;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO: Add class javadoc
 * User: snoctl
 * Date: 11/7/2017
 * Time: 2:59 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class Pipeline implements IdentifiableString {
    String id;
    Stage[] stages;
    Map<String, Object> properties;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Stage[] getStages() {
        return stages;
    }

    public void setStages(Stage[] stages) {
        this.stages = stages;
    }

    @JsonAnyGetter
    public Map<String, Object> any() {
        return properties;
    }

    @JsonAnySetter
    public void set(String name, Object value) {
        properties.put(name, value);
    }

    public Object get(String name) {
        return properties.get(name);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public abstract String generateFileName();
}
