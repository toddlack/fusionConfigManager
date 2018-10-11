package com.sas.itq.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sas.itq.search.configManager.QueryPipeline;
import com.sas.itq.search.configManager.QueryPipelineTest;
import com.sas.itq.search.elevate.Elevate;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Test SpreadsheetToJosn class
 */
public class SpreadsheetToJsonTest {

    private static Logger log = LoggerFactory.getLogger(SpreadsheetToJsonTest.class);

    SpreadsheetToJson spreadsheetToJson;
    String rulesFile = "src/test/resources/sascom-full-landing.json";

    @BeforeMethod
    public void setup() {
        spreadsheetToJson = new SpreadsheetToJson();
    }

    @Test

    public void testReadQpReplaceLandingPage() throws Exception {
        String fileName = "Landing-Pages-SASCOM.xlsx";
        String sheetName = "en_us";
        String testOutput = "src/test/resources/read-qp-replace-landing-page.json";
        QueryPipeline qp = getQueryPipelineFromFile();
        File newPipeline = spreadsheetToJson.replaceLandingPage(QueryPipelineTest.SASCOM_EN_US_QP, fileName, sheetName, testOutput);

        assertNotNull(newPipeline);
    }

    public QueryPipeline getQueryPipelineFromFile() throws java.io.IOException {
        URI uri = Paths.get(QueryPipelineTest.SASCOM_EN_US_QP).toUri();
        File f = new File(uri);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(f, QueryPipeline.class);
    }

    /**
     * Command line test of replacing a landing page
     *
     * @throws Exception
     */
    @Test
    public void testReplaceLandingPageCLI() throws Exception {
        Instant start = Instant.now();
        String outFileName = "src/test/resources/read-qp-replace-landing-page-command.json";
        StringBuilder argString = new StringBuilder("-sp Landing-Pages-SASCOM.xlsx ")
                .append("-qp ").append(QueryPipelineTest.SASCOM_EN_US_QP).append(" ")
                .append("-wsh en_us ")
                .append("-out ").append(outFileName).append(" ")
                .append("-c landing");

        SpreadsheetToJson.main(argString.toString().split(" "));
        //Check that outfilename exists
        FileTime ft = Files.getLastModifiedTime(Paths.get(outFileName));
        assertThat("Landing page File was not modified staert of test", start.compareTo(ft.toInstant()) < 0);

    }
    @Test
    /**
     * read the full landing page format with the matchtype column
     */
    public void readFullLandingPage() throws Exception {
        //This is in the resources directory
        String fileName = "Landing-Pages-SASCOM.xlsx";
        String sheetName = "en_is";
        String json = "{\n\"rules\": " + spreadsheetToJson.readWorksheetPage(fileName, LandingPage.FULL_COLUMNS, sheetName) + "\n}";
        Files.write(Paths.get("src/test/resources/sascom-full-landing2.json"), json.getBytes());
        assertTrue("Returned json has no keyword.", json.contains("keyword") & json.contains("url"));
    }

    @Test
    public void readSimpleLandingPage() throws Exception {
        String fileName = "landing-page-samples.xlsx";
        fileName = "sascom-full-landing.xlsx";
        String sheetName = "en_is";
        String json = "\"rules\": " + spreadsheetToJson.readWorksheetPage(fileName, LandingPage.FULL_COLUMNS, sheetName);

        Files.write(Paths.get("src/test/resources/landing-pages-sascom.json"), json.getBytes());
        assertTrue("Returned json has no keyword.", json.contains("\"keyword\" :") & json.contains("url"));
    }

    @Test
    public void readJMPLandingPage() throws Exception {
        String fileName = "Pages-JMPCOM.xlsx";
        String sheetname = "jmpcom_en_be";
//        String fileName = "Landing-Pages-JMPCOM_OCT2.xlsx";
        String json = "\"rules\": " + spreadsheetToJson.readWorksheetPage(fileName, LandingPage.FULL_COLUMNS, sheetname);
        Files.write(Paths.get("src/test/resources/landing-pages-jmp.json"), json.getBytes());
        assertTrue("Returned json has no keyword.", json.contains("\"keyword\" :") & json.contains("url"));
    }

    @Test
    public void readQueryElevationPAge() throws Exception {
        String fileName = "SASCOMQueryElevation.xlsx";
        String sheetName = "sascom_en_au";
        String xml = spreadsheetToJson.readElevationPage(fileName, Elevate.QUERY_ELEVATION_COLUMNS, sheetName);
        Files.write(Paths.get("src/test/resources/queryElevation-" + sheetName + ".xml"), xml.getBytes());
    }

    /**
     * Run command line version of query elevation .
     * @throws Exception
     */
    @Test
    public void runElevationCommandLine() throws Exception {
        StringBuilder argstring = new StringBuilder("-sp SASCOMQueryElevation.xlsx ")
                .append("--worksheet sascom_en_au ")
                .append("--output src/test/resources/commandLineOutput.xml ")
                .append("-c elevation");
        ;
        SpreadsheetToJson.main(argstring.toString().split(" "));
    }
    @Test
    public void readAllQueryElevationPAge() throws Exception {
//        String fileName = "SASCOMQueryElevation.xlsx";
        String fileName = "JMPCOMQueryElevation.xlsx";
        String sheetName = "*";
        String xml = spreadsheetToJson.readElevationPage(fileName, Elevate.QUERY_ELEVATION_COLUMNS, sheetName);
        Files.write(Paths.get("src/test/resources/jmp-elevate.xml"), xml.getBytes());
    }

    @Test
    public void readAllQueryElevationPageFromURI() throws Exception {
        String fileName = "SASCOMQueryElevation.xlsx";
        String sheetName = "*";
        InputStream excel = this.getClass().getClassLoader().getResourceAsStream(fileName);
        Workbook workbook = new XSSFWorkbook(excel);
        String xml = spreadsheetToJson.readElevationPage(Elevate.QUERY_ELEVATION_COLUMNS, sheetName, workbook);
        Files.write(Paths.get("src/test/resources/sascom-elevate-all2.xml"), xml.getBytes());
    }
}