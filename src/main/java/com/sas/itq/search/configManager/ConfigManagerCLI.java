package com.sas.itq.search.configManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.sas.itq.search.FusionManagerRestClient;
import org.apache.commons.cli.*;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Commandline interface to manage Fusion configuration updates and migration.
 */
public class ConfigManagerCLI {
    //static values for properties
    public static final String SERVER_NAME = "server.name";
    public static final String AUTHENTICATION_ENCODED = "authentication.encoded";
    public static final String AUTHENTICATION_CLEAR = "authentication.clear";
    public static final String AUTHENTICATION_REALM = "authentication.realm";
    public static final String SERVER_PORT = "server.port";
    public static final String SERVER_PROTOCOL = "server.protocol";
    public static final String SERVER_FULL_URL = "server.url";
    public static final String PROPERTY_FILE_PATH_SHORT = "pf";
    public static final String PROPERTY_FILE_PATH = "properties.file";
    public static final String COMMAND_FILE_PATH = "command.file";
    public static final String CONFIG_DIRECTORY_SHORT = "conf";
    public static final String CONFIG_DIRECTORY = "config.directory";
    public static final String COLLECTIONS_DIRECTORY = "collections.directory";
    public static final String APPLICATION_NAME = "application.name";
    public static final String QUIET_OUPUT = "quiet";


    public static final String COLLECTION_NAME = "collection.name";
    public static final String INDEX_PIPELINE = "index.pipeline";
    public static final String QUERY_PIPELINE = "query.pipeline";
    public static final String DEFAULT_CONFIG_DIRECTORY = "fusionconf";
    //Default REST endpoints
    public static final String PATH_SOLRCONFIG = "/solr-config";
    public static final String PATH_COLLECTIONS = "/api/apollo/collections";
    public static final String PATH_JOBS = "/api/apollo/connectors/jobs";
    public static final String PATH_SCHEDULES = "/api/apollo/scheduler/schedules";
    public static final String PATH_QUERY_PIPELINES = "/api/apollo/query-pipelines";
    public static final String PATH_INDEX_PIPELINES = "/api/apollo/index-pipelines";
    public static final String PATH_OBJECTS = "/api/apollo/objects";
    public static final String COMMAND_GET = "get";
    public static final String COMMAND_UPDATE = "update";
    public static final String COMMAND_SET = "set";
    public static final String COMMAND_QUIT = "quit" ;
    public static final String COMMAND_HELP = "help" ;
    public static final String COMMAND_LOAD = "load";
    public static final String COMMAND_VAR = "var";

    //Common to commandLine applications
    String fileSeparator = System.getenv("file.separator");
    Options appOptions;
    Options commandOptions;
    //Hold variables for substitution
    Map<String,String> variables;
    //options that are set from a properties file. They are overridden by commandline options
    Properties configOptions;
    CommandLine sessionCommandLine;
    //Specific to fusion
    ConfigManager manager;
    FusionManagerRestClient client;
    /* Use for multiple calls - save the session and re-use */
    Cookie sessionCookie;
    private static Logger logger = LoggerFactory.getLogger(ConfigManagerCLI.class);
    private CommandLineParser commandLineParser;
    private Set<String> validTypes;

    public static void main(String[] args) throws IOException {
        ConfigManagerCLI configCli = new ConfigManagerCLI(args);
        StringBuilder bld = new StringBuilder();
        System.out.println("Running fusion config manager:");
        Arrays.stream(args).forEach(arg -> bld.append(arg).append(" "));
        System.out.println(bld);
        //If command file specified, run those commands, otherwise use stdin
        configCli.processCommands();
    }

    public ConfigManagerCLI() {
        manager = new ConfigManager();
        client = new FusionManagerRestClient();
        appOptions = startOptions();
        commandOptions = commandOptions();
        commandLineParser = new DefaultParser();
        configOptions = new Properties();
        variables = new LinkedHashMap<>();
        validTypes = Arrays.stream(EntityType.values())
                .map(et -> et.name())
                .collect(Collectors.toSet());
    }

    /**
     * Initialize with the command line arguments and apply those arguments
     * over any loaded properties. Command line arguments are the same as the
     * properties set in the properties file.
     *
     * @param args Typically command line style arguments to be parsed by the CLI package.
     */
    public ConfigManagerCLI(String[] args) throws IOException {
        this();
        buildArgs(this.appOptions, args);
        String propsFile = sessionCommandLine.getOptionValue(PROPERTY_FILE_PATH_SHORT);
        if (propsFile != null) {
            loadProps(propsFile);
        }
    }

