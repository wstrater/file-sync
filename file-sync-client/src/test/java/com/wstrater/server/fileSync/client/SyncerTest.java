package com.wstrater.server.fileSync.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.zip.CRC32;

import org.junit.Before;
import org.junit.Test;

import com.wstrater.server.fileSync.common.data.DirectoryInfo;
import com.wstrater.server.fileSync.common.data.DirectoryListRequest;
import com.wstrater.server.fileSync.common.data.DirectoryListResponse;
import com.wstrater.server.fileSync.common.data.FileInfo;
import com.wstrater.server.fileSync.common.data.HashRequest;
import com.wstrater.server.fileSync.common.data.HashStatus;
import com.wstrater.server.fileSync.common.file.DirectoryLister;
import com.wstrater.server.fileSync.common.hash.HashProcessor;
import com.wstrater.server.fileSync.common.utils.Compare;
import com.wstrater.server.fileSync.common.utils.Constants.SyncEnum;
import com.wstrater.server.fileSync.common.utils.DirectoryUtils;
import com.wstrater.server.fileSync.common.utils.FilePermissions;
import com.wstrater.server.fileSync.common.utils.FileUtils;

public class SyncerTest extends AbstractPlanMapTest {

  private static final String TEST_CASE_FILE = "syncerTestCases.csv";

  /**
   * Set up the remote and local directories in the temporary file space.
   */
  @Before
  public void before() {
    File tempDir = new File(System.getProperty("java.io.tmpdir"));

    simpleTest = false;

    localBaseDir = FileUtils.canonicalFile(new File(tempDir, "syncTest/local"));
    localBaseDir.mkdirs();
    logger.info(String.format("Local Base Dir: %s", localBaseDir.getAbsolutePath()));

    remoteBaseDir = FileUtils.canonicalFile(new File(tempDir, "syncTest/remote"));
    remoteBaseDir.mkdirs();
    logger.info(String.format("Remote Base Dir: %s", remoteBaseDir.getAbsolutePath()));
  }

  /**
   * This was run as a "test" and used to build the outline of the test case file. The file is best
   * viewed and edited in as a spread sheet. Using filters make adjusting the expected testResult
   * easier.
   */
  public void buildTestCases() {
    System.out.println(String.format("sync,localDelete,localWrite,remoteDelete,remoteWrite,testSetup,testResult"));
    for (SyncEnum sync : SyncEnum.values()) {
      for (boolean localDelete : TrueFalse) {
        for (boolean localWrite : TrueFalse) {
          for (boolean remoteDelete : TrueFalse) {
            for (boolean remoteWrite : TrueFalse) {
              for (TestSetup testSetup : TestSetup.values()) {
                System.out.println(String.format("%s,%b,%b,%b,%b,%s,''", sync.name(), localDelete, localWrite, remoteDelete,
                    remoteWrite, testSetup));
              }
            }
          }
        }
      }
    }
  }

  /**
   * Allow using a local or remote implementation of the "remote" file system for the {@link Syncer}
   * .
   * 
   * @param permissions
   * @return
   */
  protected Syncer newSyncer(FilePermissions permissions) {
    return Syncer.builder().localBaseDir(localBaseDir).remoteBaseDir(remoteBaseDir).permissions(permissions).recursive(true)
        .build();
  }

  /**
   * Run through the unique permutations of {@link TestKey} and {@link TestSetup}. There are
   * currently 288.
   * 
   * @throws Exception
   */
  @Test
  public void testCases() throws Exception {
    loadTestCases(TEST_CASE_FILE);

    try {
      for (TestCase testCase : testCases) {
        cleanUpTest();

        setupTestCase(testCase);

        dumpDirectories();

        FilePermissions permissions = testCase.getTestKey().getPermissions();
        FileUtils.setPermissions(permissions);

        Syncer syncer = newSyncer(permissions);
        syncer.sync(testCase.getTestKey().getSync(), ".");

        dumpDirectories();

        verifyTestCase(testCase);

        verifyDirectoryListings(testCase);
      }
    } finally {
      cleanUpTest();
    }
  }

  /**
   * Verify that every file in the {@link DirectoryInfo} is expected. It is removed from
   * expectedFiles once verified.
   * 
   * @param baseDir
   * @param directoryInfo
   * @param path
   * @param verifyHash
   */
  private void verifyDirectoryInfo(File baseDir, DirectoryInfo directoryInfo, String path, boolean verifyHash) {
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
        String hash = expectedHashes.get(fileName);
        if (hash != null) {
          if (verifyHash) {
            assertEquals(String.format("Invalid hash for %s: %s,%s", file.getAbsolutePath(), hash, fileInfo.getHash()), hash,
                fileInfo.getHash());
            expectedHashes.remove(fileName);
          } else {
            expectedHashes.put(fileName, fileInfo.getHash());
          }
        }

        boolean found = false;
        for (int xx = 0; xx < expectedFiles.size(); xx++) {
          FileDetails details = expectedFiles.get(xx);
          if (Compare.equals(baseDir, details.getBaseDir()) && Compare.equals(fileName, details.getFileName())) {
            found = true;
            expectedFiles.remove(xx);

            assertTrue(
                String.format("Invalid file length for %s: %s,%s", file.getAbsolutePath(), details.getLength(),
                    fileInfo.getLength()), Compare.equals(details.getLength(), fileInfo.getLength()));
            assertTrue(String.format("Invalid file last modified for %s: %s,%s", file.getAbsolutePath(), details.getLastModified(),
                fileInfo.getLastModified()), Compare.equals(details.getLastModified(), fileInfo.getLastModified()));

            break;
          }
        }
        assertTrue(String.format("Missing expected details: %s %s", baseDir.getAbsolutePath(), fileName), found);
      }

