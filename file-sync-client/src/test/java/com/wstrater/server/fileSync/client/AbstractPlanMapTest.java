package com.wstrater.server.fileSync.client;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wstrater.server.fileSync.common.data.DirectoryDeleteRequest;
import com.wstrater.server.fileSync.common.data.DirectoryListRequest;
import com.wstrater.server.fileSync.common.data.DirectoryListResponse;
import com.wstrater.server.fileSync.common.utils.Constants;
import com.wstrater.server.fileSync.common.utils.Constants.SyncEnum;
import com.wstrater.server.fileSync.common.utils.Compare;
import com.wstrater.server.fileSync.common.utils.Converter;
import com.wstrater.server.fileSync.common.utils.DirectoryUtils;
import com.wstrater.server.fileSync.common.utils.FilePermissions;
import com.wstrater.server.fileSync.common.utils.FileUtils;

/**
 * This abstract class is used to set up a local and remote file system for a {@link TestCase} using
 * a list of {@link TestSetup}. It updates the <code>expectedFiles</code> and
 * <code>expectedHashes</code> based on the {@link TestResult} for each{@link TestSetup}.
 * 
 * @author wstrater
 *
 */
public abstract class AbstractPlanMapTest {

  protected enum TestSetup {
    LocalOnly(true, false), RemoteOnly(false, true), Same(false, false, 0L), NewerLocal(true, false, 5L), NewerRemote(true, false,
        -5L), Different(true, true, 0L);

    private boolean localFile;
    private boolean remoteFile;
    private boolean differentContent;
    private boolean differentSize;
    private long    localTimeOffset;

    private TestSetup(boolean localFile, boolean remoteFile) {
      this.localFile = localFile;
      this.remoteFile = remoteFile;
      this.differentContent = false;
      this.differentSize = false;
      this.localTimeOffset = 0L;
    }

    private TestSetup(boolean differentContent, boolean differentSize, long localTimeOffset) {
      this.localFile = true;
      this.remoteFile = true;
      this.differentContent = differentContent;
      this.differentSize = differentSize;
      this.localTimeOffset = localTimeOffset;
    }

    public boolean localFile() {
      return localFile;
    }

    public boolean remoteFile() {
      return remoteFile;
    }

    public boolean differentContent() {
      return differentContent;
    }

    public boolean differentSize() {
      return differentSize;
    }

    public long localTimeOffset() {
      return localTimeOffset;
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();

      builder.append(name()).append(" (LF=").append(Constants.tOrF(localFile)).append(",RF=").append(Constants.tOrF(remoteFile))
          .append(",DC=").append(Constants.tOrF(differentContent)).append(",DS=").append(Constants.tOrF(differentSize))
          .append(",LO=").append(localTimeOffset).append(")");

      return builder.toString();
    }

  }

  protected enum TestResult {
    LocalOnly, DeleteLocal, BothLocal, BothOriginal, BothRemote, DeleteRemote, RemoteOnly;
  }

  protected static final boolean[] TrueFalse      = new boolean[] { true, false };

  private static final String      NO_HASH        = "[NoHash]";
  protected static final String    TIME_FORMAT    = "hh:mm:ss.SSS";

  protected final Logger           logger         = LoggerFactory.getLogger(getClass());
  protected List<FileDetails>      expectedFiles  = new ArrayList<>();
  protected Map<String, String>    expectedHashes = new HashMap<>();
  protected File                   localBaseDir;
  protected Random                 rand           = new Random();
  protected File                   remoteBaseDir;
  /** A simpleTest has only one file per {@link TestSetup} and no sub-directories. */
  protected boolean                simpleTest     = false;
  protected List<TestCase>         testCases      = new ArrayList<>();

  public AbstractPlanMapTest() {
    super();
  }

  private void addExpectedFile(FileDetails details) {
    logger.debug(String.format("Expected File: %s", details));
    expectedFiles.add(details);
  }

  private void addExpectedHash(String fileName) {
    logger.debug(String.format("Expected Hash: %s", fileName));
    expectedHashes.put(fileName, NO_HASH);
  }

  private long calcCRC(byte[] content) {
    long ret = -1;

    if (content != null) {
      CRC32 crc = new CRC32();
      crc.update(content);
      ret = crc.getValue();
    }

    return ret;
  }

