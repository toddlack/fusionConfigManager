package com.sas.itq.search.configManager;

import java.nio.file.Path;

/**
 * GenerateFilename will be used for fusion entities when writign to a file. We will need to generate a filename for each
 * entity based on its Id , name, or userName. There should be this one method that returns the appropriate filename.
 *
 * User: snoctl
 * Date: 11/10/2017
 * Time: 8:54 AM
 * To change this template use File | Settings | File Templates.
 */
public interface IdentifiableString {
    public static String JSON=".json";
    String generateFileName();
    String generateFileName(String parentDir);

    /**
     * Return a string that is the PATH segment for a get or PUT. Sometimes it is ID , sometimes name
     */
    String getPathSegmentName();
}
