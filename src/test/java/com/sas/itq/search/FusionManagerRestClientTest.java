package com.sas.itq.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sas.itq.search.configManager.*;
import com.sas.itq.search.configManager.connectors.Datasource;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * TODO: Add class javadoc
 * User: snoctl
 * Date: 6/7/2017
 * Time: 1:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class FusionManagerRestClientTest {
    FusionManagerRestClient client;
    public static final String AUTHENTICATION_AEMPUSH = "YWVtcHVzaDphZW1wdXNoMTIz";
    public static final String AUTHENTICATION_2 = "c25vY3RsOnNub2N0bDEyMw==";
    public static final String BASE_URL_AROMA = "http://aroma.exnet.sas.com:8764";
    public static final String BASE_URL_CHICAGO = "http://chicago.unx.sas.com:8764";
    public static final String BASE_URL_FUSIONDEV1 = "http://fusiondev1.unx.sas.com:8764";
    public static final String BASE_URL_FUSIONDEV3 = "http://fusiondev3.unx.sas.com:8764";

    /**
     * fusion 3.1 instance DEV
     */
    public static final String BASE_URL_31_INT_DEV = "http://stereo.unx.sas.com:8764";
    public static final String BASE_URL_31_INT_DEV_HTTPS = "https://lucidintdev1.unx.sas.com:8443";
    public static final String BASE_URL_31_INT_STAGE_HTTPS = "https://lucidintstage1.unx.sas.com:8443";
    public static final String BASE_URL_410_EXT_STAGE = "https://lucidextstage1.exnet.sas.com:8443";

    public static final String INTRANET_COLLECTION = "Intranet_v1";
    private String collectionName;
    private static Logger LOG = LoggerFactory.getLogger(FusionManagerRestClient.class);

    Cookie sessionCookie = null;

    @BeforeMethod
    public void setup() {
        client = new FusionManagerRestClient();
//        client.setBaseUrl(BASE_URL_CHICAGO);
//        client.setBaseUrl(BASE_URL_FUSIONDEV1);
//        client.setBaseUrl(BASE_URL_31_INT_DEV_HTTPS);
        client.setBaseUrl(BASE_URL_31_INT_STAGE_HTTPS);
//        client.setBaseUrl(BASE_URL_410_EXT_STAGE);
//        client.setAuthenticationRealm("na.sas.com");

        //Add authenticated session cookie
        if (sessionCookie == null) {
            sessionCookie = client.session(AUTHENTICATION_2, true);
//            sessionCookie = client.session("admin:admin123",false);
        }
        client.addCookie(sessionCookie);
    }


    @Test
    public void testQueryPipelineList() {
        LOG.info("testing");
        List<QueryPipeline> results = client.getQueryPipelines();
        QueryPipeline tester = results.get(2);
        String temp = String.valueOf(tester.get("secretSourcePipelineId"));
        assertTrue("No response from query pipelines!", results.size() > 2);
    }

    @Test
    public void testIndexPipelines() {
//        String json = client.getIndexPipelines();
        List<IndexPipeline> results = client.getIndexPipelines();
        assertTrue("No response from index pipelines! " + client.getBaseUrl(), results.size() > 1);
    }


    @Test
    public void testObjectTree() {
//        List<FusionObject> results = client.getFusionObjects("collection.ids=intranet");
        List<FusionObject> results = client.getFusionObjects("collection.ids=intranet","type=datasource,query-pipeline,index-pipeline",
                "deep='true'");
        FusionObject obj = results.get(0);
        List<Datasource> dataSources = obj.getDatasources();
        List<FusionCollection> collections=obj.getCollections();
        assertTrue(dataSources.isEmpty());
        assertTrue("No objects returned",!results.isEmpty());
    }
    @Test
    public void testObjectGroup() {
        List<FusionObject> results = client.getFusionObjects("collection.ids=intranet","type=group,job,parser,spark,link",
                "deep='true'");
        FusionObject obj = results.get(0);
        List<SparkJob> sparkJobs = obj.getSparkJobs();
        List<FusionLink> links = obj.getLinks();
        List<FusionGroup> groups = obj.getGroups();
        assertTrue("No objects returned",!results.isEmpty());
    }
    @Test
    public void testRelatedLinks() {
        FusionLink sample=new FusionLink("collection:intranet",null,null);
        List<FusionLink> results=client.getRelatedLinks("collection:intranet",null,null);
        FusionLink one = results.get(0);
        String filename=sample.generateFileName();
        assertTrue("No results from related links",!results.isEmpty());

    }

    @Test
    public void testFeatures() {
        List<CollectionFeature> results = client.getCollectionFeatures("cdhub_search");
        assertTrue(!results.isEmpty());
    }
    @Test
    public void testAggregations() {
        List<Aggregator> results = client.getAggregations();
        assertTrue("No aggregators. Not a real problem", !results.isEmpty());
    }
    @Test
    public void testRoles() {
        List<Role> results = client.getRoles();
        Role testRole = results.get(0);
        assertTrue("No response from index pipelines!", results.size() > 1);
        assertNotNull(testRole.getId());
    }

    @Test
    public void testSystemInfo() {
        List<SystemInfo> results= client.getSystemInfo();
        SystemInfo info = results.get(0);
        assertNotNull(info.get("app.version"));
    }

    @Test
    public void testCollectionsList() {
        List<FusionCollection> results = client.getCollections(".*", "^((?!SIGNALS).)*$", Boolean.TRUE);
        String status = client.getCollectionsStatus();
        assertNotNull("No response from Collections", results.get(0).getId());
    }

    @Test
    public void testSolrConfigsForCollection() {
//        List<SolrConfigData> results = client.getSolrConfigData(INTRANET_COLLECTION);
        List<SolrConfigData> results = client.getSolrConfigData("Intranet");
        assertNotNull(results.get(2));
    }

    @Test
    public void testGetSolrConfigFile() throws IOException {
        List<SolrConfigData> results = client.getSolrConfigData(INTRANET_COLLECTION);
        String filename = results.get(6).getName();
        String solrconfig = client.getSolrConfigFile(INTRANET_COLLECTION, filename);
        String compareVal = "For more details about configurations options that may appear in";
        assertTrue(solrconfig.contains(compareVal));
    }
    @Test
    public void testReadSingleCollection() {
        String testCollection = "Intranet_v1";
        FusionCollection t = client.getCollection(testCollection);
        assertNotNull("Could not get collection " + testCollection + " from server " + client.getBaseUrl(), t.getId());

    }

    @Test
    public void testGetAllSolrForCollection() {
        String testCollection = INTRANET_COLLECTION;
        //Get all config file data and the files' contents.
        List<SolrConfigData> results = client.getSolrConfigData(testCollection, Boolean.TRUE);
        List<SolrConfigData> results2 = client.getSolrConfigData(testCollection);
        Map<String, SolrConfigData> allConfigs = results.stream()
                .collect(Collectors.toMap(x -> x.getName(), x -> x));
        assertEquals(allConfigs.size(), results.size());
    }

    @Test
    public void testCreateCollection() {
        collectionName = "FusionCollection-test-junit";
        FusionCollection c1 = new FusionCollection();
        c1.setId("junit-" + Calendar.getInstance().getTimeInMillis());
        c1.setType("DATA");
        c1.setSearchClusterId("default");
        SolrParams pr = new SolrParams();
        pr.setName(c1.getId());
        pr.setNumShards(2);
        pr.setReplicationFactor(2);
        pr.setMaxShardsPerNode(2);
        c1.setSolrParams(pr);

        //Good collection
        Response response2 = client.createCollection(c1);
        assertEquals("FusionCollection NOT created", Response.Status.OK.getStatusCode(), response2.getStatusInfo().getStatusCode());
        //Now delete
        Response response3 = client.deleteCollection(c1.getId());
        assertEquals("FusionCollection NOT Deleted", Response.Status.NO_CONTENT.getStatusCode(), response3.getStatusInfo().getStatusCode());
    }

    @Test
    public void testDatasources() throws Exception {
        List<Datasource> results = client.getDataSources();
        assertTrue("No datasources on getting datasources", !results.isEmpty());
    }

    @Test
    public void testGetSingleDatasource() throws Exception {
//        String datasourceName = "Internet_v1_SASCOM_Products";
        String datasourceName = "Intranet_v1-RND-ds";
        Datasource result = client.getDataSource(datasourceName);
        assertEquals("No datasources on getting datasources", datasourceName, result.id);
    }

    @Test
    public void readDataSourceFromFile() throws URISyntaxException, IOException {
        // fusionJson/datasources/sasit-ds-updated.json
        String filename = "fusionJson/datasources/sasit-ds-updated.json";
        Path path = Paths.get(getClass().getClassLoader().getResource(filename).toURI());
        StringBuilder sb = new StringBuilder();
        Stream<String> lines = Files.lines(path);
        lines.forEach(line -> sb.append(line).append("\n"));
        lines.close();
        ObjectMapper mapper = new ObjectMapper();
        Datasource t = readObjectFromJson(path, Datasource.class);

        assertEquals(t.getId(), "SasIT");
    }

    @Test
    public void readQueryPipelineFromFile() throws URISyntaxException, IOException {
        // fusionJson/datasources/sasit-ds-updated.json
        String filename = "fusionJson/query-pipelines/Intranet_Query_Pipeline.json";
        Path path = Paths.get(getClass().getClassLoader().getResource(filename).toURI());
        QueryPipeline t = readObjectFromJson(path, QueryPipeline.class);
        assertEquals(t.getId(), "Intranet_Query_Pipeline");
    }

    /**
     * Read and update a schedule by moving up 2 hours.
     */
    @Test
    public void getSchedules() {
        List<Schedule> schedules = client.getSchedules();
        assertTrue(schedules.size() > 2);

        Schedule first = schedules.get(0);
        Calendar dt = first.getStartTime();
        dt.add(Calendar.HOUR, 2);
        first.setStartTime(dt);
        Response response = client.updateOrCreate(first);
        //good responses could be 204 or 200
        assertTrue("Schedule " + first.getId() + " not updated",
                response.getStatus() <= Response.Status.NO_CONTENT.getStatusCode());
    }

    /**
     * Create a new schedule form a json template, and delete
     *
     * @throws URISyntaxException
     * @throws IOException
     */
    @Test
    public void createSchedule() throws URISyntaxException, IOException {
        String fileName = "fusionJson/schedules/test-schedule.json";
        Path path = Paths.get(getClass().getClassLoader().getResource(fileName).toURI());
        Schedule base = readObjectFromJson(path, Schedule.class);
        //We have the base schedule
        String testId = base.getId() + "-" + Calendar.getInstance().getTimeInMillis();
        base.setId(testId);
        base.setActive(Boolean.FALSE);

        Response response = client.updateOrCreate(base, "");
        assertEquals("Schedule NOT created", Response.Status.OK.getStatusCode(), response.getStatusInfo().getStatusCode());
        Schedule result = client.readScheduleFromResponse(response);
        Schedule result2 = client.getSchedule(testId);
        assertEquals(result.getStartTime(), base.getStartTime());
        assertEquals(result.getStartTime(), result2.getStartTime());
        Response deleteResponse = client.deleteSchedule(result);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), deleteResponse.getStatusInfo().getStatusCode());

    }

    /**
     * Read JSON from a path, and create an object of the valueType
     *
     * @param path      Path to the JSON
     * @param valueType class instance to create
     * @param <T>       Generic parameter
     * @return an instance of valueType
     * @throws IOException
     */
    private <T> T readObjectFromJson(Path path, Class<T> valueType) throws IOException {
        StringBuilder sb = new StringBuilder();
        Stream<String> lines = Files.lines(path);
        lines.forEach(line -> sb.append(line).append("\n"));
        lines.close();
        ObjectMapper mapper = new ObjectMapper();
        T retVal = mapper.readValue(sb.toString(), valueType);
        return retVal;
    }

}
