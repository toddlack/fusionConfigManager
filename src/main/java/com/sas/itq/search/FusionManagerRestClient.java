package com.sas.itq.search;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sas.itq.search.configManager.*;
import com.sas.itq.search.configManager.connectors.Datasource;
import org.glassfish.jersey.internal.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.*;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.*;
import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Client to access fusion's Rest APIs for managing a server. Not for querying.
 */
public class FusionManagerRestClient implements Client {
    public static final String SOLR_BASE = "/api/apollo/solr";
    public static final String DATASOURCES_BASE = "/api/apollo/connectors/datasources";
    public static final String COLLECTIONS_BASE = "/api/apollo/collections";
    public static final String COLLECTIONS_STATUS_BASE = "/api/apollo/collections/status";
    public static final String COLLECTIONS_FEATURES_BASE = "/api/apollo/collections/{1}/features";
    public static final String QUERY_PROFILE_BASE="/api/apollo/collections/{1}/query-profiles";
    public static final String INDEX_PROFILE_BASE="/api/apollo/collections/{1}/index-profiles";
    public static final String QUERY_PIPELINES_BASE = "/api/apollo/query-pipelines";
    public static final String GROUP_BASE = "/api/apollo/groups";
    public static final String INDEX_PIPELINES_BASE = "/api/apollo/index-pipelines";
    public static final String SCHEDULER_BASE = "/api/apollo/jobs/{1}/schedule";
    public static final String AGGREGATOR_BASE = "/api/apollo/spark/configurations"; //as of v3.1
    public static final String PARSER_BASE = "/api/apollo/parsers";
    public static final String JOB_BASE = "/api/apollo/jobs";
    public static final String ROLE_BASE = "/api/roles";
    public static final String USER_BASE = "/api/users";
    public static final String SYSINFO_BASE = "/api/apollo/configurations";
    public static final String COLLECTION_SOLR_BASE = "/api/apollo/collections/{1}/solr-config";
    public static final String SESSION_BASE = "/api/session";
    public static final String LINK_BASE = "/api/apollo/links";
    public static final String OBJECTS_BASE = "/api/apollo/objects";


    Client client;
    Configuration clientConfig;
    String baseUrl;
    String authenticationHeader;
    String authenticationRealm = "native";
    List<Cookie> cookies = new ArrayList<>();

    private static Logger log = LoggerFactory.getLogger(FusionManagerRestClient.class);
    private static final Map<Class, String> classBaseMap;

    static {
        classBaseMap = new HashMap<>();
        classBaseMap.put(Datasource.class, DATASOURCES_BASE);
        classBaseMap.put(IndexPipeline.class, INDEX_PIPELINES_BASE);
        classBaseMap.put(QueryPipeline.class, QUERY_PIPELINES_BASE);
        classBaseMap.put(FusionCollection.class, COLLECTIONS_BASE);
        classBaseMap.put(Schedule.class, SCHEDULER_BASE);
        classBaseMap.put(Role.class, ROLE_BASE);
        classBaseMap.put(User.class, USER_BASE);
        classBaseMap.put(Aggregator.class, AGGREGATOR_BASE);
        classBaseMap.put(Parser.class, PARSER_BASE);
        classBaseMap.put(Job.class, JOB_BASE);
        classBaseMap.put(FusionGroup.class, GROUP_BASE);
        classBaseMap.put(SolrConfigData.class, COLLECTION_SOLR_BASE);
        classBaseMap.put(CollectionFeature.class, COLLECTIONS_FEATURES_BASE);
        classBaseMap.put(SystemInfo.class, SYSINFO_BASE);
        classBaseMap.put(FusionLink.class, LINK_BASE);
        classBaseMap.put(FusionObject.class, OBJECTS_BASE);
        classBaseMap.put(QueryProfile.class, QUERY_PROFILE_BASE);
        classBaseMap.put(IndexProfile.class, INDEX_PROFILE_BASE);

    }

    public FusionManagerRestClient() {
        client = ClientBuilder.newBuilder()
                .build();

    }

    public FusionManagerRestClient(Configuration clientConfig) {
        this.clientConfig = clientConfig;
        client = ClientBuilder.newBuilder()
                .withConfig(clientConfig).build();
    }

    public List<String> getAsJson(Class typeClass) {
        return getAsJson(typeClass, ConfigManager.nullPredicate());
    }

