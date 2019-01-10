package com.sas.itq.search.configManager;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TODO: Add class javadoc
 * User: snoctl
 * Date: 9/7/2018
 * Time: 1:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigManagerCLITest {

    public static final String propsFile = "src/test/resources/cli/test41.properties";
    ConfigManagerCLI cli;

    @BeforeMethod
    public void setup() throws IOException {
        String[] args={"-pf", "src/test/resources/cli/test41.properties",
                "-cmf", "src/test/resources/cli/test41cmd.txt"};
        cli = new ConfigManagerCLI(args);
    }

    @Test
    public void testRun() throws IOException {
        List<String> commands = cli.getCommandList().collect(Collectors.toList());
        Assert.assertNotNull(commands);
        cli.processCommands();
    }
}