      for (DirectoryInfo childInfo : directoryInfo.getDirectories()) {
        String newPath = path == null ? childInfo.getName() : String.format("%s/%s", path, childInfo.getName());
        verifyDirectoryInfo(baseDir, childInfo, newPath, verifyHash);
      }
    }
  }

  /**
   * Do a {@link DirectoryLister#listDirectory(DirectoryListRequest)} on the local and remote
   * directories and compare them to the expected files.
   * 
   * @param testCase
   * @throws ExecutionException
   * @throws InterruptedException
   */
  private void verifyDirectoryListings(TestCase testCase) throws InterruptedException, ExecutionException {
    // Remove the files expected to be deleted.
    for (int xx = expectedFiles.size() - 1; xx >= 0; xx--) {
      FileDetails details = expectedFiles.get(xx);
      if (!details.isExpecting()) {
        expectedFiles.remove(xx);
      }
    }

    HashRequest hashRequest = new HashRequest();
    hashRequest.setHiddenDirectories(true);
    hashRequest.setHiddenFiles(true);
    hashRequest.setPath(".");
    hashRequest.setRecursive(true);
    hashRequest.setReHashExisting(false);

    DirectoryListRequest listRequest = new DirectoryListRequest();
    listRequest.setPath(".");
    listRequest.setHiddenDirectories(true);
    listRequest.setHiddenFiles(true);
    listRequest.setRecursive(true);

    hashRequest.setBaseDir(localBaseDir);
    Future<HashStatus> hashFuture = HashProcessor.queueRequest(hashRequest);
    assertNotNull(String.format("Missing local future from hash directory: %s", listRequest.getBaseDir()), hashFuture);
    HashStatus hashStatus = hashFuture.get();
    assertNotNull(String.format("Missing local response from hash directory: %s", listRequest.getBaseDir()), hashStatus);

    DirectoryInfo localInfo = null;
    DirectoryUtils.setBaseDir(localBaseDir);
    listRequest.setBaseDir(localBaseDir);
    DirectoryListResponse listResponse = DirectoryUtils.listDirectory(listRequest);
    assertNotNull(String.format("Missing local response from list directory: %s", listRequest.getBaseDir()), listResponse);
    assertNotNull(String.format("Missing local directory info from list directory: %s", listRequest.getBaseDir()),
        listResponse.getDirectoryInfo());
    localInfo = listResponse.getDirectoryInfo();
    logger.debug(String.format("Local Listing: %s", localInfo));
    verifyDirectoryInfo(localBaseDir, localInfo, null, false);

    hashRequest.setBaseDir(remoteBaseDir);
    hashFuture = HashProcessor.queueRequest(hashRequest);
    assertNotNull(String.format("Missing remote future from hash directory: %s", listRequest.getBaseDir()), hashFuture);
    hashStatus = hashFuture.get();
    assertNotNull(String.format("Missing remote response from hash directory: %s", listRequest.getBaseDir()), hashStatus);

    DirectoryInfo remoteInfo = null;
    DirectoryUtils.setBaseDir(remoteBaseDir);
    listRequest.setBaseDir(remoteBaseDir);
    listResponse = DirectoryUtils.listDirectory(listRequest);
    assertNotNull(String.format("Missing remote response from list directory: %s", listRequest.getBaseDir()), listResponse);
    assertNotNull(String.format("Missing remote directory info from list directory: %s", listRequest.getBaseDir()),
        listResponse.getDirectoryInfo());
    remoteInfo = listResponse.getDirectoryInfo();
    logger.debug(String.format("Remote Listing: %s", remoteInfo));
    verifyDirectoryInfo(remoteBaseDir, remoteInfo, null, true);

    assertTrue("Not all expected files found in directory listings", expectedFiles.isEmpty());
    assertTrue("Not all expected hashes found in directory listings", expectedHashes.isEmpty());
  }

  private void verifyTestCase(TestCase testCase) throws IOException {
    logger.debug(String.format("Verify Test Case: %s", testCase));

    for (FileDetails details : expectedFiles) {
      verifyTestFile(testCase, details);
    }
  }

  private void verifyTestFile(TestCase testCase, FileDetails details) throws IOException {
    File file = new File(details.getBaseDir(), details.getFileName());
    String msg = String.format("testKey: %s, exists: %s, %s", testCase.getTestKey(), file.exists(), details);
    if (details.isExpecting()) {
      if (file.exists()) {
        logger.debug(String.format("Expected Found %s", msg));
      } else {
        logger.error(String.format("Missing Exptected File %s", msg));
      }
      assertTrue(String.format("Missing file %s", msg), file.exists());
      assertEquals(String.format("Invalid file length %s", msg), details.getLength(), file.length());
      DateFormat fmt = new SimpleDateFormat(TIME_FORMAT);
      assertEquals(
          String.format("Invalid file last modified %s (%s vs %s)", msg, fmt.format(new Date(details.getLastModified())),
              fmt.format(new Date(file.lastModified()))), details.getLastModified(), file.lastModified());
    } else {
      if (file.exists()) {
        logger.error(String.format("Found Unexpected File %s", msg));
      } else {
        logger.debug(String.format("Expected Missing %s", msg));
      }
      assertFalse(String.format("Found file %s", msg), file.exists());
    }

    if (file.exists()) {
      byte[] content = new byte[(int) file.length()];
      CRC32 crc32 = new CRC32();
      InputStream in = new FileInputStream(file);
      try {
        int len = 0;
        while ((len = in.read(content)) >= 0) {
          crc32.update(content, 0, len);
        }
      } finally {
        in.close();
      }
      assertEquals(String.format("Invalid CRC: %s", msg), details.getCrc(), crc32.getValue());
    }
  }

}