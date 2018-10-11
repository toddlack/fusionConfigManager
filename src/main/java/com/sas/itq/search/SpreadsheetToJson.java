package com.sas.itq.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sas.itq.search.configManager.QueryPipeline;
import com.sas.itq.search.configManager.Stage;
import com.sas.itq.search.elevate.DocType;
import com.sas.itq.search.elevate.Elevate;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Convert a spreadsheet to json format for Fusion server consumption.
 * First one is to convert landingpage spreadsheet.
 */

public class SpreadsheetToJson {

    private static final String XML_VERSION_1_0_ENCODING_UTF_8 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    private static final String COMMA = "\\,";
    private static final String ASTERISK = "*";
    private static final String LANDING_PAGES_TYPE = "landing-pages";
    private static final String SPREADSHEET = "spreadsheet";
    private static final String WORKSHEET = "worksheet";
    private static final String OUTPUT_FILE = "outputFile";
    private static final String HELP = "help";
    private static final String ELEVATE = "elevation";
    private static final String LANDING = "landing";
    private static final String QUERY_PIPELINE_NAME = "query-pipeline";
    private static Row.MissingCellPolicy BAD_CELL_BLANK = Row.CREATE_NULL_AS_BLANK;
    private static final String DEFAULT_MATCH = "match";
    private static Logger log = LoggerFactory.getLogger(FusionManagerRestClient.class);

    private Options options;

    public SpreadsheetToJson() {
        initOptions();
    }


    /**
     * command line version of Spreadsheet to json
     */
    public static void main(String[] args) throws IOException, ParseException {
        SpreadsheetToJson spreadsheetToJson = new SpreadsheetToJson();
        CommandLine commandLine = spreadsheetToJson.createCommandLine(spreadsheetToJson.getOptions(), args);
        if (commandLine.hasOption(HELP)) {
            spreadsheetToJson.showHelp();
        }
        String command = commandLine.getOptionValue("command", "");
        String fileName = commandLine.getOptionValue(SPREADSHEET, "none");
        String worksheet = commandLine.getOptionValue(WORKSHEET);
        String output = commandLine.getOptionValue(OUTPUT_FILE, "");
        String qpName = commandLine.getOptionValue(QUERY_PIPELINE_NAME, "");
        if (command.equals(LANDING) && qpName.length() > 0) {
            //read landing page
            File result = spreadsheetToJson.replaceLandingPage(qpName, fileName, worksheet, output);
            log.info("Landing page from {} added to pipeline {}", fileName, qpName);
        } else if (command.equals(ELEVATE)) {
            //read query elevation
            String xml = spreadsheetToJson.readElevationPage(fileName, Elevate.QUERY_ELEVATION_COLUMNS, worksheet);
            Files.write(Paths.get(output), xml.getBytes());
            log.info("** Success - New query elevation page written to {}", output);
        }

    }

    public CommandLine createCommandLine(Options options, String[] mainArgs) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = null;
        try {
            commandLine = parser.parse(options, mainArgs);
        } catch (ParseException e) {
            log.info("error parsing options : {}", mainArgs);
            showHelp();
            throw (e);
        }
        return commandLine;
    }

    public File replaceLandingPage(String pipelineName, String spreadsheetName, String worksheetName, String outputFileName) throws IOException {
        QueryPipeline qp = readQueryPipeLineFile(pipelineName);
        List<LandingPage> pages = readWorksheetPage(spreadsheetName, LandingPage.FULL_COLUMNS, worksheetName);
        String newJson = replaceLandingPage(qp, pages);
        Path outputPath = Paths.get(outputFileName);
        Files.write(outputPath, newJson.getBytes());
        return outputPath.toFile();
    }
    /**
     * Go to spreadsheet and build replacement landingpage section for a given Query pipeline
     *
     * @param qp       populated QueryPipeline
     * @param landingPages The list of new landing pages read from the spreadsheet
     * @return The new contents of the query pipeline file, which will need to be written back to disk
     */
    public String replaceLandingPage(QueryPipeline qp, List<LandingPage> landingPages) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        Stage lp = Arrays.stream(qp.getStages())
                .filter(stage -> stage.get("type").equals(LANDING_PAGES_TYPE))
                .findFirst()
                .get();
        lp.set("rules", landingPages);

        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, qp);
        return sw.toString();
        //Add a return and tab between  '}, {'
