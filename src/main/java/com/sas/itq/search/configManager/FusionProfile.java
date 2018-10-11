package com.sas.itq.search.configManager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Super class for index and query profiles. These are
 * simple name-value json pairs. The format is:
 * <pre>profile-name : pipeline-id</pre>
 *
 * Profiles belong to a collection. The actual json looks like:
 * <pre>
 *    "queryProfiles" : {
 *       "intranet" : {
 *         "default" : "intranet",
 *         "qp2" : "intra_date_boost4"
 *       }
 *     }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class FusionProfile extends JsonBased implements IdentifiableString{

    @JsonProperty
    public String getCollection() {
        return String.valueOf(properties.get("collection"));
    }

    public void setCollection(String val) {
        set("collection",val);
    }

    @JsonProperty
    public String getId() {
        return String.valueOf(properties.get("id"));
    }

    public void setId(String id) {
        set("id",id);
    }

    @JsonProperty
    public String getPipeline() {
        return String.valueOf(properties.get("pipeline"));
    }
    public void setPipeline(String val) {
        set("pipeline",val);
    }
    @Override
    public String generateFileName() {
        //Add to collection folder
        String pathname = Paths.get(getCollection(),
                getPathSegmentName()+JSON).toString();
        return pathname;
    }
    @Override
    public String generateFileName(String parentDir) {
        String fname = Paths.get(parentDir,generateFileName()).toString();
        return fname;
    }

    /** Constant value that matches the string key when retrieving profiles from
     * an Objects API call.
     * @return
     */
    public abstract  String getProfileType();

    /**
     * Return a string that is the PATH segment for a get or PUT. Sometimes it is ID , sometimes name
     */
    @Override
    public String getPathSegmentName() {
        return getId();
    }
}
