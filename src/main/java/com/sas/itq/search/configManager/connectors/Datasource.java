package com.sas.itq.search.configManager.connectors;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.sas.itq.search.configManager.IdentifiableString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Manage fusion datasources
 * User: snoctl
 * Date: 9/15/2017
 * Time: 2:07 PM
 * To change this template use File | Settings | File Templates.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Datasource implements IdentifiableString {

    public Map<String, Object> properties = new HashMap<>();
    public String id;
    public Calendar created;
    public Calendar modified;
    public String connector;
    public String type;
    public String description;
    public String pipeline;

    public Object get(String name) {
        return properties.get(name);
    }

    @JsonAnyGetter
    public Map<String, Object> any() {
        return properties;
    }

    @JsonAnySetter
    public void set(String name, Object value) {
        properties.put(name, value);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Calendar getCreated() {
        return created;
    }

    public void setCreated(Calendar created) {
        this.created = created;
    }

    public Calendar getModified() {
        return modified;
    }

    public void setModified(Calendar modified) {
        this.modified = modified;
    }

    public String getConnector() {
        return connector;
    }

    public void setConnector(String connector) {
        this.connector = connector;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPipeline() {
        return pipeline;
    }

    public void setPipeline(String pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public String generateFileName() {
        //TODO - should this have 'ds' in the filename?
        return getId();
    }
    @Override
    public String generateFileName(String parentDir) {
        StringBuilder sb= new StringBuilder(parentDir).append(generateFileName()).append(JSON);
        return sb.toString();
    }

    /**
     * Return a string that is the PATH segment for a get or PUT. Sometimes it is ID , sometimes name
     */
    @Override
    public String getPathSegmentName() {
        return getId();
    }
}
