package com.wstrater.server.fileSync.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import com.wstrater.server.fileSync.common.data.DirectoryInfo;
import com.wstrater.server.fileSync.common.data.DirectoryListRequest;
import com.wstrater.server.fileSync.common.data.DirectoryListResponse;
import com.wstrater.server.fileSync.common.data.FileInfo;
import com.wstrater.server.fileSync.common.utils.Constants.SyncEnum;
import com.wstrater.server.fileSync.common.utils.DirectoryUtils;
import com.wstrater.server.fileSync.common.utils.FileUtils;

public class HasherTest extends AbstractPlanMapTest {

  private static final String TEST_CASE_FILE = "hasherTestCases.csv";

  /**
   * Set up the remote and local directories in the temporary file space.
   */
  @Before
  public void before() {
    File tempDir = new File(System.getProperty("java.io.tmpdir"));

    simpleTest = false;

    localBaseDir = FileUtils.canonicalFile(new File(tempDir, "hashTest/local"));
    localBaseDir.mkdirs();
    logger.info(String.format("Local Base Dir: %s", localBaseDir.getAbsolutePath()));

    remoteBaseDir = FileUtils.canonicalFile(new File(tempDir, "hashTest/remote"));
    remoteBaseDir.mkdirs();
    logger.info(String.format("Remote Base Dir: %s", remoteBaseDir.getAbsolutePath()));
  }

  @Test
  public void testHasher() throws Exception {
    loadTestCases(TEST_CASE_FILE);

    try {
      for (TestCase testCase : testCases) {
        cleanUpTest();

        setupTestCase(testCase);

        dumpDirectories();

        SyncEnum hash = testCase.getTestKey().getSync();

        Hasher hasher = Hasher.builder().localBaseDir(localBaseDir).remoteBaseDir(remoteBaseDir).hiddenDirectories(true)
            .hiddenFiles(true).recursive(true).rehash(true).build();
        hasher.hash(hash, ".");

        if (hash.syncRemoteToLocal()) {
          verifyHash(localBaseDir);
        }
        if (hash.syncLocalToRemote()) {
          verifyHash(remoteBaseDir);
        }
      }
    } finally {
      cleanUpTest();
    }
  }

  private void verifyDirectoryInfo(File baseDir, DirectoryInfo directoryInfo, String path) {
    File dir = FileUtils.canonicalFile(path == null ? baseDir : new File(baseDir, path));
    logger.debug(String.format("Verifing Directory: %s", dir.getAbsolutePath()));
    assertTrue(String.format("Missing expected directory: %s", dir.getAbsolutePath()), dir.isDirectory());

    if (directoryInfo != null) {
      for (FileInfo fileInfo : directoryInfo.getFiles()) {
        String fileName = path == null ? String.format("/%s", fileInfo.getName()) : String
            .format("%s/%s", path, fileInfo.getName());
        File file = FileUtils.canonicalFile(new File(baseDir, fileName));
        logger.debug(String.format("Verifing File: %s", file.getAbsolutePath()));
        assertTrue(String.format("Missing expected file: %s", file.getAbsolutePath()), file.isFile());

        assertNotNull(String.format("Missing hash: %s", file.getAbsolutePath(), fileInfo.getHash()));
      }

      for (DirectoryInfo childInfo : directoryInfo.getDirectories()) {
        String newPath = path == null ? childInfo.getName() : String.format("%s/%s", path, childInfo.getName());
        verifyDirectoryInfo(baseDir, childInfo, newPath);
      }
    }
  }

  private void verifyHash(File baseDir) {
    DirectoryListRequest listRequest = new DirectoryListRequest();
    listRequest.setPath(".");
    listRequest.setHiddenDirectories(true);
    listRequest.setHiddenFiles(true);
    listRequest.setRecursive(true);

    DirectoryInfo directoryInfo = null;
    DirectoryUtils.setBaseDir(baseDir);
    listRequest.setBaseDir(baseDir);
    DirectoryListResponse listResponse = DirectoryUtils.listDirectory(listRequest);
    assertNotNull(String.format("Missing local response from list directory: %s", listRequest.getBaseDir()), listResponse);
    assertNotNull(String.format("Missing local directory info from list directory: %s", listRequest.getBaseDir()),
        listResponse.getDirectoryInfo());
    directoryInfo = listResponse.getDirectoryInfo();
    logger.debug(String.format("Directory Listing: %s", directoryInfo));
    verifyDirectoryInfo(baseDir, directoryInfo, null);
  }

}