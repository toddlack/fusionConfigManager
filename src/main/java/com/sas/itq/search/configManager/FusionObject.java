package com.sas.itq.search.configManager;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sas.itq.search.configManager.connectors.Datasource;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represent an object from the objects api. This holds all the other objects, too.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FusionObject  {


    //property keys in the objects map
    public static final String DATASOURCES = "dataSources" ;
    public static final String COLLECTIONS ="collections";
    public static final String FEATURES="features";
    public static final String INDEX_PROFILES="indexProfiles";
    public static final String QUERY_PROFILES="queryProfiles";
    public static final String NONE="none";
    public static final String QUERY_PIPELINES = "queryPipelines";
    public static final String INDEX_PIPELINES = "indexPipelines";
    public static final String JOBS="jobs";
    public static final String SPARK_JOBS="sparkJobs";
    public static final String GROUPS="objectGroups";
    public static final String LINKS="links";
    public static final String PARSERS="parsers";
    public static final String OBJECTS="objects";

    Map<String,Object> objects ;
    Map<String,Object> metadata;
    Map<String,Object> variables;

    public  static Map<String,Class> objectNames = new HashMap<>();
    List<?> properties;
    Map<String,Object> raw = new HashMap<>();

    String filterPolicy=NONE;
    Boolean deep=true;


    List<Datasource> datasources = new ArrayList<>();
    List<FusionCollection> collections = new ArrayList<>();
    List<CollectionFeature> features = new ArrayList<>();
    List<IndexPipeline> indexPipelines = new ArrayList<>();
    List<QueryPipeline> queryPipelines = new ArrayList<>();
    List<SparkJob> sparkJobs = new ArrayList<>();
    List<Job> jobs = new ArrayList<>();
    List<FusionGroup> groups = new ArrayList<>();
    List<FusionLink> links = new ArrayList<>();
    List<QueryProfile> queryProfiles = new ArrayList<>();
    List<IndexProfile> indexProfiles = new ArrayList<>();

    static
    {
        objectNames = new HashMap<>();
        objectNames.put(COLLECTIONS,FusionCollection.class);
        objectNames.put(DATASOURCES,Datasource.class);
        objectNames.put(QUERY_PIPELINES,QueryPipeline.class);
        objectNames.put(INDEX_PIPELINES,IndexPipeline.class);
        objectNames.put(INDEX_PROFILES,IndexProfile.class);
        objectNames.put(QUERY_PROFILES,QueryProfile.class);
        objectNames.put(JOBS,Job.class);
        objectNames.put(SPARK_JOBS,SparkJob.class);
        objectNames.put(GROUPS,FusionGroup.class);
        objectNames.put(LINKS,FusionLink.class);
        objectNames.put(PARSERS,Parser.class);
        objectNames.put(OBJECTS,FusionObject.class);
        objectNames.put(FEATURES,CollectionFeature.class);

    }
    String externalFileName;
    public FusionObject() {

    }

    public static FusionObject loadFromJson(String json) throws IOException {
        ObjectMapper mp =new ObjectMapper();
        return mp.readValue(json,FusionObject.class);
    }

    @JsonAnyGetter
    public Map<String, Object> raw() {
        return raw;
    }

    @JsonAnySetter
    public Object set(String name, Object value) {
        return raw.put(name, value);
    }


    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public List<?> getProperties() {
        return properties;
    }

    public void setProperties(List<?> properties) {
        this.properties = properties;
    }

    public Map<String, Object> getObjects() {
        return objects;
    }

    public void setObjects(Map<String, Object> objects) {
        this.objects = objects;
    }

    public List<Datasource> getDatasources() {
        if (datasources.isEmpty()) {
            setDatasources(loadFromMapList(DATASOURCES,Datasource.class));
        }
        return datasources;
    }
    public List<FusionCollection> getCollections() {
        if (collections.isEmpty()) {
            setCollections(loadFromMapList(COLLECTIONS,FusionCollection.class));
        }
        return collections;
    }
    public List<CollectionFeature> getFeatures() {
        if (features.isEmpty()) {
            setCollections(loadFromMapList(FEATURES,CollectionFeature.class));
        }
        return features;
    }
    public List<QueryPipeline> getQueryPipelines() {
        if (queryPipelines.isEmpty()) {
            setQueryPipelines(loadFromMapList(QUERY_PIPELINES,QueryPipeline.class));
        }
        return queryPipelines;
    }
    public List<IndexPipeline> getIndexPipelines() {
        if (indexPipelines.isEmpty()) {
            setIndexPipelines(loadFromMapList(INDEX_PIPELINES,IndexPipeline.class));
        }
        return indexPipelines;
    }
    public List<SparkJob> getSparkJobs() {
        if (sparkJobs.isEmpty()) {
            setSparkJobs(loadFromMapList(SPARK_JOBS,SparkJob.class));
        }
        return sparkJobs;
    }

    public List<FusionGroup> getGroups() {
        if (groups.isEmpty()) {
            setGroups(loadFromMapList(GROUPS,FusionGroup.class));
        }
        return groups;
    }

    public List<FusionLink> getLinks() {
        if (links.isEmpty()) {
            setLinks(loadFromMapList(LINKS,FusionLink.class));
        }
        return links;
    }

    public List<Job> getJobs() {
        if (jobs.isEmpty()) {
            setJobs(loadFromMapList(JOBS,Job.class));
        }
        return jobs;
    }

    /**
     * Return list of query profiles
     * @return
     */
    public List<QueryProfile> getQueryProfiles() {
        if (queryProfiles.isEmpty()) {
            List<QueryProfile> pMap = getProfiles(QueryProfile.class);
            queryProfiles.addAll(getProfiles(QueryProfile.class));
        }
        return queryProfiles;
    }

    public List<IndexProfile> getIndexProfiles() {
        if (indexProfiles.isEmpty()) {
            List<IndexProfile> pMap = getProfiles(IndexProfile.class);
            indexProfiles.addAll(getProfiles(IndexProfile.class));
        }
        return indexProfiles;
    }

    /**
     * Return list of query profiles
     * @return
     */
    public <T extends FusionProfile> List<T> getProfiles(Class<T> type) {
        List<T> retVal = new ArrayList<>();
        String typeName=null;
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            T base = constructor.newInstance();
            typeName=base.getProfileType();
            //Profiles key in the object map are ArrayLists of Maps - each profile is a one entry map,
            //or name-value pair (profileName --> pipelineName)
            //For each one, create a new Profile instance from the map and add to return list.
            Map<String,String> tMap= (Map<String, String>) getObjects().get(typeName);
//            List<T> tList = (List<T>) getObjects().get(typeName);
            Constructor<T> constructorMap = type.getDeclaredConstructor(Map.class);
            for (Map.Entry collectionEntry : tMap.entrySet()) {
                String curCollection = collectionEntry.getKey().toString();
                Map<String,String> m1 = (Map<String, String>) collectionEntry.getValue();
                Map<String,String> profMap = (Map<String, String>) collectionEntry.getValue();
                for (Map.Entry profileEntry : profMap.entrySet()) {
                    Map<String,String> initVals=new HashMap<>();
                    initVals.put(FusionProfile.COLLECTION,curCollection);
                    initVals.put(FusionProfile.ID,profileEntry.getKey().toString());
                    initVals.put(FusionProfile.PIPELINE,profileEntry.getValue().toString());
                    retVal.add(constructorMap.newInstance(initVals));
                }
            }
            //for base(T t: Each(retVal.add(constructor.newInstance(item)));
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
       return retVal;
    }

    public void setQueryProfiles(List<QueryProfile> queryProfiles) {
        this.queryProfiles = queryProfiles;
    }

    public void setIndexProfiles(List<IndexProfile> indexProfiles) {
        this.indexProfiles = indexProfiles;
    }

    public void setJobs(List<Job> jobs) {
        this.jobs = jobs;
    }

    public void setGroups(List<FusionGroup> groups) {
        this.groups = groups;
    }

    public void setLinks(List<FusionLink> links) {
        this.links = links;
    }

    public void setDatasources(List<Datasource> datasources) {
        this.datasources = datasources;
    }

    public void setCollections(List<FusionCollection> collections) {
        this.collections = collections;
    }

    public void setFeatures(List<CollectionFeature> features) {
        this.features = features;
    }

    public void setIndexPipelines(List<IndexPipeline> indexPipelines) {
        this.indexPipelines = indexPipelines;
    }

    public void setQueryPipelines(List<QueryPipeline> queryPipelines) {
        this.queryPipelines = queryPipelines;
    }
    public void setSparkJobs(List<SparkJob> sparkJobs) {
        this.sparkJobs = sparkJobs;
    }

    /**
     * Generate a file name based on the contents, so there might be some meaning to the file name
     * @return the generated filename
     */
    public String getExternalFileName() {
        if (externalFileName == null) {
            StringBuilder sb = new StringBuilder();
            if(!getCollections().isEmpty()){
                sb.append("cl-");
                collections.stream().limit(3).forEach(t-> sb.append(t.getPathSegmentName()).append('-'));
            }
            if (!getQueryPipelines().isEmpty()) {
                sb.append("qp-");
                queryPipelines.stream().limit(3).forEach(t-> sb.append(t.getPathSegmentName()).append('-'));
            }
            if (!getIndexPipelines().isEmpty()) {
                sb.append("ip-");
                indexPipelines.stream().limit(3).forEach(t-> sb.append(t.getPathSegmentName()).append('-'));
            }
            if (!getJobs().isEmpty()) {
                sb.append("jb-");
                indexPipelines.stream().limit(3).forEach(t-> sb.append(t.getPathSegmentName()).append('-'));
            }
            if (!getSparkJobs().isEmpty()) {
                sb.append("spk-");
                indexPipelines.stream().limit(3).forEach(t-> sb.append(t.getPathSegmentName()).append('-'));
            }
            if (sb.length()>0) {
                externalFileName=sb.deleteCharAt(sb.lastIndexOf("-")).toString();
            } else {
                externalFileName = sb.toString();
            }
        }
        return externalFileName;
    }

    public void setExternalFileName(String externalFileName) {
        this.externalFileName = externalFileName;
    }

    public <T> List<T> loadFromMapList(String typeName, Class toClass1) {
        Object r = getObjects().get(typeName);
        List<Map<String,Object>> dataRaw = (List<Map<String, Object>>) getObjects().get(typeName);
        return convertMapToEntity(dataRaw,toClass1);
    }
    /**
     * Convert a List of maps into a list of the specified class. Json loaded from
     * api results
     * @param dataRaw
     * @param toClass
     * @param <T>
     * @return
     */
    private <T> List<T> convertMapToEntity(List<Map<String,Object>> dataRaw,Class<T> toClass) {
        ObjectMapper mp = new ObjectMapper();
        if (dataRaw == null) {
            return new ArrayList<>();
        }
        return dataRaw.stream()
                .map(m -> mp.convertValue(m, toClass))
                .collect(Collectors.toList());
    }

}
