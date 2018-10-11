package com.sas.itq.search.configManager;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Stage is used in query and index pipelines.
 * Not a standalone
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Stage extends JsonBased {
    public Stage() {
    }

}
