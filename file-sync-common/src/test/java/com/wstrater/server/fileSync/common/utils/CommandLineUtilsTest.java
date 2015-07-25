package com.wstrater.server.fileSync.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Test;

import com.wstrater.server.fileSync.common.utils.Constants.SyncEnum;

public class CommandLineUtilsTest {

  private String captureHelp(CommandLineUtils cli) throws IOException {
    String ret = null;

    StringWriter writer = new StringWriter();
    try {
      PrintWriter printer = new PrintWriter(writer);
      try {
        cli.displayHelp(printer);
      } finally {
        printer.close();
      }
    } finally {
      writer.close();
    }

    ret = writer.toString();

    return ret;
  }

  private String[] getArgs(String line) {
    return line.split("[\\s]");
  }

  @Test
  public void testHelp() throws Exception {
    String argName = CommandLineUtils.HOST_ARG;
    CommandLineUtils cli = new CommandLineUtils(getClass());

    String help = captureHelp(cli);
    assertNotNull("Missing help", help);
    assertFalse(String.format("Should not have found help: %s", argName), help.indexOf(argName) >= 0);

    cli.useClient();

    help = captureHelp(cli);
    assertNotNull("Missing help", help);
    int at = help.indexOf(argName);
    assertTrue(String.format("Should have found help: %s", argName), at >= 0);

    cli.useClient();

    help = captureHelp(cli);
    assertNotNull("Missing help", help);
    at = help.indexOf(argName);
    assertTrue(String.format("Should have found help: %s", argName), at >= 0);
    at = help.indexOf(argName, at + 1);
    assertTrue(String.format("Should have found help once: %s", argName), at >= 0);
  }

  @Test
  public void testOptionalArgWithout() throws Exception {
    String argName = CommandLineUtils.ALLOW_DELETE_ARG;
    CommandLineUtils cli = new CommandLineUtils(getClass());
    cli.useAllow();
    assertTrue("Not able to parse args", cli.parseArgs(getArgs(String.format("--%s", argName))));
    assertTrue(String.format("Should have %s", argName), cli.hasAllowDelete());
    assertFalse(String.format("Shouldn't have %s", CommandLineUtils.ALLOW_WRITE_ARG), cli.hasAllowWrite());
    assertTrue(String.format("Should be true %s", argName), cli.isAllowDelete());
  }

  @Test
  public void testOptionalArgWithFalse() throws Exception {
    String argName = CommandLineUtils.REMOTE_DELETE_ARG;
    CommandLineUtils cli = new CommandLineUtils(getClass());
    cli.useAllow();
    assertTrue("Not able to parse args", cli.parseArgs(getArgs(String.format("--%s false", argName))));
    assertTrue(String.format("Should have %s", argName), cli.hasRemoteDelete());
    assertFalse(String.format("Should be true %s", argName), cli.isRemoteDelete());
  }

  @Test
  public void testOptionalArgWithTrue() throws Exception {
    String argName = CommandLineUtils.REMOTE_WRITE_ARG;
    CommandLineUtils cli = new CommandLineUtils(getClass());
    cli.useAllow();
    assertTrue("Not able to parse args", cli.parseArgs(getArgs(String.format("--%s true", argName))));
    assertTrue(String.format("Should have %s", argName), cli.hasRemoteWrite());
    assertTrue(String.format("Should be true %s", argName), cli.isRemoteWrite());
  }

  @Test
  public void testProperties() throws Exception {
    String argName = CommandLineUtils.PLAN_ARG;
    CommandLineUtils cli = new CommandLineUtils(getClass());
    cli.usePlan();

    SyncEnum expectedArg = SyncEnum.Both;
    assertTrue("Should have required arg", cli.parseArgs(getArgs(String.format("--%s %s", argName, expectedArg.toString()))));
    assertEquals(String.format("Not expected arg %s", argName), expectedArg, cli.getPlan());

    SyncEnum expectedFile = SyncEnum.Remote;
    assertNotEquals("Make sure they are different", expectedArg, expectedFile);

    File file = File.createTempFile(getClass().getSimpleName(), ".properties");
    file.deleteOnExit();
    PrintWriter writer = new PrintWriter(file);
    try {
      writer.println(String.format("%s=%s", argName, expectedFile.name()));
    } finally {
      writer.close();
    }

    assertTrue("Should have no args",
        cli.parseArgs(getArgs(String.format("--%s %s", CommandLineUtils.PROPS_ARG, file.getAbsolutePath()))));
    assertEquals(String.format("Not expected file %s", argName), expectedFile, cli.getPlan());

    // Args take precedent over properties
    assertTrue("Should have required arg", cli.parseArgs(getArgs(String.format("--%s %s --%s %s", CommandLineUtils.PROPS_ARG,
        file.getAbsolutePath(), argName, expectedArg.toString()))));
    assertEquals(String.format("Not expected arg with file %s", argName), expectedArg, cli.getPlan());

    SyncEnum expectedProp = SyncEnum.Local;
    assertNotEquals("Make sure they are different", expectedArg, expectedProp);
    assertNotEquals("Make sure they are different", expectedFile, expectedProp);

    System.setProperty(argName, expectedProp.name());

    // System properties take precedent over file properties
    assertTrue("Should have no args",
        cli.parseArgs(getArgs(String.format("--%s %s", CommandLineUtils.PROPS_ARG, file.getAbsolutePath()))));
    assertEquals(String.format("Not expected prop %s", argName), expectedProp, cli.getPlan());
  }

  @Test
  public void testRequiredArg() throws Exception {
    String argName = CommandLineUtils.SYNC_ARG;
    CommandLineUtils cli = new CommandLineUtils(getClass());
    cli.useSync();

    assertFalse("Should have required arg", cli.parseArgs(getArgs(String.format("--%s", argName))));
    assertFalse(String.format("Should have %s", argName), cli.hasSync());

    SyncEnum expected = SyncEnum.Both;
    assertTrue("Should have required arg", cli.parseArgs(getArgs(String.format("--%s %s", argName, expected.toString()))));
    assertTrue(String.format("Should have %s", argName), cli.hasSync());
    assertNotNull(String.format("Should get %s", argName), cli.getSync());
    assertEquals(String.format("Not expected %s", argName), expected, cli.getSync());
  }

  @Test
  public void testUse() throws Exception {
    String argName = CommandLineUtils.ALLOW_WRITE_ARG;
    CommandLineUtils cli = new CommandLineUtils(getClass());

    assertFalse("Was able to parse args", cli.parseArgs(getArgs(String.format("--%s", argName))));

    cli.useAllow();
    assertTrue("Not able to parse args", cli.parseArgs(getArgs(String.format("--%s", argName))));
    assertTrue(String.format("Should have %s", argName), cli.hasAllowWrite());
  }

}