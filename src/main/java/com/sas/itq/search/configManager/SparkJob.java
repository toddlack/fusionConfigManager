package com.sas.itq.search.configManager;

/**
 * Represent a spark job object from fusion. Read and write json file.
 */
public class SparkJob extends JsonBased implements IdentifiableString {


    @Override
    public String generateFileName() {
        return String.valueOf(get("id"));
    }
    @Override
    public String generateFileName(String parentDir) {
        StringBuilder sb= new StringBuilder(parentDir).append(generateFileName());
        return sb.toString();
    }

    /**
     * Return a string that is the PATH segment for a get or PUT. Sometimes it is ID , sometimes name
     */
    @Override
    public String getPathSegmentName() {
        return String.valueOf(get("id"));
    }
}
