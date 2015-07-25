package com.wstrater.server.fileSync.client;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import com.wstrater.server.fileSync.common.utils.Constants;
import com.wstrater.server.fileSync.common.utils.FilePermissions;
import com.wstrater.server.fileSync.common.utils.FileUtils;

public class PlannerTest extends AbstractPlanMapTest {

  private static final String TEST_CASE_FILE = "plannerTestCases.csv";

  /**
   * Set up the remote and local directories in the temporary file space.
   */
  @Before
  public void before() {
    File tempDir = new File(System.getProperty("java.io.tmpdir"));

    simpleTest = false;

    localBaseDir = FileUtils.canonicalFile(new File(tempDir, "planTest/local"));
    localBaseDir.mkdirs();
    logger.info(String.format("Local Base Dir: %s", localBaseDir.getAbsolutePath()));

    remoteBaseDir = FileUtils.canonicalFile(new File(tempDir, "planTest/remote"));
    remoteBaseDir.mkdirs();
    logger.info(String.format("Remote Base Dir: %s", remoteBaseDir.getAbsolutePath()));
  }

  @Test
  public void testPlanner() throws Exception {
    loadTestCases(TEST_CASE_FILE);

    try {
      for (TestCase testCase : testCases) {
        cleanUpTest();

        setupTestCase(testCase);

        dumpDirectories();

        FilePermissions permissions = testCase.getTestKey().getPermissions();
        FileUtils.setPermissions(permissions);

        String reportName = String.format("planReport_%s_%s%s%s%s", testCase.getTestKey().getSync().name(),
            Constants.tOrF(permissions.isLocalDelete()), Constants.tOrF(permissions.isLocalWrite()),
            Constants.tOrF(permissions.isRemoteDelete()), Constants.tOrF(permissions.isRemoteWrite()));

        for (String[] report : new String[][] {
            { "src/main/resources/planTemplateCSV.jmte", "[EOL]", "build/reports/planTest/" + reportName + ".csv" },
            { "src/main/resources/planTemplateHTML.jmte", null, "build/reports/planTest/" + reportName + ".html" } }) {
          Planner planner = Planner.builder().localBaseDir(localBaseDir).remoteBaseDir(remoteBaseDir).permissions(permissions)
              .templateName(report[0]).recursive(true).eol(report[1]).reportFile(report[2]).build();
          planner.plan(testCase.getTestKey().getSync(), ".");
        }
      }
    } finally {
      cleanUpTest();
    }
  }

}