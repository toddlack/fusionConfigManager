package com.sas.itq.search.configManager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Calendar;

/**
 * {
 *   "resource" : "spark:hourlyMetricsRollup-gauges",
 *   "enabled" : true,
 *   "triggers" : [ {
 *     "type" : "interval",
 *     "enabled" : false,
 *     "interval" : 1,
 *     "timeUnit" : "hour",
 *     "startTime" : "2018-10-25T20:31:44.158Z",
 *     "type" : "interval"
 *   } ],
 *   "default" : false
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Trigger {
    String type;
    Boolean enabled;
    Long interval;
    String timeUnit;
    Calendar startTime;
    Calendar endTime;

    /** Pattern for trigger - could bve a crom format, or a start -after */
    String expression;

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

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    public String getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(String timeUnit) {
        this.timeUnit = timeUnit;
    }

    public Calendar getStartTime() {
        return startTime;
    }

    public void setStartTime(Calendar startTime) {
        this.startTime = startTime;
    }

    public Calendar getEndTime() {
        return endTime;
    }

    public void setEndTime(Calendar endTime) {
        this.endTime = endTime;
    }
}