  protected void cleanUpTest() {
    DirectoryDeleteRequest request = new DirectoryDeleteRequest();
    request.setPath(".");
    request.setFiles(true);
    request.setRecursive(true);

    // Allow clean up.
    FileUtils.getPermissions().setLocalDelete(true);
    FileUtils.getPermissions().setRemoteDelete(true);

    if (localBaseDir != null) {
      DirectoryUtils.setBaseDir(localBaseDir);
      request.setBaseDir(localBaseDir);
      DirectoryUtils.deleteDirectory(request);
    }

    if (remoteBaseDir != null) {
      DirectoryUtils.setBaseDir(remoteBaseDir);
      request.setBaseDir(remoteBaseDir);
      DirectoryUtils.deleteDirectory(request);
    }
  }

  protected void dumpDirectories() {
    DirectoryListRequest request = new DirectoryListRequest();
    request.setPath(".");
    request.setHiddenDirectories(true);
    request.setHiddenFiles(true);
    request.setRecursive(true);

    if (localBaseDir != null) {
      DirectoryUtils.setBaseDir(localBaseDir);
      request.setBaseDir(localBaseDir);
      DirectoryListResponse response = DirectoryUtils.listDirectory(request);
      logger.debug(String.format("Local Listing: %s", response.getDirectoryInfo()));
    }

    if (remoteBaseDir != null) {
      DirectoryUtils.setBaseDir(remoteBaseDir);
      request.setBaseDir(remoteBaseDir);
      DirectoryListResponse response = DirectoryUtils.listDirectory(request);
      logger.debug(String.format("Remote Listing: %s", response.getDirectoryInfo()));
    }
  }

  protected void loadTestCases(String fileName) throws Exception {
    testCases.clear();
    InputStream in = getClass().getResourceAsStream(fileName);
    if (in == null) {
      File file = new File(String.format("src/test/resources/%s", fileName));
      if (file.canRead()) {
        in = new FileInputStream(file);
      }
    }
    assertNotNull(String.format("Missing test case file: %s", fileName), in);
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    try {
      TestCase currentTestCase = null;
      String line;
      while ((line = reader.readLine()) != null) {
        String[] fields = line.split("[,]");
        if (fields != null && fields.length == 7 && Compare.isNotBlank(fields[0])) {
          int index = 0;
          SyncEnum sync = SyncEnum.parseSync(fields[index++], null);
          if (sync != null) {
            boolean localDelete = Converter.parseBoolean(fields[index++]);
            boolean localWrite = Converter.parseBoolean(fields[index++]);
            boolean remoteDelete = Converter.parseBoolean(fields[index++]);
            boolean remoteWrite = Converter.parseBoolean(fields[index++]);
            TestKey testKey = new TestKey(sync, localDelete, localWrite, remoteDelete, remoteWrite);
            if (currentTestCase == null || !currentTestCase.getTestKey().equals(testKey)) {
              currentTestCase = new TestCase(testKey);
              testCases.add(currentTestCase);
            }

            TestSetup testSetup = TestSetup.valueOf(fields[index++]);
            TestResult testResult = TestResult.valueOf(fields[index++]);
            currentTestCase.addTestResult(testSetup, testResult);
          }
        }
      }
    } finally {
      reader.close();
    }
  }

  protected void setupTestCase(TestCase testCase) throws IOException {
    logger.debug(String.format("Setup Test Case: %s", testCase));

    expectedFiles.clear();
    expectedHashes.clear();

    for (Entry<TestSetup, TestResult> entry : testCase.getTestResults().entrySet()) {
      setupTestSetup(entry.getKey(), entry.getValue());
    }
  }

  private long setupTestContent(File file, byte[] content, long timeStamp) throws IOException {
    long ret = 0L;

    file.getParentFile().mkdirs();

    OutputStream out = new FileOutputStream(file);
    try {
      out.write(content);
    } finally {
      out.flush();
      out.close();
    }

    file.setLastModified(timeStamp);

    ret = calcCRC(content);

    DateFormat fmt = new SimpleDateFormat(TIME_FORMAT);
    logger.debug(String.format("Setup Test File: %s %d %d %s", file.getAbsolutePath(), content.length, ret,
        fmt.format(new Date(timeStamp))));

    return ret;
  }

