package com.sas.itq.search.configManager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * TODO: Add class javadoc
 * User: snoctl
 * Date: 1/23/2018
 * Time: 3:46 PM
 * To change this template use File | Settings | File Templates.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParserStage {
    String id;
    String type;
    Boolean enabled;
    String[] mediaTypes;
    String[] pathPatterns;
    String errorHandling;
    Boolean alwaysDetect;
    Boolean inheritMediaTypes;
    Boolean ignoreBOM;
    Boolean autoDetect;
    String charset;
    String delimiter;
    Boolean hasHeaders;
    String[] headers;
    String comment;
    String fillValue;
    Boolean trimWhiteSpace;
    Boolean includeRowNumber;
    String commentHandling;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String[] getMediaTypes() {
        return mediaTypes;
    }

    public void setMediaTypes(String[] mediaTypes) {
        this.mediaTypes = mediaTypes;
    }

    public String[] getPathPatterns() {
        return pathPatterns;
    }

    public void setPathPatterns(String[] pathPatterns) {
        this.pathPatterns = pathPatterns;
    }

    public String getErrorHandling() {
        return errorHandling;
    }

    public void setErrorHandling(String errorHandling) {
        this.errorHandling = errorHandling;
    }

    public Boolean getAlwaysDetect() {
        return alwaysDetect;
    }

    public void setAlwaysDetect(Boolean alwaysDetect) {
        this.alwaysDetect = alwaysDetect;
    }

    public Boolean getInheritMediaTypes() {
        return inheritMediaTypes;
    }

    public void setInheritMediaTypes(Boolean inheritMediaTypes) {
        this.inheritMediaTypes = inheritMediaTypes;
    }

    public Boolean getIgnoreBOM() {
        return ignoreBOM;
    }

    public void setIgnoreBOM(Boolean ignoreBOM) {
        this.ignoreBOM = ignoreBOM;
    }

    public Boolean getAutoDetect() {
        return autoDetect;
    }

    public void setAutoDetect(Boolean autoDetect) {
        this.autoDetect = autoDetect;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public Boolean getHasHeaders() {
        return hasHeaders;
    }

    public void setHasHeaders(Boolean hasHeaders) {
        this.hasHeaders = hasHeaders;
    }

    public String[] getHeaders() {
        return headers;
    }

    public void setHeaders(String[] headers) {
        this.headers = headers;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getFillValue() {
        return fillValue;
    }

    public void setFillValue(String fillValue) {
        this.fillValue = fillValue;
    }

    public Boolean getTrimWhiteSpace() {
        return trimWhiteSpace;
    }

    public void setTrimWhiteSpace(Boolean trimWhiteSpace) {
        this.trimWhiteSpace = trimWhiteSpace;
    }

    public Boolean getIncludeRowNumber() {
        return includeRowNumber;
    }

    public void setIncludeRowNumber(Boolean includeRowNumber) {
        this.includeRowNumber = includeRowNumber;
    }

    public String getCommentHandling() {
        return commentHandling;
    }

    public void setCommentHandling(String commentHandling) {
        this.commentHandling = commentHandling;
    }

}
