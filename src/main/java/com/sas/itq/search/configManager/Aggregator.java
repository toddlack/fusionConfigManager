package com.sas.itq.search.configManager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * /api/apollo/aggregator/aggregations
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Aggregator implements IdentifiableString {
    String id;
    String[] groupingFields;
    String[] signalTypes;
    String outputPipeline;
    Boolean sourceRemove;
    Boolean outputRollup;
    Boolean sourceCatchup;
    String timeRange;
    String[] statsFields;
    String aggregator;
    String selectQuery;

    @Override
    public String generateFileName() {
        return getId();
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String[] getGroupingFields() {
        return groupingFields;
    }

    public void setGroupingFields(String[] groupingFields) {
        this.groupingFields = groupingFields;
    }

    public String[] getSignalTypes() {
        return signalTypes;
    }

    public void setSignalTypes(String[] signalTypes) {
        this.signalTypes = signalTypes;
    }

    public String getOutputPipeline() {
        return outputPipeline;
    }

    public void setOutputPipeline(String outputPipeline) {
        this.outputPipeline = outputPipeline;
    }

    public Boolean getSourceRemove() {
        return sourceRemove;
    }

    public void setSourceRemove(Boolean sourceRemove) {
        this.sourceRemove = sourceRemove;
    }

    public Boolean getOutputRollup() {
        return outputRollup;
    }

    public void setOutputRollup(Boolean outputRollup) {
        this.outputRollup = outputRollup;
    }

    public Boolean getSourceCatchup() {
        return sourceCatchup;
    }

    public void setSourceCatchup(Boolean sourceCatchup) {
        this.sourceCatchup = sourceCatchup;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }

    public String[] getStatsFields() {
        return statsFields;
    }

    public void setStatsFields(String[] statsFields) {
        this.statsFields = statsFields;
    }

    public String getAggregator() {
        return aggregator;
    }

    public void setAggregator(String aggregator) {
        this.aggregator = aggregator;
    }

    public String getSelectQuery() {
        return selectQuery;
    }

    public void setSelectQuery(String selectQuery) {
        this.selectQuery = selectQuery;
    }
}