    public FusionManagerRestClient changeUrl(FusionManagerRestClient fClient, String baseUrl) {
        fClient.setBaseUrl(baseUrl);
        //Get authentication session cookie
        String realm = getOptionValue(AUTHENTICATION_REALM, "native");
        fClient.setAuthenticationRealm(realm);
        String authString = getOptionValue(AUTHENTICATION_ENCODED, null);
        boolean authEncoded = (authString != null);
        if (authString == null) {
            authString = getOptionValue(AUTHENTICATION_CLEAR, null);
        }
        sessionCookie = fClient.session(authString, authEncoded);
        fClient.addCookie(sessionCookie);
        return fClient;
    }

    public String getOptionValue(String propertyName, String defaultVal) {
        String retVal = sessionCommandLine.getOptionValue(propertyName);
        if (retVal == null) {
            retVal = configOptions.getProperty(propertyName);
        }
        if (retVal == null) {
            retVal = defaultVal;
        }
        return retVal;
    }

    /**
     * Process commands. If a command file was specified use that. Otherwise, read from Stdin.
     *
     * @throws IOException
     */
    public void processCommands() throws IOException {
        Stream<String> lines = null;
        String cmdPath = sessionCommandLine.getOptionValue(COMMAND_FILE_PATH);
        if (cmdPath == null) {
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            lines = stdin.lines();
        } else {
            lines = Files.lines(Paths.get(cmdPath));
        }
        processCommands(lines);
    }

    public void processCommands(Stream<String> lines) {
        lines.filter(line -> !line.startsWith("#") && !line.startsWith(" "))
                .forEach(line -> runSingleCommand(parseCommand(line)));
    }

    private void runSingleCommand(CommandLine commandLine) {
        StreamSupport.stream(Spliterators.spliteratorUnknownSize(commandLine.iterator(), Spliterator.ORDERED), false)
                .forEach(opt -> {
                    String option = resolveString(opt.getOpt(),variables);
                    String[] optVals = opt.getValues();
                    if (optVals == null) {
                        optVals = new String[0];
                    }
                    String[] values = new String[optVals.length];
                    for (int i = 0; i < optVals.length; i++) {
                        values[i] = resolveString(optVals[i],variables);
                    }

                        boolean ok = false;
                        switch (option) {
                            case COMMAND_SET:
                                logCmd(commandLine);
                                Properties p = new Properties();
                                p.setProperty(values[0], values[1]);
                                //single property set.
                                setConfigValues(p);
                                ok = true;
                                break;
                            case COMMAND_GET:
                                logCmd(commandLine);
                                int rc=runGet(values);
                                ok = (rc<1);
                                break;
                            case COMMAND_UPDATE:
                                logCmd(commandLine,Optional.of(" --update"));
                                rc=runUpdate(values);
                                ok = (rc<1);
                                break;
                            case COMMAND_VAR:
                                if (values.length >= 2) {
                                    StringBuilder sb = new StringBuilder(" --var ").append(values[0])
                                            .append(" assigned the value of ").append(values[1]);
                                    String resultVal = variables.put(values[0], values[1]);
                                    if (resultVal != null) {
                                        sb.append(" (changed from ").append(resultVal);
                                    }
                                    logCmd(commandLine, Optional.of(sb.toString()));
                                    ok = true;
                                } else {
                                    logCmd(commandLine,Optional.of(" error in assigning variable - not enough info. "));
                                    ok=false;
                                }
                                break;
                            case COMMAND_HELP:
                                usage(commandOptions);
                                break;

                            case COMMAND_QUIT:
                                logCmd(commandLine);
                                System.exit(0);
                                default:
                                    //assume a variable assignment
                                    logCmd(commandLine,Optional.of(" ---- unknown command"));
                                    break;

                        };

                });
    }

