package com.sas.itq.search.configManager;

import com.sas.itq.search.FusionManagerRestClient;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

/**
 * TODO: Add class javadoc
 * User: snoctl
 * Date: 12/6/2017
 * Time: 8:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class FusionCollectionTest {

    FusionCollection fusionCollection;
    ConfigManager manager;
    FusionManagerRestClient client;
    public static final String INTRANET_DIRECTORY = "src/test/resources/fusionJson/collections/intranet";

    @BeforeMethod
    public void setUp() throws Exception {
        client = new FusionManagerRestClient();
        manager = new ConfigManager();
        List<FusionCollection> temps = manager.readSourceFiles(INTRANET_DIRECTORY, "Intranet.json", FusionCollection.class);
        fusionCollection = temps.get(0);

    }

    @Test
    public void testGenerateFileName() throws Exception {

        assertEquals("Intranet/Intranet", fusionCollection.generateFileName());
    }

    @Test
    public void testGetPathSegmentName() throws Exception {
        assertEquals("Intranet", fusionCollection.getPathSegmentName());
    }

    @Test
    public void testClone() throws Exception {
        FusionCollection fcClone = fusionCollection.copy(fusionCollection, true);
        assertEquals(fcClone.getId(), fusionCollection.getId());
    }

}