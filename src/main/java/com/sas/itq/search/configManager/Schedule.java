package com.sas.itq.search.configManager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Calendar;
import java.util.Map;

/**
 * Schedule object from Fusion
 * User: snoctl
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Schedule implements IdentifiableString {
    String id;
    String creatorType;
    String creatorId;
    Calendar createTime;
    Calendar startTime;
    Calendar endTime;
    String repeatUnit;
    Integer interval;
    Boolean active;
    Map<String, Object> callParams;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreatorType() {
        return creatorType;
    }

    public void setCreatorType(String creatorType) {
        this.creatorType = creatorType;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public Calendar getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Calendar createTime) {
        this.createTime = createTime;
    }

    public Calendar getStartTime() {
        return startTime;
    }

    public void setStartTime(Calendar startTime) {
        this.startTime = startTime;
    }

    public String getRepeatUnit() {
        return repeatUnit;
    }

    public void setRepeatUnit(String repeatUnit) {
        this.repeatUnit = repeatUnit;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public Calendar getEndTime() {
        return endTime;
    }

    public void setEndTime(Calendar endTime) {
        this.endTime = endTime;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Map<String, Object> getCallParams() {
        return callParams;
    }

    public void setCallParams(Map<String, Object> callParams) {
        this.callParams = callParams;
    }

    @Override
    public String generateFileName() {
        StringBuilder sb = new StringBuilder(getId());
        //Schedule file names should start with an underscore
        if (sb.charAt(0) != '_') {
            sb = new StringBuilder("_").append(getId()).append(JSON);
        }
        return sb.toString();
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
