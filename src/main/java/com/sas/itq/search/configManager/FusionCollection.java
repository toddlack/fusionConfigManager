package com.sas.itq.search.configManager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.beanutils.BeanUtils;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Hold FusionCollection properties for the Fusion API
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FusionCollection implements IdentifiableString, Cloneable {

    public static final String TYPE_DATA = "DATA";
    public static final String TYPE_SIGNALS = "SIGNALS";
    public static final String TYPE_SEARCH_LOGS = "SEARCHLOGS";
    public static final String TYPE_SIGNALS_AGGREGATION = "SIGNALS_AGGREGATION";
    String id;
    String searchClusterId;
    SolrParams solrParams;
    String type;
    Calendar createdAt;
    @JsonProperty("metaData")
    Map<String,Object> metaData;
    /**
     * Keeps all solr configuration file info in this list
     */
    List<SolrConfigData> solrConfigData;
    List<CollectionFeature> features;

    public FusionCollection() {

    }

    public static FusionCollection create(JsonNode node) {
        ObjectMapper mp = new ObjectMapper();
        FusionCollection f = null;
        try {
            f = mp.readValue(node.toString(), FusionCollection.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return f;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSearchClusterId() {
        return searchClusterId;
    }

    public void setSearchClusterId(String searchClusterId) {
        this.searchClusterId = searchClusterId;
    }

    public SolrParams getSolrParams() {
        return solrParams;
    }

    public void setSolrParams(SolrParams solrParams) {
        this.solrParams = solrParams;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Calendar getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Calendar createdAt) {
        this.createdAt = createdAt;
    }

    public Map getMetaData() {
        return metaData;
    }

    public void setMetaData(Map metaData) {
        this.metaData = metaData;
    }

    public List<SolrConfigData> getSolrConfigData() {
        return solrConfigData;
    }

    public void setSolrConfigData(List<SolrConfigData> solrConfigData) {
        this.solrConfigData = solrConfigData;
    }

    public List<CollectionFeature> getFeatures() {
        return features;
    }

    public void setFeatures(List<CollectionFeature> features) {
        this.features = features;
    }

    @Override
    public String generateFileName(String parentDir) {
        StringBuilder sb=new StringBuilder(parentDir).append("/").append(generateFileName());
        return sb.toString();
    }

    @Override
    public String generateFileName() {
        StringBuilder sb = new StringBuilder(getId()).append("/").append(getId()).append(JSON);
        return sb.toString();
    }

    /**
     * Return a string that is the PATH segment for a get or PUT. Sometimes it is ID , sometimes name
     */
    @Override
    public String getPathSegmentName() {
        return getId();
    }

    @Override
    public String toString() {
        return "FusionCollection{" +
                "id='" + id + '\'' +
                ", searchClusterId='" + searchClusterId + '\'' +
                ", solrParams=" + solrParams +
                ", type='" + type + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    /**
     * Clean data for post. Remove things like created Date, and special ID strings that are unique to a server.
     */
    public FusionCollection clean() {
        if (solrParams != null) {
            solrParams.setMaxShardsPerNode(null);
            //remove the name - if it does not exist in SOLR already, it will error out. Revisit later.
            solrParams.setName(null);
        }
        features=null;
        createdAt = null;
        return this;
    }

    /** Copy a new version with a new name */
    public FusionCollection copy(FusionCollection original, final String newName, boolean shallow) {

        FusionCollection retVal=new FusionCollection();
        retVal.setId(newName);
        retVal.setSearchClusterId(original.getSearchClusterId());
        SolrParams p = null;
        try {
            p = (SolrParams) BeanUtils.cloneBean(original.getSolrParams());
        } catch (Exception e) {
            p=new SolrParams();
            p.setNumShards(original.solrParams.numShards);
            e.printStackTrace();
        }
        SolrParams o = original.getSolrParams();
        p.setNumShards(o.getNumShards());
        p.setReplicationFactor(o.replicationFactor);
        p.properties.putAll(o.properties);
        retVal.setSolrParams(p);

        retVal.setMetaData(original.getMetaData());
        retVal.setType(original.getType());
        retVal.setFeatures(original.getFeatures());
        if (!shallow) {
            retVal.setSolrConfigData(original.getSolrConfigData());
        }
        List<CollectionFeature> newFeatures = original.getFeatures().stream()
                .map(f -> {//Make a copy and reset collection name
                            try {
                                CollectionFeature t = (CollectionFeature) BeanUtils.cloneBean(f);
                                t.setCollectionId(newName);
                                return t;
                            } catch (Exception e) {
                                e.printStackTrace();
                                return f;
                            }
                        }
                )
                .collect(Collectors.toList());
        retVal.setFeatures(newFeatures);
        return retVal;
    }
    public FusionCollection copy(FusionCollection original, boolean shallow) {
        return copy(original,original.getId(),shallow);
    }
}
