package com.sas.itq.search.configManager;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sas.itq.search.FusionManagerRestClient;
import com.sas.itq.search.configManager.connectors.Datasource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Fusion configuration manager - read, create, update parts of a Fusion/Solr setup. Maintain
 * in a file structure, in JSON and XML files. USe the fusion API client to read and write to server.
 * <p>
 * Entities that are in JSON format:
 * <ul>
 * <li>Datasources</li>
 * <li>QueryPipelines</li>
 * <li>IndexPipelines</li>
 * <li>Schedules</li>
 * <li>Roles</li>
 * <li>Users</li>
 * <li>Collections</li>
 * <li>CollectionFeatures</li>
 * <li>Linked</li>
 * </ul>
 * A collection has its own folder with the collection json definiton, plus any solr files that go with the collection.
 *
 * @see FusionManagerRestClient
 */
public class ConfigManager {

    //These are the names of directories to hold the enity data, as well as the json names in the Object
    //class
    public static final String DIR_APPS = "apps";
    public static final String DIR_DATASOURCES = "datasources";
    public static final String DIR_INDEX_PIPELINES = "indexPipelines";
    public static final String DIR_QUERY_PIPELINES = "queryPipelines";
    public static final String DIR_SCHEDULES = "schedules";
    public static final String DIR_ROLES = "roles";
    public static final String DIR_USERS = "users";
    public static final String DIR_COLLECTIONS = "collections";
    public static final String DIR_JOBS = "jobs";
    public static final String DIR_PARSERS = "parsers";
    public static final String DIR_SERVER_INFO = "server-info";
    public static final String DIR_LINK = "links";
    public static final String DIR_GROUPS = "groups";

    private static final String OBJECTS = "objects";
    public static final String DIR_OBJECTS = OBJECTS;
    public static final String DIR_QUERY_PROFILES = "queryProfiles";
    public static final String DIR_INDEX_PROFILES = "indexProfiles";
    public static final String DIR_SPARK_JOBS = "spark" ;

    public static final String JSON = "json";
    public static final String JSON_REGEX_FILE_SUFFIX = "^.*\\.json";
    public static final String DIR_AGGREGATIONS = "aggregations";
    public static final EnumMap<EntityType, String> dirMap = new EnumMap<EntityType, String>(EntityType.class);
    private static final String FROM_REGEX_BRACES = "\\}\\, \\{";
    private static final String TO_REGEX_BRACES_NEWLINE = "\\}\\,\\\r\\\n\\\t{";
    public static final Logger log = LoggerFactory.getLogger(FusionManagerRestClient.class);

    FusionManagerRestClient restClient;

    /**
     * if field does not exist, return false.
     *
     * @param regex
     * @param field
     * @return
     */
    public static Predicate<JsonNode> nodePredicate(String regex, String field) {
        return node -> {
            boolean retVal = node.has(field);
            if (retVal) {
                retVal = node.get(field).textValue().matches(regex);
            }
            return retVal;
        };
    }

    public static <T> Predicate<T> nullPredicate() {
        return node -> {
            return true;
        };
    }

    /**
     * Default predicate filter for collections - if no filter specified, exclude Logs and signals
     *
     * @return
     */
    public static Predicate<JsonNode> defaultCollectionPredicate() {
        return (nodePredicate(FusionCollection.TYPE_SEARCH_LOGS, "type"))
                .or(nodePredicate(FusionCollection.TYPE_SIGNALS_AGGREGATION, "type"))
                .or(nodePredicate(FusionCollection.TYPE_SIGNALS, "type")).negate();
    }

    public ConfigManager() {
        dirMap.put(EntityType.DATASOURCE, DIR_DATASOURCES);
        dirMap.put(EntityType.COLLECTION, DIR_COLLECTIONS);
        dirMap.put(EntityType.USER, DIR_USERS);
        dirMap.put(EntityType.ROLE, DIR_ROLES);
        dirMap.put(EntityType.AGGREGATION, DIR_AGGREGATIONS);
        dirMap.put(EntityType.QUERY_PIPELINE, DIR_QUERY_PIPELINES);
        dirMap.put(EntityType.INDEX_PIPELINE, DIR_INDEX_PIPELINES);
        dirMap.put(EntityType.SCHEDULE, DIR_SCHEDULES);
        dirMap.put(EntityType.JOB, DIR_JOBS);
        dirMap.put(EntityType.PARSER, DIR_PARSERS);
        dirMap.put(EntityType.SYSINFO, DIR_SERVER_INFO);
        dirMap.put(EntityType.LINK, DIR_LINK);
        dirMap.put(EntityType.GROUP, DIR_GROUPS);
        dirMap.put(EntityType.OBJECT, DIR_OBJECTS);
        dirMap.put(EntityType.QUERY_PROFILE, DIR_QUERY_PROFILES);
        dirMap.put(EntityType.INDEX_PROFILE, DIR_INDEX_PROFILES);
        dirMap.put(EntityType.APP, DIR_APPS);
    }

