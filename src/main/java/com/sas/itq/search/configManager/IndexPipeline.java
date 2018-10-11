package com.sas.itq.search.configManager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Manage the index pipelines for a Fusion instance
 * User: snoctl
 * Date: 9/15/2017
 * Time: 2:03 PM
 * To change this template use File | Settings | File Templates.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IndexPipeline extends Pipeline {
    @Override
    public String generateFileName() {
        StringBuilder fname = new StringBuilder(getId()).append(JSON);
        return fname.toString();
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
        return getId();
    }
}
