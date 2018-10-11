package com.sas.itq.search.configManager;

/**
 * TODO: Add class javadoc
 * User: snoctl
 * Date: 9/30/2018
 * Time: 8:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class FusionGroup extends JsonBased implements IdentifiableString {

    @Override
    public String generateFileName() {
        return String.valueOf(get("name")+JSON);    }
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
        return String.valueOf(get("name"));
    }
}