    public String resolveString(String templateString,Map<String,String> varMap) {
        StringSubstitutor sub = new StringSubstitutor(varMap);
        return sub.replace(templateString);
    }
    private void logCmd(CommandLine cmd) {
        logCmd(cmd,Optional.empty());
    }
    private void logCmd(CommandLine commandLine,Optional<String> postMsg) {
        if (!sessionCommandLine.hasOption(QUIET_OUPUT)) {
            Iterator<Option> iter = commandLine.iterator();
            StringBuilder line = new StringBuilder();
            while (iter.hasNext()) {
                Option cur = iter.next();
                line.append(cur.getOpt()).append(" ");
                String[] vals =cur.getValues();
                if (vals != null) {
                    for (int i = 0; i < vals.length; i++) {
                        String val = vals[i];
                        line.append(val).append(" ");
                    }
                }
            }
            if (postMsg.isPresent()) {
                line.append(postMsg.get());
            }
            System.out.println(line.toString().trim());
        }
    }

    /** Do the GET command */
    public int runGet(String[] values) {
        int rc=0;
        EntityType[] types = parseEntityTypes(values[0]);
        //now get any filters
        String regex = values[1];
        Predicate<JsonNode> filter = ConfigManager.nodePredicate(regex, "id");
        String dir = this.configOptions.getProperty(CONFIG_DIRECTORY, DEFAULT_CONFIG_DIRECTORY);
        Map<String, Boolean> resultMap = null;
        StringBuilder cmdSb=new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            cmdSb.append(value).append(" ");
        }
        try {
            if (types[0].equals(EntityType.OBJECT)) {
                String[] params = new String[values.length-1];
                //check for separate files
                String separateParm="false";
                int pindex=0;
                for (int i = 0; i < values.length; i++) {
                    String value = values[i];
                    if (value.toLowerCase().startsWith("separate")) {
                        String[] seps = value.split("=");
                        if (seps.length>1) {
                            separateParm=seps[1];
                        }
                    }  else if (!value.equalsIgnoreCase("object")) {
                        params[pindex++] = value;
                    }
                }
                Boolean separate = Boolean.valueOf(separateParm);
                resultMap = manager.copyFromServer(client,Paths.get(dir),separate,params);
            } else {
                resultMap = manager.copyFromServer(client, dir, types, "temp", filter);
            }

        } catch (IOException e) {
            logger.error("problem getting config data from server ", e);
            rc=6;
        }
        if (resultMap != null) {
            logger.info("Command: {}  ---run with {} results",cmdSb.toString(), resultMap.size());
        }
        return rc;
    }

    /** Do the UPDATE command */
    public int runUpdate(String[] values) {
        int rc=0;
        EntityType[] types = parseEntityTypes(values[0]);
        //now get any filters
        String regex = values[1];
        Predicate<File> filter = file->{return file.getName().matches(regex);};
        String dir = this.configOptions.getProperty(CONFIG_DIRECTORY, DEFAULT_CONFIG_DIRECTORY);
        List<Response> results=null;
        results = manager.copyToServer(client, dir, types,filter);
        if (results != null) {
            int numCalls=results.size();
            int numErrs=0;
            logger.info("Command run with {} results", results.size());
            results.stream()
                    .filter(r -> r.getStatus()>399)
                    .map(r -> {StringBuilder sb=new StringBuilder();
                        sb.append("RC:").append(r.getStatus()).append(" --").append(r.readEntity(String.class));
                        return sb.toString();
                    })
                    .forEach(msg -> logger.error(msg));
        } else {rc=4;}
        return rc;
    }

    private EntityType[] parseEntityTypes(String value) {
        //parse comma delimited entity types
        String[] typeArray = value.split(",");
        SortedSet<EntityType> typeSet = Collections.synchronizedSortedSet(new TreeSet<>());
        //Add all matching regexes OR an exact match  ignoring case
        for (String cur : typeArray) {
            String regex="(?i)"+cur;
            validTypes.stream()
                    .filter(et -> et.matches(regex) || et.equals(cur.toUpperCase()))
                    .forEach(et -> typeSet.add(EntityType.valueOf(et)));
        }
        return typeSet.toArray(new EntityType[typeSet.size()]);
    }

    /**
     * PArse a line for a command nad its options
     *
     * @param command
     */
    public CommandLine parseCommand(String command) {
        CommandLine cmdLine;

        //Options parser needs a dash in front to parse.
        StringBuilder fakedCommand = new StringBuilder("-").append(command);
        String[] comArray = fakedCommand.toString().split(" ");
        try {
            cmdLine = commandLineParser.parse(commandOptions, comArray);
        } catch (ParseException e) {
            cmdLine = new CommandLine.Builder().build();
        }

        return cmdLine;
    }



    private void buildArgs(Options options, String[] args) {


        logger.debug("checking command line options.");
        try {
            sessionCommandLine = commandLineParser.parse(options, args);
        } catch (ParseException e) {
            logger.error("ParseException", e);
            usage(options);
            System.exit(1);
        }

        if (sessionCommandLine.hasOption("help")) {
            logger.debug("Help argument detected, listing command line options.");
            usage(options);
            System.exit(0);
        }
    }


    /**
     * Load properties from file, and then override with any command-line options.
     *
     * @param filePath
     */
    void loadProps(String filePath) throws IOException {
        Properties props = new Properties();
        props.load(Files.newInputStream(Paths.get(filePath)));
        overrideOptions(props);
    }

    /**
     * Take command line args in place of config file properties
     */
    private void overrideOptions(Properties properties) {
        Option[] opt = sessionCommandLine.getOptions();
        //Options long names are properties separated with an underscore.
        // Any options set in the command line override the ones loaded in the properties file.
        for (int i = 0; i < opt.length; i++) {
            Option option = opt[i];
            String optProp = option.getLongOpt().replace("_", ".");
            properties.setProperty(optProp, option.getValue());
        }
        setConfigValues(properties);

    }

    /**
     * Set fusion rest client's properties based on given Properties
     *
     * @param properties
     */
    public void setConfigValues(Properties properties) {
        properties.stringPropertyNames().stream()
                .forEach(name -> configOptions.setProperty(name, properties.getProperty(name)));
        String url = properties.getProperty(SERVER_FULL_URL);
        String authenticationRealm = properties.getProperty(AUTHENTICATION_REALM);

        if (url == null) {
            String protocol = properties.getProperty(SERVER_PROTOCOL, "https");
            String server = properties.getProperty(SERVER_NAME);
            String port = properties.getProperty(SERVER_PORT);
            StringBuilder urlBuild = new StringBuilder();
            if (server!=null) {
                urlBuild.append(protocol).append("://").append(server);
                if (port != null) {
                    urlBuild.append(":").append(port);
                }
                url = urlBuild.toString();
            }
        }
        if (url != null) {
            changeUrl(client, url);
        }
    }

    /**
     * For command lines
     * <ul>
     * <li>get <i>type</i> <i>regex</i></li>
     * <li>put <i>type</i> <i>regex</i>||<i>body</i>   {read from the type's directory or the body directly (how to determine which)}</li>
     * <li>post <i>type</i> <i>regex</i> ||<i>body</i>   {read from the type's directory or the body directly (how to determine which)}</li>
     * </ul>
     *
     * @return
     */
    private Options commandOptions() {
        Options options = new Options();
        Option getVerb = Option.builder(COMMAND_GET).argName("entity-name").argName("name regex").desc("get an entity (datasource,pipeline definition, etc) from the server. Filtered by the optional regex. Server url should be previously set.")
                .hasArgs().numberOfArgs(Option.UNLIMITED_VALUES).valueSeparator(' ').build();
        Option putVerb = Option.builder(COMMAND_UPDATE).argName("entity-name").desc("update an existing entity on the server. Server url should be previously set.")
                .hasArgs().numberOfArgs(2).valueSeparator(' ').build();
        Option setCmd = Option.builder(COMMAND_SET).argName("property").argName("value").desc("sets a property value").
                hasArgs().numberOfArgs(2).valueSeparator(' ').build();
        Option helpCmd = Option.builder(COMMAND_HELP).argName("command").desc("Display Command Line commands").optionalArg(true).build();
        Option loadCmd = Option.builder(COMMAND_LOAD).argName("propertiesFile").desc("Load settings from the specified properties file").optionalArg(true).build();
        Option quitCmd = Option.builder(COMMAND_QUIT).argName("return-code").desc("Quit the command processing").optionalArg(true).build();
        Option varCmd = Option.builder(COMMAND_VAR).argName("varName=varValue").desc("Assign a variable. use ${varName} in commands to substitute.")
                .numberOfArgs(2).valueSeparator('=').build();
        //The verbs are mutually exclusive
        OptionGroup group = new OptionGroup();
        group.addOption(getVerb);
        group.addOption(putVerb);
        group.addOption(setCmd);
        group.addOption(helpCmd);
        group.addOption(quitCmd);
        group.addOption(loadCmd);
        group.addOption(varCmd);
        options.addOptionGroup(group);

        return options;
    }

    private Options startOptions() {
        Options options = new Options();
        Option propFile = Option.builder(PROPERTY_FILE_PATH_SHORT).argName("file").desc("file to load initial options. Must be visible in classpath")
                .longOpt(PROPERTY_FILE_PATH).hasArg().build();
        Option configDir = Option.builder(CONFIG_DIRECTORY_SHORT).argName("config-directory").longOpt(CONFIG_DIRECTORY)
                .desc("Parent directory in file system - start importing from this location")
                .hasArg().build();
        Option collectionsDir = Option.builder("colldir").argName("collection-dir").longOpt(COLLECTIONS_DIRECTORY)
                .desc("Directory in config directory that holds the Colleciton json definitons. Copy to and from here.")
                .hasArg().build();
        Option serverName = Option.builder("srv").argName("server-name").longOpt(SERVER_NAME)
                .desc("Server host for Fusion - ex: stereo.unx.sas.com")
                .hasArg().build();
        Option serverUrl = Option.builder("url").argName("server-url").longOpt(SERVER_FULL_URL)
                .desc("Full url host for Fusion, in case you do not want to build it with protocol, host and port options. This overrides" +
                        "server.protocol, server.name and server.port options - ex: https://stereo.unx.sas.com:8443")
                .hasArg().build();
        Option serverAuth = Option.builder("auth").argName("auth-encoded").longOpt(AUTHENTICATION_ENCODED)
                .desc("Encoded authorization string")
                .hasArg().build();
        Option serverAuthClear = Option.builder("ac").argName("auth-clear").longOpt(AUTHENTICATION_CLEAR)
                .desc("Clear text authorization string")
                .hasArg().build();
        Option serverPortOpt = Option.builder("p").argName("port-number").longOpt(SERVER_PORT)
                .desc("Port number where Fusion is listening - ex: 8764 or 8443")
                .hasArg().build();
        Option serverProtocol = Option.builder("protocol").argName("server-protocol").longOpt(SERVER_PORT)
                .desc("Port number where Fusion is listening - ex: 8764 or 8443")
                .hasArg().build();
        Option collection = Option.builder("coll").argName("collection name").longOpt(COLLECTION_NAME)
                .desc("Which collection to add documents. Example: 'intranet'")
                .hasArg().build();
        Option index = Option.builder("ix").argName("index pipeline").longOpt(INDEX_PIPELINE)
                .desc("Which indexing pipeline to use. Ex: 'defects' or 'midas'")
                .hasArg().build();
        Option commandFile = Option.builder("cmf").argName("command file").longOpt(COMMAND_FILE_PATH)
                .desc("File that is a list of commands for the config manager to run")
                .hasArg().build();
        Option dryRun = Option.builder("dry").argName("dry run").desc("do not execute the commands - only list what would be executed").build();
        Option quiet = Option.builder("q").argName("quiet").desc("Do not echo commands as they are executed").longOpt(QUIET_OUPUT).build();
        options.addOption(propFile);
        options.addOption(configDir);
        options.addOption(collectionsDir);
        options.addOption(commandFile);
        options.addOption(serverName);
        options.addOption(serverPortOpt);
        options.addOption(serverProtocol);
        options.addOption(serverAuth);
        options.addOption(serverAuthClear);
        options.addOption(serverUrl);
        options.addOption(index);
        options.addOption(collection);
//        options.addOption(dryRun);
        options.addOption(quiet);

        return options;
    }

    public static void usage(Options options) {
        // automatically generate the help statement
        HelpFormatter formatter = new HelpFormatter();
        formatter.setDescPadding(5);

        formatter.setWidth(132);
        String header = "Interface to Fusion configurations - datasources, pipelines, solr, etc. ";
        String footer="Read commands from a command file or piped to stdin";
        if (options.hasOption(COMMAND_VAR)) {
            formatter.setLongOptPrefix("");
            formatter.setOptPrefix("");
            header=" command syntax for Fusion config manager";
            footer="Process commands until quit.";
        }
        formatter.printHelp("configMgr", header,options,footer,true);
    }

}