    public FusionManagerRestClient getRestClient() {
        return restClient;
    }

    public void setRestClient(FusionManagerRestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Copy from a server.
     *
     * @param client          The client to use for the operation -it should already be setup.
     * @param outputDirectory Directory where the configuration files will be copied to.
     * @param entityTypes     Array of the types to copy. {@link EntityType}
     * @param envPath         should be a string like "dev","prodr", etc. For entity types like schedules, this says to create one extra directory based
     *                        on the environment. If empty or null, then no effect.
     * @return
     * @throws IOException
     */
    public Map<String, Boolean> copyFromServer(FusionManagerRestClient client, String outputDirectory, EntityType[] entityTypes, String envPath, Predicate<JsonNode> nodeFilter) throws IOException {
        Map<String, Boolean> writeStatuses = new HashMap<>();
        Path outputDir = Paths.get(outputDirectory);
        Files.createDirectories(outputDir);
        if (nodeFilter == null) {
            nodeFilter = nullPredicate();
        }
        Map<String, String> entityNameMapJson = new HashMap<>();
        for (int i = 0; i < entityTypes.length; i++) {
            EntityType entityType = entityTypes[i];
            Class entityClass = entityType.classType();
            String pathSection = dirMap.get(entityType);
            if (entityType != EntityType.COLLECTION && entityType != EntityType.COLLECTION_FEATURE) {
                entityNameMapJson = createFileEntityNameMapJson(client.getAsJson(entityClass, nodeFilter), entityClass, JSON);
            }
            switch (entityType) {
                case DATASOURCE:
                case INDEX_PIPELINE:
                case INDEX_PROFILE:
                case QUERY_PIPELINE:
                case QUERY_PROFILE:
                case PARSER:
                case GROUP:
                case USER:
                case AGGREGATION:
                case SYSINFO:
                case ROLE:
                    writeStatuses.putAll(writeEntitiesToFile(entityNameMapJson, Paths.get(outputDirectory, pathSection)));
                    break;
                case JOB:
                    //get schedules for each job, too
                    writeStatuses.putAll(writeEntitiesToFile(entityNameMapJson, Paths.get(outputDirectory, pathSection)));
                    ObjectMapper mp = new ObjectMapper();
                    JsonFactory jf = new JsonFactory();

                    List<String> jobs = entityNameMapJson.values().stream()
                            .map(line -> {
                                Job j = readJsonToClass(line,Job.class);
                                return j.getResource();
                            }).collect(Collectors.toList());
                    List<Schedule> schedules = client.getSchedules(jobs);
                    Map<String,Schedule> schedMap = createFileEntityNameMap( schedules, JSON);
                    writeStatuses.putAll(writeEntitiesToFile(schedMap,Paths.get(outputDirectory,dirMap.get(EntityType.SCHEDULE) )) );
                    break;
                case SCHEDULE:
                    //we have Schedules in an extra environment folder, specified here.
                    Path schedPath = Paths.get(outputDirectory, pathSection, envPath);
                    Files.createDirectories(schedPath);
                    writeStatuses.putAll(writeEntitiesToFile(entityNameMapJson, schedPath));
                    break;
                case COLLECTION:
                    writeStatuses.putAll(copyCollectionsFromServer(client, outputDirectory, nodeFilter));
                    break;
                case APP:
                    //Get the app
                    writeStatuses.putAll(writeEntitiesToFile(entityNameMapJson, Paths.get(outputDirectory, pathSection)));
                    break;
            }
        }
        return writeStatuses;
    }

    /**
     * Copy from a server.
     *
     * @param client          The client to use for the operation -it should already be setup.
     * @param outputDirectory Directory where the configuration files will be copied to.
     * @param entityTypes     Array of the types to copy. {@link EntityType}
     * @param envPath         should be a string like "dev","prodr", etc. For entity types like schedules, this says to create one extra directory based
     *                        on the environment. If empty or null, then no effect.
     * @return
     * @throws IOException
     */
    public Map<String, Boolean> copyFromServer(FusionManagerRestClient client, String outputDirectory, EntityType[] entityTypes, String envPath) throws IOException {
        return copyFromServer(client, outputDirectory, entityTypes, envPath, nullPredicate());
    }

    public Map<String, Boolean> copyFromServer(FusionManagerRestClient client, String outputDirectory, EntityType[] entityTypes) throws IOException {
        return copyFromServer(client, outputDirectory, entityTypes, "");
    }

    /**
     * Copy from server using the Objects API. This will get related groups so you can
     * get all related objects for a collection.
     *
     * @param client
     * @param outputDirectory
     * @param fileName name of external file to write JSON to, relative to output directory above. If empty, a filename is generated.
     * @param separateFles    If true then write out object tree as separate files for each entity. False - write as one single file for Object
     * @param queryParams     array of strings that are query parameters to send into the /objects/export api
     * @return
     * @throws IOException
     * @see https://doc.lucidworks.com/fusion/3.1/REST_API_Reference/Objects-API.html
     */
    public Map<String, Boolean> copyFromServer(FusionManagerRestClient client, Path outputDirectory,
                                               String fileName, Boolean separateFles, String... queryParams) throws IOException {
        Map<String, Boolean> writeStatuses = new HashMap<>();
        Files.createDirectories(outputDirectory);
        String json = client.getFusionObjectsAsJson(queryParams);
        JsonNode root = client.buildJsonRootNode(json);
        FusionObject obj = FusionObject.loadFromJson(json);
        obj.setExternalFileName(fileName);
        Map<String, String> nameMap = new LinkedHashMap<>();

        if (!separateFles) {
            nameMap.put(obj.getExternalFileName() + ".json", json);
        } else {
            //Get each entity list and add a filename ==> contents entry
                ObjectMapper mp = new ObjectMapper();
                obj.getCollections().stream().forEach(writeObjectAsJson(nameMap, mp,DIR_COLLECTIONS));
                obj.getDatasources().stream().forEach(writeObjectAsJson(nameMap, mp,DIR_DATASOURCES));
                obj.getQueryPipelines().stream().forEach(writeObjectAsJson(nameMap, mp,DIR_QUERY_PIPELINES));
                obj.getIndexPipelines().stream().forEach(writeObjectAsJson(nameMap, mp,DIR_INDEX_PIPELINES));
                obj.getGroups().stream().forEach(writeObjectAsJson(nameMap, mp,DIR_GROUPS));
                obj.getLinks().stream().forEach(writeObjectAsJson(nameMap, mp,DIR_LINK));
                obj.getJobs().stream().forEach(writeObjectAsJson(nameMap, mp,DIR_JOBS));
                obj.getSparkJobs().stream().forEach(writeObjectAsJson(nameMap, mp,DIR_SPARK_JOBS));
                JsonNode node1 = root.path("objects").path(DIR_QUERY_PROFILES);
                List<QueryProfile> qlist=obj.getQueryProfiles();
                List<IndexProfile> ilist=obj.getIndexProfiles();
                qlist.stream().forEach(writeObjectAsJson(nameMap,mp,DIR_QUERY_PROFILES));
                ilist.stream().forEach(writeObjectAsJson(nameMap,mp,DIR_INDEX_PROFILES));

            }
        writeStatuses.putAll(writeEntitiesToFile(nameMap, outputDirectory));
        return writeStatuses;
    }

    /**
     *  Add a generated filename and the object contents as json to a map.
     * @param nameMap
     * @param mp
     * @param parentDir IF specified, prepend directory to filename.
     * @return
     */
    public Consumer<IdentifiableString> writeObjectAsJson(Map<String, String> nameMap, ObjectMapper mp,String parentDir) {
        return t -> {
            try {
                StringBuilder filepath=new StringBuilder(parentDir).append("/").append(t.generateFileName());
                String contents= mp.writerWithDefaultPrettyPrinter().writeValueAsString(t);
                nameMap.put(filepath.toString(), contents);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        };
    }

    /**
     * Read all configurations from server in fusion client, and copy to
     * directory
     *
     * @param client
     */
    public Map<String, Boolean> copyAllFromServer(FusionManagerRestClient client, String outputDirectory) throws IOException {
        EntityType[] allTypes = EntityType.values();
        return copyFromServer(client, outputDirectory, allTypes);
    }


    /**
     * Copying a collections is more than just one type of file. Handle it here.
     *
     * @param client
     * @param outputDirectory
     * @return
     */
    public Map<String, Boolean> copyCollectionsFromServer(FusionManagerRestClient client, String outputDirectory, Predicate<JsonNode> filter) {
        //Process collections first- they are more complicated than the other entities.
        Predicate<JsonNode> noSystem = filter.and(defaultCollectionPredicate());
        List<JsonNode> jsonNodeList = client.getAsJsonNodes(EntityType.COLLECTION.classType(), noSystem);
        return copyCollectionsFromServer(client, jsonNodeList, outputDirectory);
    }

    /**
     * Copy a specified list of collections to json files in the specifed directory.
     *
     * @param client          Should be set up to call server.
     * @param jsonNodeList    - collection definitions as JsonNodes - copy only these collections.
     * @param outputDirectory - target directory for json files.
     * @return Map of collections and the write status of each file.
     */
    public Map<String, Boolean> copyCollectionsFromServer(FusionManagerRestClient client, List<JsonNode> jsonNodeList, String outputDirectory) {
        List<FusionCollection> fusionCollections = jsonNodeList.stream()
                .map(node -> FusionCollection.create(node))
                .collect(Collectors.toList());

        List<FusionCollection> baseCollections = new ArrayList<>();
        //Get all file data for all collections and create a shallow copy
        fusionCollections.forEach(coll -> {
            coll.setFeatures(client.getCollectionFeatures(coll.getId()));
            baseCollections.add(coll.copy(coll, true));
            //Now get all solr config data files.
            coll.setSolrConfigData(client.getSolrConfigData(coll.getId(), Boolean.TRUE));
        });

        Map<String, FusionCollection> collectionMap = createFileEntityNameMap(fusionCollections, JSON);
        Map<String, FusionCollection> collectionSmallMap = createFileEntityNameMap(baseCollections, JSON);

        //Collect all the statuses of writing the objects to the file system
        Map<String, Boolean> writeStatuses = null;
        try {
            writeStatuses = writeEntitiesToFile(collectionSmallMap, Paths.get(outputDirectory, DIR_COLLECTIONS));
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Problem writing collections to storage: {}", e.getMessage());
        }
        //Write all config files for each collection
        Properties stringProps = new Properties();
        stringProps.setProperty("writeOnly", "true");
        for (FusionCollection fusColl : collectionMap.values()) {
            Map<String, SolrConfigData> temp = createFileEntityNameMap(fusColl.getSolrConfigData(), null);
            Map<String, Boolean> solrWriteStatuses = writeSolrFiles(outputDirectory, stringProps, fusColl.getId(), temp);
            writeStatuses.putAll(solrWriteStatuses);
        }
        return writeStatuses;

    }

    private Map<String, Boolean> writeSolrFiles(String outputDirectory, Properties stringProps, String collectionName, Map<String, SolrConfigData> temp) {
        Map<String, Boolean> solrWriteStatuses = new HashMap<>();
        try {
            //For each entry value(SolrConfigData), get the value's name and value
            Map<String, String> fileContentMap = getSolrConfigContentMap(temp);
            Map<String, Boolean> results = writeEntitiesToFile(fileContentMap, Paths.get(outputDirectory, DIR_COLLECTIONS + "/" + collectionName), stringProps);
            solrWriteStatuses.putAll(results);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return solrWriteStatuses;
    }

    private Map<String, String> getSolrConfigContentMap(Map<String, SolrConfigData> temp) {
        Map<String, String> fileContentMap = new LinkedHashMap<>();
        temp.forEach((key, value) -> {
            //managed-schema is a different thing - the file should have .xml added only when writing it out.
            if (key.equals(SolrConfigData.MANAGED_SCHEMA)) {
                key = key + ".xml";
            }
            fileContentMap.put(key, value.getValue());
        });
        return fileContentMap;
    }


    /**
     * Collections are stored as directories, with all of the SOLR files in that directory. Also the colleciton definition.
     * To copy to a server we need to use the collections API and the solr api to get all the schema and other files updated.
     *
     * @param client
     * @param sourceDir This is the master collections directory
     * @param filter    This will match on the collection name. Specifically the directory name in the collections directory.
     *                  And there should be a json file with the same name.
     * @return List of responses from the update call.
     */
    public List<Response> copyCollectionsToServer(FusionManagerRestClient client, String sourceDir, Predicate<File> filter) {
        List<Response> responseList = new LinkedList<>();
        listPaths(sourceDir + "/" + DIR_COLLECTIONS, filter)
                .forEach(collFile -> responseList.addAll(copySingleCollectionToServer(client, collFile)));
        return responseList;
    }

    /**
     * Steps:
     * <ol>
     * <li>get collection files in directory.</li>
     * <li>get the collection json file (<collection_name>.json)</li>
     * <li>Update the collection with this file's contents</li>
     * <li>update the solrconfig.xml - this will have any definitions used by managed-schema</li>
     * <li>managed-schema.xml</li>
     * <li>all others</li>
     * </ol>
     *
     * @param client              The REST client
     * @param collectionDirectory direcroy on the file system that hods the configuration files.
     * @return All the http responses to the updates.
     */
    public List<Response> copySingleCollectionToServer(FusionManagerRestClient client, File collectionDirectory) {

        List<Response> responseList = new LinkedList<>();
        String collectionName = collectionDirectory.getName();
        String collectionFile = collectionName + ".json";
        SolrConfigData solrConfig = new SolrConfigData();
        Map<String, String> fileContentMap = solrConfig.loadDirectoryContents(collectionDirectory,Boolean.FALSE);

        //Create a clean collection for posting, along with a features list. Also remove the collection.json from the map
        FusionCollection fusionCollection = readJsonToClass(fileContentMap.remove(collectionFile), FusionCollection.class);
        List<CollectionFeature> features = fusionCollection.getFeatures();
        Response response = client.updateOrCreate(fusionCollection.clean(), "", FusionCollection.class);
        responseList.add(response);
        //Now update the features -- dynamicSchema was giving problems. Come back to that later.
        if (features!=null) {
            features.stream()
                    .filter(f -> !f.getName().equalsIgnoreCase("dynamicSchema"))
                    .forEach(f -> responseList.add(client.updateOrCreate(f, "")));
        }

        //Update the solrconfig, and managed schema first. Then all others.
        String keys[] = {"solrconfig.xml", SolrConfigData.MANAGED_SCHEMA};
        for (int i = 0; i < keys.length; i++) {
            String curKey = keys[i];
            //remove these from the map once we use them.
            String curVal = fileContentMap.remove(curKey);
            //this is a different thing - the actual managed-schema data is in managed-schema.xml
            if (curVal == null && curKey.equals(SolrConfigData.MANAGED_SCHEMA)) {
                curVal = fileContentMap.remove(curKey + ".xml");
            }
            postSpecificConfig(client, responseList, collectionName, curKey, curVal);
        }

        //Now go through the config files in the directory
        fileContentMap.entrySet().forEach(entry -> {
            Response rsp = postSolrConfigData(client, collectionName, entry.getKey(), entry.getValue());
            responseList.add(rsp);
        });

        return responseList;
    }

    private void postSpecificConfig(FusionManagerRestClient client, List<Response> responseList, String collectionName, String key, String value) {
        if (value != null) {
            Response rsp = postSolrConfigData(client, collectionName, key, value);
            responseList.add(rsp);
        }
    }

    private Response postSolrConfigData(FusionManagerRestClient client, String collectionName, String key, String value) {
        SolrConfigData configData = new SolrConfigData(key, value);
        configData.setCollection(collectionName);
        configData.setParent(collectionName);
        return client.updateOrCreate(configData, "");
    }

    /**
     * Read all configurations from server in fusion client, and copy to
     * directory
     *
     * @param client
     */
    public List<Response> copyAllToServer(FusionManagerRestClient client, String baseDirectory) {
        List<Response> responses = new LinkedList<>();
        responses.addAll(copyToServer(client, baseDirectory + "/" + DIR_SCHEDULES, Schedule.class));
        responses.addAll(copyToServer(client, baseDirectory + "/" + DIR_DATASOURCES, Datasource.class));
        responses.addAll(copyToServer(client, baseDirectory + "/" + DIR_INDEX_PIPELINES, IndexPipeline.class));
        responses.addAll(copyToServer(client, baseDirectory + "/" + DIR_QUERY_PIPELINES, QueryPipeline.class));
        responses.addAll(copyToServer(client, baseDirectory + "/" + DIR_ROLES, Role.class));
        responses.addAll(copyToServer(client, baseDirectory + "/" + DIR_USERS, User.class));
        responses.addAll(copyToServer(client, baseDirectory + "/" + DIR_AGGREGATIONS, Aggregator.class));
        return responses;
    }

    /**
     * Read all configurations from server in fusion client, and copy to
     * directory
     *
     * @param client
     */
    public List<Response> copyToServer(FusionManagerRestClient client, String baseDirectory, EntityType[] entityTypes) throws IOException {
        Predicate<File> p = nullPredicate();
        return copyToServer(client, baseDirectory, entityTypes, p);
    }

    public <T extends IdentifiableString> List<Response> copyToServer(FusionManagerRestClient client, String baseDirectory, EntityType[] entityTypes, Predicate<File> filter) {
        List<Response> responses = new LinkedList<>();
        for (int i = 0; i < entityTypes.length; i++) {
            EntityType entityType = entityTypes[i];
            Class entityClass = entityType.classType();
            String pathSection = dirMap.get(entityType);
            if (entityType.equals(EntityType.COLLECTION)) {
                responses.addAll(copyCollectionsToServer(client, baseDirectory, filter));
            } else {
                responses.addAll(copyToServer(client, baseDirectory + "/" + pathSection, filter, entityClass));
            }
        }
        return responses;
    }

    /**
     * Update or create fusion server entities (datasource, query-pipeline, etc.) from the file system that match the
     * passed in entity class, and with the corresponding files.
     *
     * @param client        REST client to talk to server.
     * @param baseDirectory directory on the file system that contains the json or xml files that define the entities
     * @param nameFilter    A predicate to match File attributes. Typically name, and can also include modified dates.
     * @param entityClass   The entity type to upload.
     * @param <T>           Must implement the IdentifiableString interface.
     * @return List of http responses to show status of updates.
     */
    public <T extends IdentifiableString> List<Response> copyToServer(FusionManagerRestClient client, String baseDirectory, Predicate<File> nameFilter, Class<T> entityClass) {
        Predicate<File> filter = nameFilter.and(file -> file.getName().matches(JSON_REGEX_FILE_SUFFIX));
        List<T> sources = readSourceFiles(baseDirectory, filter, entityClass);
        List<Response> responses = update(client, sources, entityClass);
        return responses;
    }

    public <T extends IdentifiableString> List<Response> copyToServer(FusionManagerRestClient client, String baseDirectory, Class<T> entityClass) {
        return copyToServer(client,baseDirectory,nullPredicate(),entityClass);
    }

    /**
     * Pull out the name string from each object and use it to construct a filename for saving to file system
     * as the key.
     *
     * @param sources   list of objects with a getName method that returns a string.
     * @param extension filename extension, such as "xml" or "json"
     * @return
     */
    private <T extends IdentifiableString> Map<String, T> createFileEntityNameMap(List<T> sources, String extension) {
        Map<String, T> entityMap = new LinkedHashMap<>();
        sources.forEach(source -> {
            StringBuilder fileName = new StringBuilder(source.generateFileName());
            if (extension != null && !extension.isEmpty() && fileName.lastIndexOf(extension)<=0) {
                fileName.append(".").append(extension);
            }
            //SolrConfigData can have child instances. Walk the trees.
            if (source instanceof SolrConfigData) {
                List<SolrConfigData> children =((SolrConfigData) source).getChildren();
                if (children!=null) {
                    Map<String, SolrConfigData> childEntityMap = createFileEntityNameMap(children,"");
                    entityMap.putAll((Map<? extends String, ? extends T>) childEntityMap);
                }
            }
            entityMap.put(fileName.toString(), source);
        });
        return entityMap;
    }

    private <T extends IdentifiableString> Map<String, String> createFileEntityNameMapJson(List<String> sources, Class<T> entityType, String extension) {
        Map<String, String> entityMap = new HashMap<>();
        sources.forEach(source -> {
            //we only need to convert to the entity class to generate the filename.
            T entity = readJsonToClass(source, entityType);
            String fName=entity.generateFileName();
            StringBuilder fileName = new StringBuilder(fName);
            if (extension != null && !extension.isEmpty() && !fName.endsWith(extension)) {
                fileName.append(".").append(extension);
            }
            entityMap.put(fileName.toString(), source);
        });
        return entityMap;
    }

    /**
     * Read in datasource json files from a directory
     */
    public List<Datasource> readDataSourceFiles(String directoryName) {
        return readSourceFiles(directoryName, JSON_REGEX_FILE_SUFFIX, Datasource.class);
    }

    public <T extends IdentifiableString> List<T> readSourceFiles(String directoryName, String fileNamePattern, Class<T> entityType) {
        List<File> fList = listPaths(directoryName, fileNamePattern)
                .collect(Collectors.toList());
        List<T> retList = readConfigSourceFiles(fList, entityType);
        return retList;
    }

    public <T extends IdentifiableString> List<T> readSourceFiles(String directoryName, Predicate<File> fileFilter, Class<T> entityType) {
        List<File> fList = listPaths(directoryName, fileFilter)
                .collect(Collectors.toList());
        List<T> retList = readConfigSourceFiles(fList, entityType);
        return retList;
    }


    public void writeDataSourcesToDirectory(List<Datasource> datasources, Path directory) {
        File dir = directory.toFile();
        datasources.forEach(source ->
                writeEntityToFile(source, new File(dir, source.id + ".json"), Datasource.class));
    }

    /**
     * Read in datasource json files from a directory
     */
    public List<Datasource> readDataSourceFiles(List<File> fileList) {
        return readConfigSourceFiles(fileList, Datasource.class);
    }

    public void writeQueryPipelinesToDirectory(List<QueryPipeline> sources, Path directory) {
        File dir = directory.toFile();
        //Add a return and tab between  '}, {'
        sources.forEach(source -> writeEntityToFile(source,
                new File(dir, source.id + ".json"), getDefaultWriteProperties()));
    }

    /**
     * Read in source json files from a directory
     */
    public <T> List<T> readConfigSourceFiles(List<File> fileList, Class<T> toClass) {
        List<T> results = fileList.stream()
                //take each file, read its contents into a Datasource, put Datasource in the stream
                .map(jsonFile -> readJsonToClass(jsonFile, toClass))
                .collect(Collectors.toList());
        return results;
    }


    /**
     * Update or create roles.
     *
     * @param client
     * @param sources
     * @return
     */
    public List<Response> updateRoles(FusionManagerRestClient client, List<Role> sources) {
        List<Response> retList = sources.stream()
                .map(client::updateOrCreate)
                .collect(Collectors.toList());
        return retList;
    }

    /**
     * Use Fusion's Object API to import data from the file system to the server. USes form data, so it is different
     * from the othe entity API's.
     * @param client a primed fusion client
     * @param importFile name of external file that has Object json
     * @param passwordFile name of external file that has the password substitution
     * @param policy
     * @return
     */
    public List<Response> updateObject(FusionManagerRestClient client,String importFile, String passwordFile, String policy) {
        try {
            String importData = readFromFile(importFile);
            String passwordData=readFromFile(passwordFile);
            FusionObject obj = new FusionObject();


        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Response> retList = new ArrayList<>();
        StringBuilder qp =  new StringBuilder("importData");

        return retList;
    }

    private String readFromFile(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        StringBuilder sb = new StringBuilder();
        lines.stream().forEach(line -> {sb.append(line).append("\n");});
        return sb.toString();
    }

    public <T extends IdentifiableString> List<Response> update(FusionManagerRestClient client, List<T> sources, Class<T> entityType) {
        if (EntityType.COLLECTION.classType().equals(entityType)) {
            System.out.println("Collection processing");
        }
        List<Response> retList = sources.stream()
                .map(currentSource -> client.updateOrCreate(currentSource, "", entityType))
                .collect(Collectors.toList());
        return retList;
    }

    /**
     * Update or create datasources.
     *
     * @param client
     * @param sources
     * @return
     */
    public List<Response> updateDataSources(FusionManagerRestClient client, List<Datasource> sources) {
        List<Response> retList = update(client, sources, Datasource.class);
        return retList;
    }

    public List<Response> updateQueryPipelines(FusionManagerRestClient client, List<QueryPipeline> sources) {
        List<Response> retList = update(client, sources, QueryPipeline.class);
        return retList;
    }

    public List<Response> updateIndexPipelines(FusionManagerRestClient client, List<IndexPipeline> sources) {

        List<Response> retList = sources.stream()
                .map(client::updateOrCreate)
                .collect(Collectors.toList());
        return retList;
    }

    public <T> T readJsonToClass(String json, Class<T> toClass) {
        return readJsonToClass(json.getBytes(), toClass);
    }

    /**
     * Read a json file as the given class
     *
     * @param jsonFile File object that points to json file
     * @param toClass  Read to this class
     * @param <T>      the class to specify - same as "toClass" above
     * @return The result of reading in the given JSON file
     */
    public <T> T readJsonToClass(File jsonFile, Class<T> toClass) {
        T retVal = null;
        try {
            byte[] jsonData = Files.readAllBytes(Paths.get(jsonFile.getAbsolutePath()));
            retVal = readJsonToClass(jsonData, toClass);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return retVal;
    }

    public <T> T readJsonToClass(byte[] jsonData, Class<T> toClass) {
        T retVal = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            retVal = objectMapper.readValue(jsonData, toClass);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return retVal;
    }


    public String readFileContents(File configFile) {
        return SolrConfigData.readFileContents(configFile, log);
    }

    public <T> Map<String, Boolean> writeEntitiesToFile(Map<String, T> entityMap, Path directory) throws IOException {
        Properties p = getDefaultWriteProperties();
        return writeEntitiesToFile(entityMap, directory, p);
    }

    public Properties getDefaultWriteProperties() {
        Properties p = new Properties();
        p.setProperty("fromRegex", FROM_REGEX_BRACES);
        p.setProperty("toRegex", TO_REGEX_BRACES_NEWLINE);
        return p;
    }

    /**
     * Write a list of entities as the type parameter you specify. This is meant to allow you to write JSON
     * or XML directly from a rest api call, OR to write the deserialized version. Datasource vs String, for example.
     *
     * @param entityMap String,entity pairs. The key is the filename, and the value is the String or other datatype to write out.
     * @param directory existing directory to write the files.
     * @return map of files and and TRUE if succeeded, FALSE if failed.
     */
    public <T> Map<String, Boolean> writeEntitiesToFile(Map<String, T> entityMap, Path directory, Properties props) throws IOException {
        Map<String, Boolean> returnMap = new HashMap<>();
        if (!Files.exists(directory)) {
            Files.createDirectory(directory);
        }
        for (Map.Entry<String, ?> entry : entityMap.entrySet()) {
            Path newFile = directory.resolve(entry.getKey());
            //Create any directories that are in the path that do not already exist
            Files.createDirectories(newFile.getParent());
            Boolean status = writeEntityToFile(entry.getValue(), newFile.toFile(), props);
            returnMap.put(newFile.toString(), status);
        }
        return returnMap;
    }

    /**
     * Write an entity to a file
     *
     * @param entity
     * @param outputFile
     * @param props
     * @param <T>
     * @return
     */
    public <T> Boolean writeEntityToFile(T entity, File outputFile, Properties props) {
        DefaultPrettyPrinter pp = new PrettyPrinterFusion();
        DefaultIndenter di = new DefaultIndenter();
        di.withIndent("    ");
        pp.indentObjectsWith(di);
        ObjectWriter writer = new ObjectMapper().writer(pp);
        Boolean retValue = Boolean.FALSE;
        //If entity is null - it is probably a recursive thing - like lang in a collection folder has children. Skip for now
        if (entity == null) {
            log.info("SKipping writing a null entity to a file: {}", outputFile.getAbsolutePath());
            return Boolean.TRUE;
        }
        try {
            String writeOnly = props.getProperty("writeOnly");
            String json = null;
            if (writeOnly != null && writeOnly.equals("true")) {
                json = entity.toString();
            } else {
                if (entity instanceof String) {
                    json = (String) entity;
                } else {
                    json = writer.writeValueAsString(entity);
                }
                String fromRegex = props.getProperty("fromRegex");
                String toRegex = props.getProperty("toRegex");
                if (fromRegex != null && toRegex != null) {
                    //Transform to do, like for QueryPipelines- prettyprint has fewer carriage returns than we want
                    json = json.replaceAll(fromRegex, toRegex);
                }
            }
            List<String> lines = Arrays.asList(json.split("\\r?\\n"));

            Files.write(outputFile.toPath(), lines);
            retValue = Boolean.TRUE;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return retValue;
    }

    public <T> Boolean writeEntityToFile(T entity, File outputFile, Class<T> entityClass) {
        return writeEntityToFile(entity, outputFile, getDefaultWriteProperties());
    }

    /**
     * Return a stream of filenames in a directory that match a REGEX
     *
     * @param directory       The absolute path of the directory
     * @param fileNamePattern Pattern to match on names returning
     * @return
     */
    public Stream<File> listPaths(String directory, String fileNamePattern) {
        Predicate<File> predicate = file -> {
            return file.getName().matches(fileNamePattern);
        };
        return listPaths(directory, predicate);
    }

    public Stream<File> listPaths(String directory, Predicate<File> filePredicate) {
        File logDir = new File(directory);
        if (!logDir.exists()) {
            throw new RuntimeException("Directory " + directory + " does not exist");
        }
        return Arrays.stream(logDir.listFiles())
                .filter(filePredicate);
    }

}
