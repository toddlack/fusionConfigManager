package com.sas.itq.search.configManager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * TODO: Add class javadoc
 * User: snoctl
 * Date: 1/23/2018
 * Time: 2:28 PM
 * To change this template use File | Settings | File Templates.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Parser implements IdentifiableString {


    private String id;
    private Boolean enableMediaTypeDetection;
    private Integer maxParserDepth;
    ParserStage[] parserStages;

    @Override
    public String generateFileName() {
        return id+JSON;
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
        return id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getEnableMediaTypeDetection() {
        return enableMediaTypeDetection;
    }

    public void setEnableMediaTypeDetection(Boolean enableMediaTypeDetection) {
        this.enableMediaTypeDetection = enableMediaTypeDetection;
    }

    public Integer getMaxParserDepth() {
        return maxParserDepth;
    }

    public void setMaxParserDepth(Integer maxParserDepth) {
        this.maxParserDepth = maxParserDepth;
    }

    public ParserStage[] getParserStages() {
        return parserStages;
    }

    public void setParserStages(ParserStage[] parserStages) {
        this.parserStages = parserStages;
    }
}