  private void setupTestFile(TestSetup testSetup, TestResult testResult, File localBaseDir, File remoteBaseDir, String path)
      throws IOException {
    String fileName = String.format("%s/sycnerTest_%s_%d.tmp", path, testSetup.name(), 100000 + rand.nextInt(899999));

    long localTimeStamp = System.currentTimeMillis();
    long remoteTimeStamp = localTimeStamp;
    localTimeStamp += TimeUnit.MINUTES.toMillis(testSetup.localTimeOffset());

    File localFile = null;
    byte[] localContent = new byte[FileUtils.MIN_BLOCK_SIZE + rand.nextInt(3 * FileUtils.MIN_BLOCK_SIZE)];
    rand.nextBytes(localContent);
    long localCRC = 0L;

    if (testSetup.localFile()) {
      localFile = new File(localBaseDir, fileName);
      localCRC = setupTestContent(localFile, localContent, localTimeStamp);
    }

    File remoteFile = null;
    byte[] remoteContent = localContent;
    long remoteCRC = localCRC;
    if (testSetup.differentContent()) {
      if (testSetup.differentSize()) {
        remoteContent = new byte[FileUtils.MIN_BLOCK_SIZE + rand.nextInt(3 * FileUtils.MIN_BLOCK_SIZE)];
      }
      rand.nextBytes(remoteContent);
      remoteCRC = calcCRC(remoteContent);
    }

    if (testSetup.remoteFile()) {
      remoteFile = new File(remoteBaseDir, fileName);
      remoteCRC = setupTestContent(remoteFile, remoteContent, remoteTimeStamp);
    }

    switch (testResult) {
      case LocalOnly: {
        addExpectedFile(new FileDetails(testSetup, testResult, localBaseDir, fileName, localContent.length, localTimeStamp,
            localCRC, true));
        break;
      }
      case DeleteLocal: {
        addExpectedFile(new FileDetails(testSetup, testResult, localBaseDir, fileName, localContent.length, localTimeStamp,
            localCRC, false));
        break;
      }
      case BothLocal: {
        addExpectedFile(new FileDetails(testSetup, testResult, localBaseDir, fileName, localContent.length, localTimeStamp,
            localCRC, true));
        addExpectedFile(new FileDetails(testSetup, testResult, remoteBaseDir, fileName, localContent.length, localTimeStamp,
            localCRC, true));
        addExpectedHash(fileName);
        break;
      }
      case BothOriginal: {
        addExpectedFile(new FileDetails(testSetup, testResult, localBaseDir, fileName, localContent.length, localTimeStamp,
            localCRC, true));
        addExpectedFile(new FileDetails(testSetup, testResult, remoteBaseDir, fileName, remoteContent.length, remoteTimeStamp,
            remoteCRC, true));
        break;
      }
      case BothRemote: {
        addExpectedFile(new FileDetails(testSetup, testResult, localBaseDir, fileName, remoteContent.length, remoteTimeStamp,
            remoteCRC, true));
        addExpectedFile(new FileDetails(testSetup, testResult, remoteBaseDir, fileName, remoteContent.length, remoteTimeStamp,
            remoteCRC, true));
        addExpectedHash(fileName);
        break;
      }
      case DeleteRemote: {
        addExpectedFile(new FileDetails(testSetup, testResult, remoteBaseDir, fileName, remoteContent.length, remoteTimeStamp,
            remoteCRC, false));
        break;
      }
      case RemoteOnly: {
        addExpectedFile(new FileDetails(testSetup, testResult, remoteBaseDir, fileName, remoteContent.length, remoteTimeStamp,
            remoteCRC, true));
        break;
      }
    }
  }

  private void setupTestSetup(TestSetup testSetup, TestResult testResult) throws IOException {
    logger.debug(String.format("Setup Test Setup: testSetup: %s, testResult: %s", testSetup, testResult));

    String path = "";
    setupTestFile(testSetup, testResult, localBaseDir, remoteBaseDir, path);
    if (!simpleTest) {
      setupTestFile(testSetup, testResult, localBaseDir, remoteBaseDir, path);
      path = String.format("%06d", rand.nextInt(999999));
      setupTestFile(testSetup, testResult, localBaseDir, remoteBaseDir, path);
      setupTestFile(testSetup, testResult, localBaseDir, remoteBaseDir, path);
      path = String.format("%s/%06d", path, rand.nextInt(999999));
      setupTestFile(testSetup, testResult, localBaseDir, remoteBaseDir, path);
      setupTestFile(testSetup, testResult, localBaseDir, remoteBaseDir, path);
      path = String.format("%06d", rand.nextInt(999999));
      setupTestFile(testSetup, testResult, localBaseDir, remoteBaseDir, path);
      setupTestFile(testSetup, testResult, localBaseDir, remoteBaseDir, path);
    }
  }

  protected class FileDetails {

    private File       baseDir;
    private long       crc;
    private boolean    expecting;
    private String     fileName;
    private long       lastModified;
    private long       length;
    private TestResult testResult;
    private TestSetup  testSetup;

