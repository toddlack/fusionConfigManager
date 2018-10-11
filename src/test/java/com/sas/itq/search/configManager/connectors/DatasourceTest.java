package com.sas.itq.search.configManager.connectors;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * TODO: Add class javadoc
 * User: snoctl
 * Date: 9/18/2017
 * Time: 8:55 AM
 * To change this template use File | Settings | File Templates.
 */
public class DatasourceTest {

    Datasource datasource;
    JsonNode jsonTree;
    TreeNode objectTree;
    String jsonInitFile = "src/test/resources/fusionJson/datasources/sasit-ds.json";
    String jsonUpdatedFile = "src/test/resources/fusionJson/datasources/sasit-ds-updated.json";
    ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        byte[] jsonData = Files.readAllBytes(Paths.get(jsonInitFile));
        objectMapper = new ObjectMapper();
        datasource = objectMapper.readValue(jsonData, Datasource.class);
    }

    @Test
    public void testHasCollection() {
        String collection = datasource.get("collection").toString();
        assertNotNull("Did not get datasource ", datasource.id);
        assertNotNull("Did not get collection for datasource {}", collection);
    }

    @Test
    public void testChange() throws IOException {
        Date now = Calendar.getInstance().getTime();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd-hh.mm");
        datasource.setDescription("new description on " + fmt.format(now));
        String jsonOut = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(datasource);
        //Read as an Object in order for pringting to not have  extra newLines
        Object jsonObj = objectMapper.readValue(jsonOut, Object.class);
        System.out.println(jsonOut);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(Files.newOutputStream(Paths.get(jsonUpdatedFile)), jsonObj);

    }

}