    /**
     * when calling the GETs for a given entityType, like datasources, you get the whole set - there is no way to get
     * just the names, or just the entities that match a name. So if filter is specified, pare down the list before returning.
     *
     * @param typeClass
     * @param nodeFilter Filter by json node properties
     * @return
     */
    public List<String> getAsJson(Class typeClass, Predicate<JsonNode> nodeFilter) {
        List<JsonNode> nodes = getAsJsonNodes(typeClass, nodeFilter);
        ObjectMapper mp = new ObjectMapper();
        List<String> jsonList = nodes.stream()
                .map(node -> {
                    String retVal = "";
                    try {
                        retVal = mp.writerWithDefaultPrettyPrinter().writeValueAsString(node);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                    return retVal;
                })
                .collect(Collectors.toList());
        return jsonList;
    }

    public List<JsonNode> getAsJsonNodes(Class typeClass, Predicate<JsonNode> nodeFilter) {
        return getAsJsonNodes(classBaseMap.get(typeClass), nodeFilter);
    }

    public List<JsonNode> getAsJsonNodes(String uri, Predicate<JsonNode> nodeFilter) {
        Invocation.Builder invBuilder = getInvocationBuilder(uri, "wt=json");
        String json = invBuilder.accept(MediaType.APPLICATION_JSON_TYPE).get(String.class);
        //If a single json object, wrap in square brackets to make it an array of 1 element
        if (!json.startsWith("[")) {
            json = new StringBuilder("[").append(json).append("]").toString();
        }
        return getJsonNodesFiltered(json,nodeFilter);
    }

    public List<JsonNode> getJsonNodesFiltered(String json, Predicate<JsonNode> nodeFilter) {
        List<JsonNode> nodes = null;
        JsonNode root = buildJsonRootNode(json);
        if (root != null) {
            nodes = StreamSupport.stream(Spliterators.spliteratorUnknownSize(root.iterator(), Spliterator.ORDERED), false)
                    .filter(nodeFilter)
                    .collect(Collectors.toList());
        }
        return nodes;
    }

    public JsonNode buildJsonRootNode(String json) {
        ObjectMapper mp = new ObjectMapper();
        JsonNode root = null;
        try {
            root = mp.readTree(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return root;
    }


    public List<QueryPipeline> getQueryPipelines() {
        Invocation.Builder invBuilder = getInvocationBuilder(QUERY_PIPELINES_BASE, "");
        List<QueryPipeline> results = getResults(QueryPipeline.class, invBuilder, MediaType.APPLICATION_JSON_TYPE);
        return results;
    }

    public List<Job> getJobs() {
        Invocation.Builder invBuilder = getInvocationBuilder(JOB_BASE, "");
        List<Job> results = getResults(Job.class, invBuilder, MediaType.APPLICATION_JSON_TYPE);
        return results;
    }

    public List<SystemInfo> getSystemInfo() {
        Invocation.Builder invBuilder = getInvocationBuilder(SYSINFO_BASE, "");
        SystemInfo result = invBuilder.get(SystemInfo.class);
        List<SystemInfo> results = new ArrayList<>();
        results.add(result);
        return results;
    }

    public QueryPipeline getQueryPipeline(String pipelineId) {
        Invocation.Builder invBuilder = getInvocationBuilder(QUERY_PIPELINES_BASE + "/" + pipelineId, "");
        QueryPipeline result = invBuilder.get(QueryPipeline.class);
        return result;
    }

    public List<IndexPipeline> getIndexPipelines() {
        Invocation.Builder invBuilder = getInvocationBuilder(INDEX_PIPELINES_BASE, "");
        List<IndexPipeline> results = getResults(IndexPipeline.class, invBuilder, MediaType.APPLICATION_JSON_TYPE);
        return results;
    }

    //TODO: MAke paramaters optional
    public List<Datasource> getDataSources() {
        Invocation.Builder invBuilder = getInvocationBuilder(DATASOURCES_BASE);
        List<Datasource> dataSourceList = getResults(Datasource.class, invBuilder, MediaType.APPLICATION_JSON_TYPE);
        return dataSourceList;
    }

    public List<Role> getRoles() {
        Invocation.Builder invBuilder = getInvocationBuilder(ROLE_BASE);
        List<Role> resultList = getResults(Role.class, invBuilder, MediaType.APPLICATION_JSON_TYPE);
        return resultList;
    }

    public List<Parser> getParsers() {
        Invocation.Builder invBuilder = getInvocationBuilder(PARSER_BASE);
        List<Parser> resultList = getResults(Parser.class, invBuilder, MediaType.APPLICATION_JSON_TYPE);
        return resultList;
    }

    public List<User> getUsers() {
        Invocation.Builder invBuilder = getInvocationBuilder(USER_BASE);
        List<User> resultList = getResults(User.class, invBuilder, MediaType.APPLICATION_JSON_TYPE);
        return resultList;
    }

    public List<SolrConfigData> getSolrConfigData(String collectionName, Boolean includeFileData) {
        Invocation.Builder invBuilder = getInvocationBuilder(COLLECTION_SOLR_BASE.replace("{1}", collectionName), "recursive=true");
        List<SolrConfigData> results = getResults(SolrConfigData.class, invBuilder, MediaType.APPLICATION_JSON_TYPE);
        List<SolrConfigData> retVal = addCollectionAndFile(results, collectionName, includeFileData);
        return retVal;
    }

    /**
     * Get a collections features - examples are: signals enbaled? logs endabled?
     *
     * @param collectionName
     * @return List of all features fro the collection
     */
    public List<CollectionFeature> getCollectionFeatures(String collectionName) {
        Invocation.Builder invBuilder = getInvocationBuilder(COLLECTIONS_FEATURES_BASE.replace("{1}", collectionName));
        List<CollectionFeature> results = getResults(CollectionFeature.class, invBuilder, MediaType.APPLICATION_JSON_TYPE);
        return results;
    }

    public List<QueryProfile> getQueryProfiles(String collectionName) {
        Invocation.Builder invBuilder = getInvocationBuilder(QUERY_PROFILE_BASE.replace("{1}", collectionName));
        List<QueryProfile> results = getResults(QueryProfile.class,invBuilder,MediaType.APPLICATION_JSON_TYPE);
        return results;
    }

    /**
     * Get the schedule for each job in the list
     * @param jobs
     * @return List of schedules
     */
    public List<Schedule> getSchedules(List<String> jobs) {
        List<Schedule> returnList = jobs.stream()
                .map(job -> getSchedule(job))
                .collect(Collectors.toList());
        return returnList;
    }

//    public List<Schedule> getSchedules(Predicate<JsonNode> jobFilter) {
//        ObjectMapper mapper = new ObjectMapper();
//        List<JsonNode> jobs = getAsJsonNodes(Job.class,jobFilter);
//        List<Job> jobList = jobs.stream()
//                .map(jsonNode -> {
//                    Job j = null;
//                    try {
//                        j = mapper.readValue(jsonNode.asText(),Job.class);
//                        return j;
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    return j;
//                })
//                .collect(Collectors.toList());
//        List<Schedule> returnList = new ArrayList<>();
//
//        return returnList;
//
//    }

    public List<Aggregator> getAggregations() {
        Invocation.Builder invBuilder = getInvocationBuilder(AGGREGATOR_BASE + "/aggregations");
        List<String> aggregatorIds = getResults(String.class, invBuilder, MediaType.APPLICATION_JSON_TYPE);
        List<Aggregator> aggregatorList = aggregatorIds.stream()
                .map(aggId -> getAggregator(aggId))
                .collect(Collectors.toList());

        return aggregatorList;
    }

    public Aggregator getAggregator(String id) {
        Invocation.Builder invBuilder = getInvocationBuilder(AGGREGATOR_BASE + "/aggregations/" + id);
        return invBuilder.get(Aggregator.class);
    }


    /**
     * add file contents to the Value field
     *
     * @param data
     */
    private List<SolrConfigData> addCollectionAndFile(List<SolrConfigData> data, String collectionName, Boolean includeFileData) {
        if (collectionName != null) {
            data.forEach(item -> {
                item.setCollection(collectionName);
                if (item.getDir()) {
                    addCollectionAndFile(item.getChildren(), collectionName, includeFileData);
                } else if (includeFileData) {
                    item.setValue(getSolrConfigFile(item.getCollection(), item.generateFileName()));
                }
            });
        }
        return data;
    }

    /**
     * Get all solr-config info for a collection.
     *
     * @param collectionName
     * @return
     */
    public List<SolrConfigData> getSolrConfigData(String collectionName) {
        return getSolrConfigData(collectionName, false);
    }

    /**
     * Get a single config file for a collection.
     *
     * @param collectionName
     * @param fileName
     * @return String that can be saved to a file.
     */
    public String getSolrConfigFile(String collectionName, String fileName) {
        Invocation.Builder invBuilder = getInvocationBuilder(COLLECTION_SOLR_BASE.replace("{1}", collectionName) + "/" + fileName, "expand=true");
        SolrConfigData result = invBuilder.accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML).get(SolrConfigData.class);
        String solrconfig = result.getValue();
        StringBuilder retVal = new StringBuilder();
        if (solrconfig != null) {
            //This is a large base64 encoded string that turns out to be the file.
            //Use the buffered input reader to read a line at a time and append to retVal.
            InputStream in1 = new ByteArrayInputStream(solrconfig.getBytes());
            InputStream decode64Stream = java.util.Base64.getDecoder().wrap(in1);
            BufferedReader reader = new BufferedReader(new InputStreamReader(decode64Stream));
            reader.lines()
                    .forEach(line -> retVal.append(line).append("\n"));
        }
        return retVal.toString();
    }

    public List<String> getDataSourcesJson() {
        Invocation.Builder invBuilder = getInvocationBuilder(DATASOURCES_BASE);
        String json = invBuilder.accept(MediaType.APPLICATION_JSON_TYPE).get(String.class);
        List<String> dataSourceList = new ArrayList<>();
        ObjectMapper mp = new ObjectMapper();
        try {
            JsonNode root = mp.readTree(json);
            Iterator t = root.elements();
            while (t.hasNext()) {
                JsonNode curNode = (JsonNode) t.next();
                dataSourceList.add(curNode.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataSourceList;
    }


    public Datasource getDataSource(String datasourceName) {
        String queryParameters = "wt=json";
        Invocation.Builder invBuilder = getInvocationBuilder(DATASOURCES_BASE + "/" + datasourceName);
        Response response = invBuilder.get();
        Datasource dataSource = null;
        if (response.getStatusInfo().getStatusCode() == Response.Status.OK.getStatusCode()) {
            dataSource = response.readEntity(Datasource.class);
        }
        return dataSource;
    }

    /**
     * API call to update a solr configuration file - used in updating solr files for a collection.<p>
     * Curl cmd:
     * curl 'http://plasma.unx.sas.com:8764/api/apollo/collections/Intranet_v1/solr-config/solrconfig.xml?reload=true'
     * -X PUT -H 'Pragma: no-cache'
     * --data-binary $'the string body to post'
     * --compressed
     *
     * @param configName name of the file, which is the last part of the API URL path: http://plasma.unx.sas.com:8764/api/apollo/collections/Intranet_v1/solr-config/<b>solrconfig.xml</b>
     * @param configBody
     * @return
     */
    public Integer updateSolrConfig(String configName, String configBody) {

        log.info("Updating {}", configName);
        return 0;
    }

    /**
     * create a builder with default headers and query parameters
     *
     * @param url
     * @param queryParams
     * @return
     */
    private Invocation.Builder getInvocationBuilder(String url, String... queryParams) {
        WebTarget target = buildWebTarget(url, queryParams);
        return getInvocationBuilder(target);
    }

    private Invocation.Builder getInvocationBuilder(WebTarget target) {
        Invocation.Builder invBuilder = target.request(MediaType.APPLICATION_JSON_TYPE);
//        invBuilder.header("Authorization", "Basic " + authenticationHeader);
        getCookies().forEach(c -> invBuilder.cookie(c));
        return invBuilder;
    }

    private Invocation.Builder getInvocationBuilder(String url) {

        return getInvocationBuilder(url, "");
    }

    /**
     * Generate a web target for a given resource and RestQueryParameters object.
     *
     * @param resourceName
     * @param queryParameters
     * @return
     */
    public WebTarget buildWebTarget(String resourceName, String... queryParameters) {
        //Set up the resource with the path and path parameters, including the API key
        UriBuilder uriBuilder = UriBuilder.fromUri(baseUrl);
        uriBuilder.path(resourceName);
        buildFromQueryParameters(uriBuilder, queryParameters);
        return client.target(uriBuilder);
    }

    /**
     * Take a string of queryParameters and create a name-value pairs to add to Uri as query parameters
     *
     * @param uriBuilder      an existing uribuilder
     * @param queryParameters url query parameters to convert. Can be an empty String -if so, nothing happens.
     */
    private void buildFromQueryParameters(UriBuilder uriBuilder, String... queryParameters) {
        for (String param : queryParameters) {
            if (param != null) {
                String vals[] = param.split("=");
                if (vals.length > 1) {
                    uriBuilder.queryParam(vals[0], vals[1]);
                }
            }
        }
    }

    public Configuration getClientConfig() {
        return clientConfig;
    }

    public void setClientConfig(Configuration clientConfig) {
        this.clientConfig = clientConfig;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAuthenticationHeader() {
        return authenticationHeader;
    }

    public void setAuthenticationHeader(String authenticationHeader) {
        this.authenticationHeader = authenticationHeader;
    }

    public FusionCollection getCollection(String id) {
        Invocation.Builder invBuilder = getInvocationBuilder(COLLECTIONS_BASE + "/" + id, "");
        FusionCollection result = invBuilder.get(FusionCollection.class);
        return result;
    }

    public List<FusionCollection> getCollections() {
        return getCollections(".*");
    }

    public List<FusionCollection> getCollections(String typeRegex) {
        return getCollections(".*", typeRegex, Boolean.TRUE);
    }

    public List<FusionCollection> getCollections(String nameFilter, String collectionTypeRegex, Boolean exclude_system_collections) {
        String filterType = (collectionTypeRegex != null) ? collectionTypeRegex : ".*";
        Invocation.Builder invBuilder = getInvocationBuilder(COLLECTIONS_BASE, "");
        List<FusionCollection> results = getResults(FusionCollection.class, invBuilder, MediaType.APPLICATION_JSON_TYPE);
        List<FusionCollection> returnList = results.stream()
                .filter(item -> item.getId().matches(nameFilter))
                .filter(item -> exclude_system_collections && !item.getId().matches("^system\\_.*"))
                .filter(item -> filterType.isEmpty() || item.getType().matches(filterType))
                .collect(Collectors.toList());
        return returnList;
    }

    public String getCollectionsStatus() {
        Invocation.Builder invBuilder = getInvocationBuilder(COLLECTIONS_STATUS_BASE, "");
        Response response = invBuilder.get();
        String results = response.readEntity(String.class);
        return results;
    }


    public List<FusionLink> getRelatedLinks(String subject, String object, String linkType) {
        FusionLink sample = new FusionLink(subject, object, linkType);
        Invocation.Builder invBuilder = getInvocationBuilder(LINK_BASE, sample.getPathSegmentName());
        String json = invBuilder.accept(MediaType.APPLICATION_JSON_TYPE).get(String.class);
        List<FusionLink> results = getResults(FusionLink.class, invBuilder, MediaType.APPLICATION_JSON_TYPE);
        return results;
    }

    public List<FusionObject> getFusionObjects(String... queryParms) {
        Invocation.Builder invBuilder = getInvocationBuilder(OBJECTS_BASE + "/export", queryParms);

        FusionObject result = invBuilder.accept(MediaType.APPLICATION_JSON_TYPE).get(FusionObject.class);
        List<FusionObject> results = new ArrayList<>();
        results.add(result);
        return results;
    }

    public String getFusionObjectsAsJson(String... queryParms) {
        Invocation.Builder invBuilder = getInvocationBuilder(OBJECTS_BASE + "/export", queryParms);
        String json = invBuilder.accept(MediaType.APPLICATION_JSON_TYPE).get(String.class);
        return json;
    }

    /**
     * call /scheduler/schedules
     *
     * @return
     */
    public List<Schedule> getSchedules() {
        Invocation.Builder invBuilder = getInvocationBuilder(SCHEDULER_BASE, "");
        List<Schedule> results = getResults(Schedule.class, invBuilder, MediaType.APPLICATION_JSON_TYPE);
        return results;
    }

    /**
     * Add a JSON doc to the specified collection
     *
     * @param jsonDoc
     * @param indexPipeline
     * @param collection
     * @return the response of the api call
     */
    public Response addDocumentToIndex(String jsonDoc, String indexPipeline, String collection) {
        UriBuilder uriBuilder = UriBuilder.fromUri(baseUrl)
                .path(INDEX_PIPELINES_BASE)
                .path(indexPipeline)
                .path("collections")
                .path(collection);
        Response response = getInvocationBuilder(uriBuilder.toString(), "")
                .post(Entity.entity(jsonDoc, MediaType.APPLICATION_JSON_TYPE));
        return response;
    }

    public Response removeDocument(String docId, String collectionName) {
        return null;
    }


    /**
     * https://doc.lucidworks.com/fusion/2.4/REST_API_Reference/Collections-API.html#create-list-update-or-delete-collections
     * curl command : curl 'http://plasma.unx.sas.com:8764/api/apollo/collections' -H 'Pragma: no-cache'
     * -H 'Content-Type: application/json' -H 'Accept: application/json, query/plain, * /*'
     * -H 'Connection: keep-alive' --data-binary '{"id":"junk2"}' --compressed
     * {id: "t12", searchClusterId: "default", solrParams: {replicationFactor: 2, numShards: 2}}
     *
     * @param c1
     * @return
     */
    public Response createCollection(FusionCollection c1) {
        String queryParameters = "";
        Map<String, Object> pMap = new HashMap<>();
        Map<String, Object> sParms = new HashMap<>();
        sParms.put("numShards", c1.getSolrParams().getNumShards());
        sParms.put("replicationFactor", c1.getSolrParams().getReplicationFactor());
        //Api seems a bit sensitive. Just include the minimal amount of parameters, and none of the other attruibutes
        //that are in the FusionCollection object.
        pMap.put("id", c1.getId());
        pMap.put("searchClusterId", c1.getSearchClusterId());
        if (sParms.get("numShards") != null) {
            pMap.put("solrParams", sParms);
        }
        Invocation.Builder invBuilder = getInvocationBuilder(COLLECTIONS_BASE + "/" + c1.getId(), queryParameters);
        Entity<?> ent = Entity.entity(pMap, MediaType.APPLICATION_JSON_TYPE);
        Response response = invBuilder.put(ent);
        FusionCollection one = response.readEntity(FusionCollection.class);
        return response;

    }


    public Response deleteCollection(String collectionName) {
        Invocation.Builder invBuilder = getInvocationBuilder(COLLECTIONS_BASE + "/" + collectionName, "");
        Response response = invBuilder.delete();
        return response;
    }

    public Response updateOrCreate(Role source) {
        return updateOrCreate(source, "");
    }

    public Response updateOrCreate(Datasource source) {
        return updateOrCreate(source, "");
    }

    public Response updateOrCreate(QueryPipeline source) {
        return updateOrCreate(source, "");
    }

    public Response updateOrCreate(Schedule source) {
        return updateOrCreate(source, "");
    }

    public Response updateOrCreate(IndexPipeline source) {
        return updateOrCreate(source, "");
    }
    public Response updateOrCreate(Job source) {
        return updateOrCreate(source, "");
    }


    public <T> T readEntity(Class<T> entityClass, Response response) {
        return response.readEntity(entityClass);
    }

    public Response updateOrCreate(Datasource source, String queryParameters) {
        //Update the datasource
        WebTarget target = buildWebTarget(DATASOURCES_BASE, queryParameters);
        return checkUpdateOrCreate(source, target, source.getId());
    }

    /**
     * USe generic to update or create an entity
     *
     * @param source          The source.
     * @param queryParameters any query parameters.
     * @param sourceClass     The class of the source - used to lookup correct URI path.
     * @param <T>
     * @return the response of the request/
     */
    public <T extends IdentifiableString> Response updateOrCreate(T source, String queryParameters, Class<T> sourceClass) {
        WebTarget target = buildWebTarget(classBaseMap.get(sourceClass), queryParameters);
        return checkUpdateOrCreate(source, target, source.getPathSegmentName());
    }

    public Response updateOrCreate(CollectionFeature source, String queryParameters) {
        String baseSource = classBaseMap.get(CollectionFeature.class).replace("{1}", source.getCollectionId());
        WebTarget target = buildWebTarget(baseSource, queryParameters);
        return checkUpdateOrCreate(source, target, source.getPathSegmentName());

    }

    //Each source is specific for solrconfig
    public Response updateOrCreate(SolrConfigData source, String queryParameters) {
        String baseSource = classBaseMap.get(SolrConfigData.class).replace("{1}", source.getCollection());
        String pathSegmentName = source.getPathSegmentName();
        WebTarget target = buildWebTarget(baseSource, queryParameters).path(pathSegmentName);
        Response response = getInvocationBuilder(target).put(Entity.entity(source.getValue(), MediaType.TEXT_PLAIN));
        if (response.getStatus()>399) {
            //Post - this is a new one
            response = getInvocationBuilder(target).post(Entity.entity(source.getValue(), MediaType.TEXT_PLAIN));
        }
        return response;
    }

    public Response updateOrCreate(Role source, String queryParameters) {
        return updateOrCreate(source, queryParameters, Role.class);
    }

    public Response updateOrCreate(QueryPipeline source, String queryParameters) {
        return updateOrCreate(source, queryParameters, QueryPipeline.class);
    }

    public Response updateOrCreate(Schedule source, String queryParameters) {
        return updateOrCreate(source, queryParameters, Schedule.class);
    }

    public Response updateOrCreate(IndexPipeline source, String queryParameters) {
        return updateOrCreate(source, queryParameters, IndexPipeline.class);
    }
    public Response updateOrCreate(Job source, String queryParameters) {
        return updateOrCreate(source, queryParameters, Job.class);
    }

    public Response checkUpdateOrCreate(IdentifiableString source, WebTarget target, String idPath) {

        Response response = null;
        boolean doPost = false;
        ObjectMapper mp = new ObjectMapper();
        String jsonSource = "";
        try {
            jsonSource = mp.writeValueAsString(source);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        if (idPath == null) {
            doPost = true;
        } else {
            Invocation.Builder ibWithId = getInvocationBuilder(target.path(idPath));
            Response testResponse = ibWithId.get();
            //The rest api for a collection uses put to update AND create. So if a collection, use PUT
            doPost = Response.Status.OK.getStatusCode() != testResponse.getStatusInfo().getStatusCode();
        }
        if (doPost) {
            /*For POST, we need to drop the last part of the path, which is the entity ID
            http://fusiondev1.unx.sas.com:8764/api/apollo/connectors/datasources/Intranet_v1-RND-ds
            */
            response = getInvocationBuilder(target).post(Entity.entity(source, MediaType.APPLICATION_JSON_TYPE));
        } else {
            //Make sure PATH is added for PUTs
            response = getInvocationBuilder(target.path(idPath)).put(Entity.entity(source, MediaType.APPLICATION_JSON_TYPE));
        }
        return response;
    }


    public Response deleteSchedule(Schedule schedule) {
        String queryParameters = "wt=json";
        Invocation.Builder invBuilder = getInvocationBuilder(SCHEDULER_BASE.replace("{1}",schedule.getId()),queryParameters);
        Response response = invBuilder.delete();
        return response;
    }

    public Schedule getSchedule(String jobName) {
        String queryParameters = "wt=json";
        Invocation.Builder invBuilder = getInvocationBuilder(SCHEDULER_BASE.replace("{1}",jobName),queryParameters);
        Schedule result =  invBuilder.get(Schedule.class);
        return result;

    }

    /**
     * The response object for a scheduler request has the actual schedule JSON as a subset of the
     * entire response. Pull it out in this method.
     *
     * @param response An api response from a schedule call.
     * @return an object representing the json schedule object.
     */
    public Schedule readScheduleFromResponse(Response response) {
        Schedule result = null;
        String json = response.readEntity(String.class);
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode scheduleNode = root.get("schedule");
            // POSTS return extra info where "schedule" is a subno0de. IF not, the use root*/
            if (scheduleNode == null) {
                scheduleNode = root;
            }
            result = mapper.readValue(scheduleNode.traverse(), Schedule.class);
        } catch (IOException e) {
            e.printStackTrace();
            //result stays null
        }
        return result;
    }

    /**
     * Get results from a Jersey WebResource (REST-based api call).
     *
     * @param retClass   The type of class expected as the result list
     * @param target     The actual call to make - the Invocation.Builder should have the URL and query parameters and any headers already set.
     *                   all set.
     * @param acceptType The type of media to accept - Typically JSON or XML. If a JSON or XML TYPE, then the api call
     *                   will be translated from JSON or XML to the java object specified as the retClass.
     */
    private <T> List<T> getResults(final Class<T> retClass, Invocation.Builder target, MediaType acceptType) {

        /*
         Create a new parameterized type inner class based on the class sent in.
         We are doing this because with a Jersey web resource call, 'regular' generics won't work because
         this is not resolved until runtime, and you get a
         "A message body reader for Java class java.util.List, and Java type java.util.List<T>,
            and MIME media type application/json was not found " error. This gets around that, using Jersey's
            GenericType class.
          */
        ParameterizedType genericType = new ParameterizedType() {
            public Type[] getActualTypeArguments() {
                return new Type[]{retClass};
            }

            public Type getRawType() {
                return List.class;
            }

            public Type getOwnerType() {
                return List.class;
            }
        };//inner class definition

        // Now use it  - we have an object called 'genericType' that is a newly created Parameterized type
        // created from the method's type parameter.
        GenericType<List<T>> type = new GenericType<List<T>>(genericType) {
        };
        List<T> retList = target.accept(acceptType).get(type);
        return retList;
    }

    /**
     * @param jsonString a string of json
     * @return A map?
     * @throws IOException
     */
    public Object parseJsonString(String jsonString) throws IOException {
        JsonFactory jfact = new JsonFactory();
        JSONPObject retVal = new JSONPObject("test", jfact);
        JsonParser parser = jfact.createParser(new StringReader(jsonString));
        while (!parser.isClosed()) {
            JsonToken token = parser.nextToken();
            if (token == null) {
                break;
            }


        }
        return retVal;
    }

    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    public void setCookies(List<Cookie> cookies) {
        this.cookies = cookies;
    }

    /**
     * Translate username:password to json string to use in session api.
     *
     * @param authstring
     * @return
     */
    public String credentialsToJson(String authstring) {
        String[] credentials = authstring.split(":");
        StringBuilder json = new StringBuilder("{ \"username\":");
        json.append("\"").append(credentials[0]).append("\",\"password\":\"").append(credentials[1]).append("\" }");
        return json.toString();
    }

    /**
     * @param jsonCredentialsPlaintext username:password
     * @return A session cookie
     */
    public Cookie session(String jsonCredentialsPlaintext) {
        return session(jsonCredentialsPlaintext, false);
    }

    /**
     * Call fusion's session api to get a session cookie.
     *
     * @param credentialString username:password wither in plaintext OR base64 encoded.
     * @param encoded          if true, then decode the string before sending to session api.
     * @return
     * @link https://doc.lucidworks.com/fusion/2.4/REST_API_Reference/Authentication_and_Authorization_APIs/Sessions-API.html
     */
    public Cookie session(String credentialString, boolean encoded) {
        String creds = null;
        if (encoded) {
            creds = credentialsToJson(Base64.decodeAsString(credentialString));
        } else {
            creds = credentialsToJson(credentialString);
        }
        Entity<?> ent = Entity.entity(creds, MediaType.APPLICATION_JSON_TYPE);
        Invocation.Builder invBuilder = getInvocationBuilder(SESSION_BASE, "realmName=" + getAuthenticationRealm());
        Response response = invBuilder.post(ent);
        Cookie retVal = response.getCookies().get("id");
        return retVal;
    }

    public String getAuthenticationRealm() {
        return authenticationRealm;
    }

    public void setAuthenticationRealm(String authenticationRealm) {
        this.authenticationRealm = authenticationRealm;
    }

    /**
     * Close client instance and all it's associated resources. Subsequent calls
     * have no effect and are ignored. Once the client is closed, invoking any
     * other method on the client instance would result in an {@link IllegalStateException}
     * being thrown.
     * <p/>
     * Calling this method effectively invalidates all {@link WebTarget resource targets}
     * produced by the client instance. Invoking any method on such targets once the client
     * is closed would result in an {@link IllegalStateException} being thrown.
     */
    @Override
    public void close() {
        client.close();
    }

    /**
     * Build a new web resource target.
     *
     * @param uri web resource URI. May contain template parameters. Must not be {@code null}.
     * @return web resource target bound to the provided URI.
     * @throws IllegalArgumentException in case the supplied string is not a valid URI template.
     * @throws NullPointerException     in case the supplied argument is {@code null}.
     */
    @Override
    public WebTarget target(String uri) {
        return client.target(uri);
    }

    /**
     * Build a new web resource target.
     *
     * @param uri web resource URI. Must not be {@code null}.
     * @return web resource target bound to the provided URI.
     * @throws NullPointerException in case the supplied argument is {@code null}.
     */
    @Override
    public WebTarget target(URI uri) {
        return client.target(uri);
    }

    /**
     * Build a new web resource target.
     *
     * @param uriBuilder web resource URI represented as URI builder. Must not be {@code null}.
     * @return web resource target bound to the provided URI.
     * @throws NullPointerException in case the supplied argument is {@code null}.
     */
    @Override
    public WebTarget target(UriBuilder uriBuilder) {
        return client.target(uriBuilder);
    }

    /**
     * Build a new web resource target.
     *
     * @param link link to a web resource. Must not be {@code null}.
     * @return web resource target bound to the linked web resource.
     * @throws NullPointerException in case the supplied argument is {@code null}.
     */
    @Override
    public WebTarget target(Link link) {
        return client.target(link);
    }

    /**
     * <p>Build an invocation builder from a link. It uses the URI and the type
     * of the link to initialize the invocation builder. The type is used as the
     * initial value for the HTTP Accept header, if present.</p>
     *
     * @param link link to build invocation from. Must not be {@code null}.
     * @return newly created invocation builder.
     * @throws NullPointerException in case link is {@code null}.
     */
    @Override
    public Invocation.Builder invocation(Link link) {
        return client.invocation(link);
    }

    /**
     * Get the SSL context configured to be used with the current client run-time.
     *
     * @return SSL context configured to be used with the current client run-time.
     */
    @Override
    public SSLContext getSslContext() {
        return client.getSslContext();
    }

    /**
     * Get the hostname verifier configured in the client or {@code null} in case
     * no hostname verifier has been configured.
     *
     * @return client hostname verifier or {@code null} if not set.
     */
    @Override
    public HostnameVerifier getHostnameVerifier() {
        return client.getHostnameVerifier();
    }

    /**
     * Get a live view of an internal configuration state of this configurable instance.
     * <p>
     * Any changes made using methods of this {@code Configurable} instance will be reflected
     * in the returned {@code Configuration} instance.
     * <p>
     * The returned {@code Configuration} instance and the collection data it provides are not
     * thread-safe wrt. modification made using methods on the parent configurable object.
     * </p>
     *
     * @return configuration live view of the internal configuration state.
     */
    @Override
    public Configuration getConfiguration() {
        return client.getConfiguration();
    }

    /**
     * Set the new configuration property, if already set, the existing value of
     * the property will be updated. Setting a {@code null} value into a property
     * effectively removes the property from the property bag.
     *
     * @param name  property name.
     * @param value (new) property value. {@code null} value removes the property
     *              with the given name.
     * @return the updated configurable instance.
     */
    @Override
    public Client property(String name, Object value) {
        return client.property(name, value);
    }

    /**
     * Register a class of a custom JAX-RS component (such as an extension provider or
     * a {@link Feature feature} meta-provider) to be instantiated
     * and used in the scope of this configurable context.
     * <p>
     * Implementations SHOULD warn about and ignore registrations that do not
     * conform to the requirements of supported JAX-RS component types in the
     * given configurable context. Any subsequent registration attempts for a component
     * type, for which a class or instance-based registration already exists in the system
     * MUST be rejected by the JAX-RS implementation and a warning SHOULD be raised to
     * inform the user about the rejected registration.
     * <p>
     * The registered JAX-RS component class is registered as a contract provider of
     * all the recognized JAX-RS or implementation-specific extension contracts including
     * meta-provider contracts, such as {@code Feature} or {@link DynamicFeature}.
     * <p>
     * As opposed to component instances registered via {@link #register(Object)} method,
     * the lifecycle of components registered using this class-based {@code register(...)}
     * method is fully managed by the JAX-RS implementation or any underlying IoC
     * container supported by the implementation.
     * </p>
     *
     * @param componentClass JAX-RS component class to be configured in the scope of this
     *                       configurable context.
     * @return the updated configurable context.
     */
    @Override
    public Client register(Class<?> componentClass) {
        return client.register(componentClass);
    }

    /**
     * Register a class of a custom JAX-RS component (such as an extension provider or
     * a {@link Feature feature} meta-provider) to be instantiated
     * and used in the scope of this configurable context.
     * <p>
     * This registration method provides the same functionality as {@link #register(Class)}
     * except that any priority specified on the registered JAX-RS component class via
     * {@code javax.annotation.Priority} annotation is overridden with the supplied
     * {@code priority} value.
     * </p>
     * <p>
     * Note that in case the priority is not applicable to a particular
     * provider contract implemented by the class of the registered component, the supplied
     * {@code priority} value will be ignored for that contract.
     * </p>
     *
     * @param componentClass JAX-RS component class to be configured in the scope of this
     *                       configurable context.
     * @param priority       the overriding priority for the registered component
     *                       and all the provider contracts the component implements.
     * @return the updated configurable context.
     */
    @Override
    public Client register(Class<?> componentClass, int priority) {
        return client.register(componentClass, priority);
    }

    /**
     * Register a class of a custom JAX-RS component (such as an extension provider or
     * a {@link Feature feature} meta-provider) to be instantiated
     * and used in the scope of this configurable context.
     * <p>
     * This registration method provides the same functionality as {@link #register(Class)}
     * except the JAX-RS component class is only registered as a provider of the listed
     * extension provider or meta-provider {@code contracts}.
     * All explicitly enumerated contract types must represent a class or an interface
     * implemented or extended by the registered component. Contracts that are not
     * {@link Class#isAssignableFrom(Class) assignable from} the registered component class
     * MUST be ignored and implementations SHOULD raise a warning to inform users about the
     * ignored contract(s).
     * </p>
     *
     * @param componentClass JAX-RS component class to be configured in the scope of this
     *                       configurable context.
     * @param contracts      the specific extension provider or meta-provider contracts
     *                       implemented by the component for which the component should
     *                       be registered.
     *                       Implementations MUST ignore attempts to register a component
     *                       class for an empty or {@code null} collection of contracts via
     *                       this method and SHOULD raise a warning about such event.
     * @return the updated configurable context.
     */
    @Override
    public Client register(Class<?> componentClass, Class<?>... contracts) {
        return client.register(componentClass, contracts);
    }

    /**
     * Register a class of a custom JAX-RS component (such as an extension provider or
     * a {@link Feature feature} meta-provider) to be instantiated
     * and used in the scope of this configurable context.
     * <p>
     * This registration method provides same functionality as {@link #register(Class, Class[])}
     * except that any priority specified on the registered JAX-RS component class via
     * {@code javax.annotation.Priority} annotation is overridden
     * for each extension provider contract type separately with an integer priority value
     * specified as a value in the supplied map of [contract type, priority] pairs.
     * </p>
     * <p>
     * Note that in case a priority is not applicable to a provider contract registered
     * for the JAX-RS component, the supplied priority value is ignored for such
     * contract.
     * </p>
     *
     * @param componentClass JAX-RS component class to be configured in the scope of this
     *                       configurable context.
     * @param contracts      map of the specific extension provider and meta-provider contracts
     *                       and their associated priorities for which the JAX-RS component
     *                       is registered.
     *                       All contracts in the map must represent a class or an interface
     *                       implemented or extended by the JAX-RS component. Contracts that are
     *                       not {@link Class#isAssignableFrom(Class) assignable from} the registered
     *                       component class MUST be ignored and implementations SHOULD raise a warning
     *                       to inform users about the ignored contract(s).
     * @return the updated configurable context.
     */
    @Override
    public Client register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        return client.register(componentClass, contracts);
    }

    /**
     * Register an instance of a custom JAX-RS component (such as an extension provider or
     * a {@link Feature feature} meta-provider) to be instantiated
     * and used in the scope of this configurable context.
     * <p>
     * Implementations SHOULD warn about and ignore registrations that do not
     * conform to the requirements of supported JAX-RS component types in the
     * given configurable context. Any subsequent registration attempts for a component
     * type, for which a class or instance-based registration already exists in the system
     * MUST be rejected by the JAX-RS implementation and a warning SHOULD be raised to
     * inform the user about the rejected registration.
     * <p>
     * The registered JAX-RS component is registered as a contract provider of
     * all the recognized JAX-RS or implementation-specific extension contracts including
     * meta-provider contracts, such as {@code Feature} or {@link DynamicFeature}.
     * <p>
     * As opposed to components registered via {@link #register(Class)} method,
     * the lifecycle of providers registered using this instance-based {@code register(...)}
     * is not managed by JAX-RS runtime. The same registered component instance is used during
     * the whole lifespan of the configurable context.
     * Fields and properties of all registered JAX-RS component instances are injected with their
     * declared dependencies (see {@link javax.ws.rs.core.Context}) by the JAX-RS runtime prior to use.
     * </p>
     *
     * @param component JAX-RS component instance to be configured in the scope of this
     *                  configurable context.
     * @return the updated configurable context.
     */
    @Override
    public Client register(Object component) {
        return client.register(component);
    }

    /**
     * Register an instance of a custom JAX-RS component (such as an extension provider or
     * a {@link Feature feature} meta-provider) to be instantiated
     * and used in the scope of this configurable context.
     * <p>
     * This registration method provides the same functionality as {@link #register(Object)}
     * except that any priority specified on the registered JAX-RS component class via
     * {@code javax.annotation.Priority} annotation is overridden with the supplied
     * {@code priority} value.
     * </p>
     * <p>
     * Note that in case the priority is not applicable to a particular
     * provider contract implemented by the class of the registered component, the supplied
     * {@code priority} value will be ignored for that contract.
     * </p>
     *
     * @param component JAX-RS component instance to be configured in the scope of this
     *                  configurable context.
     * @param priority  the overriding priority for the registered component
     *                  and all the provider contracts the component implements.
     * @return the updated configurable context.
     */
    @Override
    public Client register(Object component, int priority) {
        return client.register(component, priority);
    }

    /**
     * Register an instance of a custom JAX-RS component (such as an extension provider or
     * a {@link Feature feature} meta-provider) to be instantiated
     * and used in the scope of this configurable context.
     * <p>
     * This registration method provides the same functionality as {@link #register(Object)}
     * except the JAX-RS component class is only registered as a provider of the listed
     * extension provider or meta-provider {@code contracts}.
     * All explicitly enumerated contract types must represent a class or an interface
     * implemented or extended by the registered component. Contracts that are not
     * {@link Class#isAssignableFrom(Class) assignable from} the registered component class
     * MUST be ignored and implementations SHOULD raise a warning to inform users about the
     * ignored contract(s).
     * </p>
     *
     * @param component JAX-RS component instance to be configured in the scope of this
     *                  configurable context.
     * @param contracts the specific extension provider or meta-provider contracts
     *                  implemented by the component for which the component should
     *                  be registered.
     *                  Implementations MUST ignore attempts to register a component
     *                  class for an empty or {@code null} collection of contracts via
     *                  this method and SHOULD raise a warning about such event.
     * @return the updated configurable context.
     */
    @Override
    public Client register(Object component, Class<?>... contracts) {
        return client.register(component, contracts);
    }

    /**
     * Register an instance of a custom JAX-RS component (such as an extension provider or
     * a {@link Feature feature} meta-provider) to be instantiated
     * and used in the scope of this configurable context.
     * <p>
     * This registration method provides same functionality as {@link #register(Object, Class[])}
     * except that any priority specified on the registered JAX-RS component class via
     * {@code javax.annotation.Priority} annotation is overridden
     * for each extension provider contract type separately with an integer priority value
     * specified as a value in the supplied map of [contract type, priority] pairs.
     * </p>
     * <p>
     * Note that in case a priority is not applicable to a provider contract registered
     * for the JAX-RS component, the supplied priority value is ignored for such
     * contract.
     * </p>
     *
     * @param component JAX-RS component instance to be configured in the scope of this
     *                  configurable context.
     * @param contracts map of the specific extension provider and meta-provider contracts
     *                  and their associated priorities for which the JAX-RS component
     *                  is registered.
     *                  All contracts in the map must represent a class or an interface
     *                  implemented or extended by the JAX-RS component. Contracts that are
     *                  not {@link Class#isAssignableFrom(Class) assignable from} the registered
     *                  component class MUST be ignored and implementations SHOULD raise a warning
     *                  to inform users about the ignored contract(s).
     * @return the updated configurable context.
     */
    @Override
    public Client register(Object component, Map<Class<?>, Integer> contracts) {
        return client.register(component, contracts);
    }


}
