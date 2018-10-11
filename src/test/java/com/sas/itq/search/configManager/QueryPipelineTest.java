package com.sas.itq.search.configManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * TODO: Add class javadoc
 * User: snoctl
 * Date: 10/17/2017
 * Time: 9:46 AM
 * To change this template use File | Settings | File Templates.
 */
public class QueryPipelineTest {
    QueryPipeline qp;
    public static final String SASCOM_EN_US_QP = "src/test/resources/fusionJson/query-pipelines/SASCOM_QP_EN_US.json";
    String rulesFile = "src/test/resources/sascom-full-landing.json";
    String testOutput = "src/test/resources/fusionJson/query-pipelines/testoutput.json";

    //    String SASCOM_EN_US_QP="fusionJson/query-pipelines/SASCOM_QP_EN_US";
    @BeforeMethod
    public void setUp() throws Exception {
        //set up a file to a query pipeline
        qp = new QueryPipeline();

    }

    @Test
    public void testRead() throws IOException {
        URI uri = Paths.get(SASCOM_EN_US_QP).toUri();
        File f = new File(uri);
        File rf = new File(rulesFile);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> map = mapper.readValue(f, HashMap.class);
        qp = mapper.readValue(f, QueryPipeline.class);

        Map<String, String> ruleMap = mapper.readValue(rf, HashMap.class);
        Stage[] stages = qp.getStages();
        for (int i = 0; i < stages.length; i++) {
            Stage stage = stages[i];
            Map<String, Object> props = stage.properties;
            String stageType = props.getOrDefault("type", "none").toString();
            if (stageType.equals("landing-pages")) {
                props.put("rules", ruleMap.get("rules"));
                break;
            }
        }
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID);
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, qp);
        //Add a return and tab between  '}, {'
        String resultJson = sw.toString().replaceAll("\\}\\, \\{", "\\}\\,\\\r\\\n\\\t{");
        //write to a test spot
        Files.write(Paths.get(testOutput), resultJson.getBytes());
        assertNotNull(qp);
    }

}