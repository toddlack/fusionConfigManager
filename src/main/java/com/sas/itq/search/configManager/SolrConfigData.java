package com.sas.itq.search.configManager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Metadata for Solr config file in a collection
 * /api/apollo/collections/{collectionName}/solr-config?recursive=true
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SolrConfigData implements IdentifiableString {

    public static final String MANAGED_SCHEMA = "managed-schema";
    private String name;
    @JsonProperty(required = false)
    private String collection;
    private Long version;
    private Boolean isDir;
    private String value;
    private String parent;
    private List<SolrConfigData> children;
    private static final Logger log = LoggerFactory.getLogger(SolrConfigData.class);

    public SolrConfigData() {

    }

    public SolrConfigData(String fName, String fContents) {
        this();
        this.value = fContents;
        this.name = fName;
    }

    public SolrConfigData(File file) {
        this();
        loadFile(file);
    }
    @Override
    public String generateFileName() {
        StringBuilder sb = new StringBuilder();
        if (this.getParent() != null) {
            sb.append(this.getParent()).append("/");
        }
        sb.append(this.getName());
        if (this.name.equals(this.collection)) {
            //This should be a json file - add the extension
            sb.append(JSON);
        }
        return sb.toString();
    }
    @Override
    public String generateFileName(String parentDir) {
        StringBuilder sb= new StringBuilder(parentDir).append(generateFileName());
        return sb.toString();
    }

    public void loadFile(File file) {
        this.isDir = file.isDirectory();
        this.setName(file.getName());
        if (!isDir) {
           setValue(readFileContents(file,log));
        }
        setParent(file.getParent());
        //todo: load children
    }


    public Map<String,String> loadDirectoryContents(File startDir,Boolean addParent) {
        Map<String,String> fileContentMap = new LinkedHashMap<>();
        File[] farr = startDir.listFiles();
        Arrays.stream(farr)
                .forEach(f -> {
                    if (f.isDirectory()) {
                        fileContentMap.putAll(loadDirectoryContents(f,Boolean.TRUE));
                    } else {
                        StringBuilder sb = new StringBuilder();
                        if (addParent) {
                            sb.append(startDir.getName()).append("/");
                        }
                        sb.append(f.getName());
                        fileContentMap.put(sb.toString(), readFileContents(f,log));
                    }
                });
        return fileContentMap;
    }


    static String readFileContents(File inFile, Logger logger) {
        String retVal;
        String filePath = inFile.getAbsolutePath();
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            StringBuilder sb = new StringBuilder();
            lines.stream().forEach(line -> {sb.append(line).append("\n");});

            retVal = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Could not read file {}. Error:{}",filePath,e.getMessage());
            retVal="";
        }
        return retVal;
    }

    /**
     * Return a string that is the PATH segment for a get or PUT. Sometimes it is ID , sometimes name
     */
    @Override
    public String getPathSegmentName() {
        return getName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonProperty(value = "isDir")
    public Boolean getDir() {
        return isDir;
    }

    public void setDir(Boolean dir) {
        isDir = dir;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public List<SolrConfigData> getChildren() {
        return children;
    }

    public void setChildren(List<SolrConfigData> children) {
        this.children = children;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SolrConfigData)) return false;

        SolrConfigData that = (SolrConfigData) o;

        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;
        if (getCollection() != null ? !getCollection().equals(that.getCollection()) : that.getCollection() != null)
            return false;
        if (getVersion() != null ? !getVersion().equals(that.getVersion()) : that.getVersion() != null) return false;
        return isDir != null ? isDir.equals(that.isDir) : that.isDir == null;
    }

    @Override
    public int hashCode() {
        int result = getName() != null ? getName().hashCode() : 0;
        result = 31 * result + (getCollection() != null ? getCollection().hashCode() : 0);
        result = 31 * result + (getVersion() != null ? getVersion().hashCode() : 0);
        result = 31 * result + (isDir != null ? isDir.hashCode() : 0);
        return result;
    }
}
