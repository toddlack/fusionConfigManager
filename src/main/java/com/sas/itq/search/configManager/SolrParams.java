package com.sas.itq.search.configManager;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO: Add class javadoc
 * User: snoctl
 * Date: 10/5/2017
 * Time: 3:20 PM
 * To change this template use File | Settings | File Templates.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SolrParams {

    String name;
    Integer numShards;
    Integer replicationFactor;
    Integer maxShardsPerNode;

    Map<String, Object> properties;

    public SolrParams() {
        properties = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getNumShards() {
        return numShards;
    }

    public void setNumShards(Integer numShards) {
        this.numShards = numShards;
    }

    public Integer getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(Integer replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    public Integer getMaxShardsPerNode() {
        return maxShardsPerNode;
    }

    public void setMaxShardsPerNode(Integer maxShardsPerNode) {
        this.maxShardsPerNode = maxShardsPerNode;
    }

    @JsonAnyGetter
    public Map<String, Object> any() {
        return properties;
    }

    @JsonAnySetter
    public void set(String name, Object value) {
        properties.put(name, value);
    }

}
