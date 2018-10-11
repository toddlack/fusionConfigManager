package com.sas.itq.search.configManager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Represent features of a collection
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CollectionFeature implements IdentifiableString {

    @JsonProperty("params")
    Map<String, Object> params;
    String name;
    String collectionId;
    Boolean enabled;

    public CollectionFeature() {
    }

    public CollectionFeature(CollectionFeature orig) {
        this();

        name = orig.name;
        collectionId = orig.collectionId;
        enabled=orig.enabled;
        params = new HashMap<>();
        params.putAll(orig.getParams());
    }

    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Object getParam(String paramName) {
        return params.get(paramName);
    }

    public void setParam(String paramName,Object value) {
        params.put(paramName,value);
    }


    @Override
    public String generateFileName() {
        return new StringBuilder(name).append(".json").toString();
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
        return name;
    }
}
