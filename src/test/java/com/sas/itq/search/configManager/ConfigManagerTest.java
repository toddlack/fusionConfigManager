package com.sas.itq.search.configManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.sas.itq.search.FusionManagerRestClient;
import com.sas.itq.search.configManager.connectors.Datasource;
import org.glassfish.jersey.client.ClientConfig;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.log4testng.Logger;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * TODO: Add class javadoc
 * User: snoctl
 * Date: 10/6/2017
 * Time: 8:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigManagerTest {
    private static final String PLATFORM_TEST_DATASOURCES = "src/test/resources/fusionJson/datasources";
    private static final String PLATFORM_TEST_OUT_DATASOURCES = "src/test/resources/output/datasources";
    private static final String PLATFORM_TEST_QUERY_PIPELINES = "src/test/resources/fusionJson/query-pipelines";
    private static final String PLATFORM_TEST_OUT_QUERY_PIPELINES = "src/test/resources/output/query-pipelines";
    private static final String PLATFORM_TEST_SCHEDULES = "src/test/resources/fusionJson/schedules";

    private static Logger LOG = Logger.getLogger(ConfigManagerTest.class);
    public static final String AUTHENTICATION_2 = "c25vY3RsOnNub2N0bDEyMw==";
    public static final String AUTHENTICATION_ADMIN = "c25vY3RsOnNub2N0bDEyMw==";
    public static final String AUTHENTICATION_SNOCTL = "c25vY3RsOnNub2N0bDEyMw==";
    public static final String BASE_URL_EXT_DEV = "http://aroma.exnet.sas.com:8764";
    private static final String BASE_URL_EXT_STAGE = "http://suzuki.exnet.sas.com:8764";
    private static final String BASE_URL_41_EXT_STAGE = "https://lucidextstage1.exnet.sas.com:8443";
    public static final String BASE_URL_INT_STAGE = "https://lucidintstage1.unx.sas.com:8443";
    /**
     * fusion 3.1 instance DEV
     */
    public static final String BASE_URL_31_INT_DEV = "https://lucidintdev1.unx.sas.com:8443";
    public static final String BASE_URL_31_INT_STAGE = "https://lucidintstage1.unx.sas.com:8443";

    public static final String BASE_URL_DEV1 = "http://fusiondev1.unx.sas.com:8764";

    ConfigManager manager;
    FusionManagerRestClient client;
    Cookie sessionCookie = null;


    @BeforeMethod
    public void setUp() throws Exception {
        String baseUrl = BASE_URL_INT_STAGE;
//        String baseUrl = BASE_URL_31_INT_DEV_HTTPS;
//        String baseUrl = BASE_URL_DEV3;
//        String baseUrl = BASE_URL_DEV1;
//        String baseUrl = BASE_URL_EXT_STAGE;
//        String baseUrl = BASE_URL_EXT_DEV;
        manager = new ConfigManager();
        ClientConfig config = new ClientConfig();
        client = new FusionManagerRestClient(config);
        changeUrl(client, baseUrl);
    }

    public FusionManagerRestClient changeUrl(FusionManagerRestClient fClient, String baseUrl) {
        fClient.setBaseUrl(baseUrl);
        //Get authentication session cookie
        fClient.setAuthenticationRealm("native");
//        fClient.setAuthenticationRealm("na.sas.com");
        boolean authEncoded = true;
        if (sessionCookie == null) {
            String creds="snoctl:snoctl123";
//            String creds="admin:admin123";
            sessionCookie = fClient.session(creds, !authEncoded);
        }
        fClient.addCookie(sessionCookie);
        return fClient;
    }

    @AfterMethod
    public void tearDown() throws Exception {
        LOG.info("test finished");
    }

    @Test
    public void testReadDataSources() throws Exception {
        List<Datasource> results = manager.readDataSourceFiles(PLATFORM_TEST_DATASOURCES);
        assertTrue(!results.isEmpty(), "no datasources read from file system");
    }

    @Test
    public void testWriteDataSources() {
        List<Datasource> sources = client.getDataSources();
        List<String> sourcesJson = client.getDataSourcesJson();
        File f = new File(PLATFORM_TEST_OUT_DATASOURCES);
        manager.writeDataSourcesToDirectory(sources, f.toPath());

    }

    @Test
    public void testWriteQueryPipelines() {
        List<QueryPipeline> sources = client.getQueryPipelines();
        File f = new File(PLATFORM_TEST_OUT_QUERY_PIPELINES);
        manager.writeQueryPipelinesToDirectory(sources, f.toPath());

    }

    @Test
    public void testUpdateRoles() {
        String pattern = "develop.*";
        client.setBaseUrl(BASE_URL_DEV1);
        List<File> fList = manager.listPaths("temp/dev3/roles", pattern)
                .collect(Collectors.toList());
        List<Role> sources = manager.readConfigSourceFiles(fList, Role.class);
        List<Response> responses = manager.updateRoles(client, sources);
        String t = responses.get(0).readEntity(String.class);
        assertEquals(responses.size(), sources.size(), "Role responses are not the same number as sources");

    }
    /**
     * Read datasource definition from file, add an updated description, and update on server.
     *
     * @throws Exception
     */
    @Test
    public void testUpdateDataSources() throws Exception {
        //Datasources that have  SASCOM-P* or SASCOM-N*
//        String pattern = "Internet_v1-SASCOM-[PN].*";
        String pattern = "C.*\\.json";
        List<File> fList = manager.listPaths("temp/int-dev3-stereo/datasources", pattern)
                .collect(Collectors.toList());
        List<Datasource> dataSources = manager.readDataSourceFiles(fList);
//        dataSources.get(0).setDescription(" description on " + Instant.now());
        List<Response> responses = manager.updateDataSources(client, dataSources);
        //Should say 'true' for puts.
        String t =  responses.get(0).readEntity(String.class);
        assertTrue(responses.size() > 1, "No responses");

    }

    @Test
    public void testUpdateQueryPipelines() {
        String pattern = "SASCOM_QP_EN_.*";
        List<File> fList = manager.listPaths(PLATFORM_TEST_QUERY_PIPELINES, pattern)
                .limit(5)
                .collect(Collectors.toList());
        List<QueryPipeline> qps = manager.readConfigSourceFiles(fList, QueryPipeline.class);
        List<Response> responses = manager.updateQueryPipelines(client, qps);
        responses.stream()
                .forEach(response -> {
                    assertTrue(response.getStatus() <= 226, "Bad response: " + response.readEntity(String.class));
                });
    }

    @Test
    public void testUpdateSchedules() {
        String pattern = "_support-books.json";
        List<File> fList = manager.listPaths(PLATFORM_TEST_SCHEDULES, pattern)
                .collect(Collectors.toList());
        List<Schedule> sources = manager.readConfigSourceFiles(fList, Schedule.class);
        List<Response> responses = manager.update(client, sources, Schedule.class);
        responses.stream()
                .forEach(response -> {
                    assertTrue(response.getStatus() <= 226, "Bad response: " + response.readEntity(String.class));
                });
    }

    /**
     * Test copying just two specific types.
     *
     * @throws IOException
     */
    @Test
    public void testCopyTypeFromServer() throws IOException {
        String directory = "temp/stage313";
        client = changeUrl(client, BASE_URL_INT_STAGE);
        EntityType[] typesToCopy = {EntityType.COLLECTION, EntityType.DATASOURCE, EntityType.INDEX_PIPELINE, EntityType.SCHEDULE};
        Map<String, Boolean> results = manager.copyFromServer(client, directory, typesToCopy, "temp");
        results.forEach((key, value) -> assertTrue(value, "file " + key + " did not get written"));
    }

    /**
     * Test copying just two specific types.
     *
     * @throws IOException
     */
    @Test
    public void testCopyTypeFromV3Server() throws IOException {
        String directory = "temp/int-dev3-stereo";
        client = changeUrl(client, BASE_URL_31_INT_DEV);
        EntityType[] typesToCopy = {EntityType.JOB, EntityType.PARSER, EntityType.DATASOURCE, EntityType.INDEX_PIPELINE, EntityType.SCHEDULE};
        Map<String, Boolean> results = manager.copyFromServer(client, directory, typesToCopy, "temp");
        results.forEach((key, value) -> assertTrue(value, "file " + key + " did not get written"));
    }

    @Test
    public void readCollectionFiles() {
        String directory="temp/stage313/collections";
        client = changeUrl(client, BASE_URL_31_INT_DEV);

    }

    @Test
    public void testGetServerInfo() throws IOException {
        String directory="temp/info";
        client = changeUrl(client, BASE_URL_DEV1);
        EntityType[] typesToCopy = {EntityType.SYSINFO};
        Map<String, Boolean> results = manager.copyFromServer(client, directory, typesToCopy, "extStage");
        results.forEach((key, value) -> assertTrue(value, "file " + key + " did not get written"));
    }
    /**
     * Test copying just two specific types.
     *
     * @throws IOException
     */
    @Test
    public void testCopyTypeJsonFromServer() throws IOException {
        String directory = "temp/ext-stage";
        client = changeUrl(client, BASE_URL_EXT_STAGE);
        EntityType[] typesToCopy = {EntityType.QUERY_PIPELINE};
        Map<String, Boolean> results = manager.copyFromServer(client, directory, typesToCopy, "temp");
        results.forEach((key, value) -> assertTrue(value, "file " + key + " did not get written"));
    }

    /**
     * Test using the node predicate
     * @throws IOException
     */
    @Test
    public void testCopyTypeJsonFromServerWithFilter() throws IOException {
        String directory = "temp/int-stage";
        Predicate<JsonNode> testPredDatasource = ConfigManager.nodePredicate("cd.*","id")
                .or(ConfigManager.nodePredicate("c360.*","id"));
        client = changeUrl(client, BASE_URL_INT_STAGE);
        EntityType[] typesToCopy = {EntityType.QUERY_PIPELINE,EntityType.AGGREGATION};
        Map<String, Boolean> results = manager.copyFromServer(client, directory, typesToCopy, "temp",testPredDatasource);
        results.forEach((key, value) -> assertTrue(value, "file " + key + " did not get written"));
    }

    @Test
    public void testCopyFromServer() throws IOException {
        String directory = "temp/stage313";
        Predicate<JsonNode> testPredDatasource = ConfigManager.nodePredicate("cd.*","id")
                .or(ConfigManager.nodePredicate("c360.*","id"));
        client = changeUrl(client, BASE_URL_INT_STAGE);
        Map<String, Boolean> results = manager.copyAllFromServer(client, directory);
        Path p1 = Paths.get(directory);
        assertTrue(Files.list(p1).count() > 2);
        results.forEach((key, value) -> assertTrue(value, "file " + key + " did not get written"));
    }


    @Test
    public void testCopyObjectFromServer() throws IOException {
        String directory = "temp/stage313";
        Path outputDirectory=Paths.get(directory);
        client = changeUrl(client, BASE_URL_31_INT_STAGE);
        Boolean separateFiles = Boolean.TRUE;
        Map<String,Boolean> results = manager.copyFromServer(client,outputDirectory,"",separateFiles,"collection.ids=intranet","type=datasource,query-pipeline,index-pipeline",
                "deep='true'");
        results.forEach((key, value) -> assertTrue(value, "file " + key + " did not get written"));
    }

    @Test
    public void testProfilesFromServer() throws IOException {
        String directory = "temp/stage313";
        Path outputDirectory=Paths.get(directory);
        client = changeUrl(client, BASE_URL_31_INT_STAGE);
        EntityType[] typesToCopy = {EntityType.QUERY_PROFILE,EntityType.INDEX_PROFILE,EntityType.AGGREGATION};
        Map<String,Boolean> results = manager.copyFromServer(client,directory,typesToCopy);
        results.forEach((key, value) -> assertTrue(value, "file " + key + " did not get written"));
    }

    @Test
    public void testCopySeparateToServer() throws IOException {
        String directory = "temp/dev3";
        Predicate<JsonNode> testPredDatasource = ConfigManager.nodePredicate("cd.*","id")
                .or(ConfigManager.nodePredicate("c360.*","id"));
        client = changeUrl(client, BASE_URL_INT_STAGE);
//        EntityType[] typesToCopy = {EntityType.COLLECTION};
        EntityType[] typesToCopy = {EntityType.DATASOURCE};
        List<Response> results = manager.copyToServer(client, directory, typesToCopy);
        results.forEach(response -> {
            int stat = response.getStatus();
            assertTrue(stat >= Response.Status.OK.getStatusCode() && stat <= Response.Status.PARTIAL_CONTENT.getStatusCode(),
                    "Problem updating server " + client.getBaseUrl() + " -- return code was " + stat + " for " + response.readEntity(String.class));
        });
    }

    @Test(enabled = true)
    public void testCopyToServer() throws IOException {
        String directory = "temp/ext-stage";
        client.setBaseUrl(BASE_URL_EXT_STAGE);
        List<Response> results = manager.copyAllToServer(client, directory);
        Path p1 = Paths.get(directory);
        results.forEach(response -> {
            assertEquals(response.getStatus(), Response.Status.OK.getStatusCode(), "Problem with " + directory + ":" + response.readEntity(String.class));
        });
        assertTrue(Files.list(p1).count() > 2);
    }
}