//        String resultJson = sw.toString().replaceAll("\\}\\, \\{", "\\}\\,\\\r\\\n\\\t{");
//        return resultJson;
    }

    /**
     * Read a landingpage spreadsheet, and convert to JSON. This JSON will need to be wrapped in a "rules" section
     *
     * @param spreadsheetName
     * @param columns   - A map where the name of the column has a value that is the column number in the spreadsheet. For example, URL=1 for column 1
     * @param sheetName - If specified, use the specific sheet. If null, use the first sheet in the workbook.
     * @return JSON formatted string of landingpage rules to apply.
     */

    public List<LandingPage> readWorksheetPage(String spreadsheetName, Map<String, Integer> columns, String sheetName) throws IOException {
        List<LandingPage> pages = new ArrayList<>();
        log.debug("Reading file {} to landing page JSON", spreadsheetName);
        InputStream excel = this.getClass().getClassLoader().getResourceAsStream(spreadsheetName);
        Workbook workbook = new XSSFWorkbook(excel);
        Sheet sheet = null;
        if (sheetName == null) {
            sheet = workbook.getSheetAt(0); //skip row0 for now - it is the column titles
        } else {
            sheet = workbook.getSheet(sheetName);
        }
        Iterator<Row> rowIterator = sheet.rowIterator();
        //String fullSheet = mapper.writeValueAsString(sheet);
        Row firstRow = rowIterator.next();
        while (rowIterator.hasNext()) {
            Row currentRow = rowIterator.next();
            pages.addAll(getLandingPage(currentRow, columns));
        }
        return pages;
    }

    /**
     * Read a spreadsheet page into an XML string to push up to the SOLR instance for Query elevation
     * @param fileName
     * @param columns
     * @param sheetName If an asterisk, read all sheets in the workbook. If null, read the first sheet. Otherwise take the sheet name.
     * @return String version of the elevate.xml based on the tab(s) specified in teh spreadsheet.
     * @throws IOException
     */
    public String readElevationPage(String fileName, Map<String, Integer> columns, String sheetName) throws IOException {
        log.debug("Reading file {} to Elevate", fileName);
        InputStream excel = this.getClass().getClassLoader().getResourceAsStream(fileName);
        Workbook workbook = new XSSFWorkbook(excel);
        return readElevationPage(columns, sheetName, workbook);
    }

    public String readElevationPage(Map<String, Integer> columns, String sheetName, Workbook workbook) {
        StringBuilder retVal = new StringBuilder();
        Elevate ev = new Elevate();
        List<Sheet> sheets = getSheets(sheetName, workbook);

        List<Elevate.Query> qeList = readElevateSheetList(columns, sheets);
        ev.getQuery().addAll(qeList);
        /* should look like :
        <elevate>
            <query text="sas studio">
               <doc id="https://www.sas.com/en_is/software/studio.html"/>
               <doc id="https://www.sas.com/en_us/webinars/studio-advanced-topics/register.html"/>
            </query>
         <query text="studio">
                <doc id="https://www.sas.com/en_is/software/studio.html"/>
                <doc id="https://www.sas.com/en_us/webinars/studio-advanced-topics/register.html"/>
         </query>
            ...
         */

        StringWriter sw = new StringWriter();
        try {
            JAXBContext jc = JAXBContext.newInstance(Elevate.class);
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(ev, sw);
            retVal.append(sw.toString());
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return retVal.toString();
    }

    public List<Sheet> getSheets(String sheetName, Workbook workbook) {
        List<Sheet> sheets = new ArrayList<>();
        if (sheetName == null) {
            sheets.add(workbook.getSheetAt(0));
        } else if (sheetName.equals(ASTERISK)) {
            int n = workbook.getNumberOfSheets();
            for (int i = 0; i < n; i++) {
                sheets.add(workbook.getSheetAt(i));
            }
        }
        //Single named sheet
        else {
            sheets.add(workbook.getSheet(sheetName));
        }
        return sheets;
    }

    public QueryPipeline readQueryPipeLineFile(String sourceFileName) throws IOException {
        URI uri = Paths.get(sourceFileName).toUri();
        File f = new File(uri);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(f, QueryPipeline.class);
    }

    /**
     * Send in list of query elevation sheets to read. Organize all docs into terms
     *
     * @param columns
     * @param sheetList
     * @return
     */
    private List<Elevate.Query> readElevateSheetList(Map<String, Integer> columns, List<Sheet> sheetList) {
        List<Elevate.Query> retList = new ArrayList<>();
        //Keep track of docs and group by terms across spreadsheets
        Map<String, List<DocType>> termMap = new HashMap<>();
        for (Sheet singleSheet : sheetList) {
            Iterator<Row> rowIterator = singleSheet.rowIterator();
            //Get header row out of the way
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }
            while (rowIterator.hasNext()) {
                Row currentRow = rowIterator.next();
                List<Elevate.Query> qeList = getQueryElevation(currentRow, columns);
                groupDocs(termMap, qeList);
            }
        }
        //Create concatenated Elevate.query objects from map
        for (String term : termMap.keySet()) {
            if (term.length() > 0) {
                Elevate.Query eq = new Elevate.Query();
                eq.setText(term);
                eq.getDocs().addAll(termMap.get(term));
                retList.add(eq);
            }
        }
        return retList;
    }

    /**
     * Keep docs grouped together in the same term.
     *
     * @param master
     * @param queries
     */
    private void groupDocs(Map<String, List<DocType>> master, List<Elevate.Query> queries) {
        for (Elevate.Query query : queries) {
            String term = query.getText();
            if (master.containsKey(term)) {
                master.get(term).addAll(query.getDocs());
            } else {
                master.put(term, query.getDocs());
            }
        }
    }

    private List<LandingPage> getLandingPage(Row currentRow, Map<String, Integer> columns) {
        List<LandingPage> landingList = new ArrayList<>();
        String keywords = currentRow.getCell(columns.get(LandingPage.KEYWORD), BAD_CELL_BLANK).getStringCellValue();
        String urls = currentRow.getCell(columns.get(LandingPage.URL), BAD_CELL_BLANK).getStringCellValue();
        String title = currentRow.getCell(columns.get(LandingPage.TITLE), BAD_CELL_BLANK).getStringCellValue();
        String mode = DEFAULT_MATCH;
        Integer modeIndex = columns.get(LandingPage.MODE);
        if (modeIndex != null) {
            mode = currentRow.getCell(modeIndex).getStringCellValue();
        }
        //Only add if both keywords and urls are non empty
        if (!(keywords.isEmpty() && urls.isEmpty())) {
            landingList.addAll(keywordSeparation(keywords, mode, urls, title));
        }
        return landingList;
    }

    private List<Elevate.Query> getQueryElevation(Row currentRow, Map<String, Integer> columns) {
        List<Elevate.Query> retList = new ArrayList<>();
        String queries = currentRow.getCell(columns.get(Elevate.QUERY), BAD_CELL_BLANK).getStringCellValue();
        String ids = currentRow.getCell(columns.get(Elevate.ID), BAD_CELL_BLANK).getStringCellValue();
        retList.addAll(querySeparation(queries, ids));
        return retList;
    }

    /**
     * Take comma delimited search terms (<b>query</b> column) and urls (<b>id</b> column) and 'expand' out
     * into the proper query elevation format.
     *
     * @param queries
     * @param ids
     * @return
     */
    private List<Elevate.Query> querySeparation(String queries, String ids) {
        List<Elevate.Query> retVal = new ArrayList<>();
        String[] keySets = queries.split(COMMA);
        String[] idList = ids.split(COMMA);

        for (int i = 0; i < keySets.length; i++) {
            Elevate.Query q = new Elevate.Query();
            String query = keySets[i].trim();
            q.setText(query);
            //now get all doctypes from idList
            for (int j = 0; j < idList.length; j++) {
                DocType d = new DocType();
                d.setId(idList[j].trim());
                q.addDoc(d);
            }
            retVal.add(q);
        }
        return retVal;
    }

    //Split out the  comma separated keyword
    private List<LandingPage> keywordSeparation(String keywords, String mode, String urls, String title) {
        List<LandingPage> retVal = new ArrayList<>();

        String[] keySets = keywords.split("\\,");
        for (int i = 0; i < keySets.length; i++) {
            String keySet = keySets[i].trim();
            LandingPage lp = new LandingPage();
            lp.setMode(mode);
            lp.setKeyword(keySet);
            //Construct the url field, which has a title in it, and maybe mroe than one url and title (comma separated)
            lp.setUrl(urlsTitlesMerge(urls, title));
            if (!keySet.isEmpty()) {
                retVal.add(lp);
            }
        }
        return retVal;
    }

    /**
     * Handle multiple urls and titles - assumming there is a one-to-one correspondence of
     * comma separated titles and comma separated urls - "url1 , url2" then title is "title1 , title2".
     * The format of multiple urls for a landing page term will be url1<b>|</b>title1<b>^</b>url2<b>|</b>title2
     *
     * @param urlsString
     * @param titlesString
     * @return
     */
    private String urlsTitlesMerge(String urlsString, String titlesString) {
        String[] urlArr = urlsString.split(",");
        String[] titleArr = titlesString.split(",");

        StringBuilder retVal = new StringBuilder();
        for (int i = 0; i < urlArr.length; i++) {
            String url = urlArr[i].trim();
            //Be safe with the titleArr - it may not have the same length.
            //Some urls have commas in them, messing up the mulitple url parsing.
            String title;
            if (i == 0) {
                title = titleArr[i];
            } else if (i > 0 && titleArr.length >= (i + 1)) {
                title = titleArr[i].trim();
            } else if (i > 0) {
                title = titleArr[i - 1];
            } else {
                title = titleArr[i];
            }

            if (i > 0) {
                retVal.append("^");
            }
            retVal.append(url).append("|").append(title);
        }
        return retVal.toString();
    }

    /**
     * Available options for json to spreadsheet
     */
    private void initOptions() {
        if (options == null) {
            options = new Options();
        }

        boolean hasArg = true;
        boolean noArg = false;
        Option spreadsheet = Option.builder("sp").required(true).longOpt(SPREADSHEET)
                .desc("Path to spreadsheet, relative to current running directory")
                .hasArg(hasArg)
                .build();
        options.addOption(spreadsheet);
        options.addOption("wsh", WORKSHEET, hasArg, "Regex for worksheet name");
        options.addOption("out", OUTPUT_FILE, hasArg, "path for file output. For elevation files, it is xml. For landing pages it is a JSON file");
        options.addOption(HELP, noArg, "print this message.");
        options.addOption("c", "command", hasArg, "which command to run : " + ELEVATE + "  or " + LANDING);
        options.addOption("qp", QUERY_PIPELINE_NAME, hasArg, "Query pipeline file to read in for the landing page. Landing pages are only in query pipelines.");

    }

    public void showHelp() {
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp(LANDING, options);
    }

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }
}