    public FileDetails(TestSetup testSetup, TestResult testResult, File baseDir, String fileName, long length, long lastModified,
        long crc, boolean expecting) {
      super();
      this.testSetup = testSetup;
      this.testResult = testResult;
      this.baseDir = baseDir;
      this.fileName = fileName;
      this.length = length;
      this.lastModified = lastModified;
      this.crc = crc;
      this.expecting = expecting;
    }

    public File getBaseDir() {
      return baseDir;
    }

    public long getCrc() {
      return crc;
    }

    public boolean isExpecting() {
      return expecting;
    }

    public String getFileName() {
      return fileName;
    }

    public long getLength() {
      return length;
    }

    public long getLastModified() {
      return lastModified;
    }

    public TestResult getTestResult() {
      return testResult;
    }

    public TestSetup getTestSetup() {
      return testSetup;
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();

      DateFormat fmt = new SimpleDateFormat(TIME_FORMAT);

      builder.append("FileDetails [");
      if (baseDir != null)
        builder.append("baseDir=").append(baseDir).append(", ");
      if (fileName != null)
        builder.append("fileName=").append(fileName).append(", ");
      builder.append("lastModified=").append(fmt.format(new Date(lastModified))).append(", length=").append(length)
          .append(", crc=").append(crc).append(", expecting=").append(expecting);
      if (testSetup != null)
        builder.append(", ").append("testSetup=").append(testSetup);
      if (testResult != null)
        builder.append(", ").append("testResult=").append(testResult);
      builder.append("]");

      return builder.toString();
    }

  }

  protected class TestCase {

    private TestKey                    testKey;
    private Map<TestSetup, TestResult> testResults = new HashMap<>();

    public TestCase(TestKey testKey) {
      super();
      this.testKey = testKey;
    }

    public void addTestResult(TestSetup testSetup, TestResult testResult) {
      testResults.put(testSetup, testResult);
    }

    public TestKey getTestKey() {
      return testKey;
    }

    public Map<TestSetup, TestResult> getTestResults() {
      return testResults;
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();

      builder.append("TestCase [");
      if (testKey != null)
        builder.append("testKey=").append(testKey).append(", ");
      if (testResults != null)
        builder.append("testResults=").append(testResults);
      builder.append("]");

      return builder.toString();
    }

  }

  protected class TestKey {

    private boolean  localDelete;
    private boolean  localWrite;
    private boolean  remoteDelete;
    private boolean  remoteWrite;
    private SyncEnum sync;

    public TestKey(SyncEnum sync, boolean localDelete, boolean localWrite, boolean remoteDelete, boolean remoteWrite) {
      super();
      this.sync = sync;
      this.localDelete = localDelete;
      this.localWrite = localWrite;
      this.remoteDelete = remoteDelete;
      this.remoteWrite = remoteWrite;
    }

    public TestKey(SyncEnum sync, FilePermissions permissions) {
      this(sync, permissions.isLocalDelete(), permissions.isLocalWrite(), permissions.isRemoteDelete(), permissions.isRemoteWrite());
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      TestKey other = (TestKey) obj;
      if (localDelete != other.localDelete)
        return false;
      if (localWrite != other.localWrite)
        return false;
      if (remoteDelete != other.remoteDelete)
        return false;
      if (remoteWrite != other.remoteWrite)
        return false;
      if (sync != other.sync)
        return false;
      return true;
    }

    public boolean isLocalDelete() {
      return localDelete;
    }

    public boolean isLocalWrite() {
      return localWrite;
    }

    public FilePermissions getPermissions() {
      return new FilePermissions(localDelete, localWrite, remoteDelete, remoteWrite);
    }

    public boolean isRemoteDelete() {
      return remoteDelete;
    }

    public boolean isRemoteWrite() {
      return remoteWrite;
    }

    public SyncEnum getSync() {
      return sync;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (localDelete ? 1231 : 1237);
      result = prime * result + (localWrite ? 1231 : 1237);
      result = prime * result + (remoteDelete ? 1231 : 1237);
      result = prime * result + (remoteWrite ? 1231 : 1237);
      result = prime * result + ((sync == null) ? 0 : sync.hashCode());
      return result;
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();

      builder.append("(");
      if (sync != null)
        builder.append(sync.name()).append(",");
      builder.append("LD=").append(Constants.tOrF(localDelete)).append(",LW=").append(Constants.tOrF(localWrite)).append(",RD=")
          .append(Constants.tOrF(remoteDelete)).append(",RW=").append(Constants.tOrF(remoteWrite)).append(")");

      return builder.toString();
    }

  }

}