package com.wstrater.server.fileSync.client;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wstrater.server.fileSync.common.data.DeleteRequest;
import com.wstrater.server.fileSync.common.data.DeleteResponse;
import com.wstrater.server.fileSync.common.data.DirectoryDeleteRequest;
import com.wstrater.server.fileSync.common.data.DirectoryDeleteResponse;
import com.wstrater.server.fileSync.common.data.DirectoryListRequest;
import com.wstrater.server.fileSync.common.data.DirectoryListResponse;
import com.wstrater.server.fileSync.common.data.DirectoryMakeRequest;
import com.wstrater.server.fileSync.common.data.DirectoryMakeResponse;
import com.wstrater.server.fileSync.common.data.HashRequest;
import com.wstrater.server.fileSync.common.data.HashResponse;
import com.wstrater.server.fileSync.common.data.HashStatus;
import com.wstrater.server.fileSync.common.data.ReadRequest;
import com.wstrater.server.fileSync.common.data.ReadResponse;
import com.wstrater.server.fileSync.common.data.WriteRequest;
import com.wstrater.server.fileSync.common.data.WriteResponse;
import com.wstrater.server.fileSync.common.exceptions.ErrorDeletingDirectoryException;
import com.wstrater.server.fileSync.common.exceptions.ErrorDeletingFileException;
import com.wstrater.server.fileSync.common.exceptions.ErrorHashingDirectoryException;
import com.wstrater.server.fileSync.common.exceptions.ErrorListingDirectoryException;
import com.wstrater.server.fileSync.common.exceptions.ErrorMakingDirectoryException;
import com.wstrater.server.fileSync.common.exceptions.ErrorReadingBlockException;
import com.wstrater.server.fileSync.common.exceptions.ErrorWritingBlockException;
import com.wstrater.server.fileSync.common.exceptions.InvalidBlockHashException;
import com.wstrater.server.fileSync.common.file.BlockReader;
import com.wstrater.server.fileSync.common.file.BlockReaderLocalImpl;
import com.wstrater.server.fileSync.common.file.BlockWriter;
import com.wstrater.server.fileSync.common.file.BlockWriterLocalImpl;
import com.wstrater.server.fileSync.common.file.DirectoryLister;
import com.wstrater.server.fileSync.common.file.HashRequester;
import com.wstrater.server.fileSync.common.utils.CommandLineUtils;
import com.wstrater.server.fileSync.common.utils.FileUtils;

/**
 * Simple program to test {@link FileSyncServer} running on the same local machine.
 * 
 * @author wstrater
 *
 */
public class RemoteClientTest {

  private final static String REMOTE_PATH = "wes";

  protected final Logger      logger      = LoggerFactory.getLogger(getClass());

  // private byte data[];
  private CommandLineUtils    cli;
  // private long crc32;
  private String              fileName    = REMOTE_PATH + "/strater.dat";
  private int                 blockSize;
  private Random              rand        = new Random();

  private File createTestFile() throws IOException {
    File ret = File.createTempFile(getClass().getSimpleName() + "_", ".dat");

    ret.deleteOnExit();

    return ret;
  }

  private File createTestFile(int length, int offset) throws IOException {
    File ret = createTestFile();

    byte[] data = new byte[length];
    rand.nextBytes(data);

    long crc32 = FileUtils.calcCRC(data);

    File baseDir = ret.getParentFile();
    String fileName = ret.getName();

    WriteRequest request = new WriteRequest();
    request.setBaseDir(baseDir);
    request.setFileName(fileName);
    request.setData(data);
    request.setOffset(offset);
    request.setLength(length);

    WriteResponse response = FileUtils.writeBlock(request);
    if (!response.isSuccess() || crc32 != response.getCrc32()) {
      throw new IOException(String.format("Failed creating temporary file: ", ret.getAbsolutePath()));
    }

    return ret;
  }

  protected String getURI(String path) {
    String ret = String.format("%s://%s:%d%s", cli.hasSsl() ? "https" : "http", cli.getHost(), cli.getPort(), path);

    logger.info(String.format("URI: %s", ret));

    return ret;
  }

  private void run(String[] args) {
    try {
      cli = new CommandLineUtils(getClass());
      cli.useAllow().useBaseDir().useClient().useSync().useTimeZone();

      if (cli.parseArgs(args)) {
        if (cli.isHelp()) {
          cli.displayHelp();
        } else {
          blockSize = FileUtils.MIN_BLOCK_SIZE;

          RemoteClient remoteClient = RemoteClient.builder().ssl(cli.hasSsl()).host(cli.getHost()).port(cli.getPort()).build();
          try {
            BlockReader localReader = new BlockReaderLocalImpl();
            BlockWriter localWriter = new BlockWriterLocalImpl();
            BlockReader remoteReader = new BlockReaderRemoteImpl(remoteClient);
            BlockWriter remoteWriter = new BlockWriterRemoteImpl(remoteClient);
            HashRequester remoteHasher = new HashRequesterRemoteImpl(remoteClient);
            DirectoryLister remoteLister = new DirectoryListerRemoteImpl(remoteClient);

            testMakeDirectory(remoteLister, REMOTE_PATH);

            for (int xx = 0; xx < 5; xx++) {
              File readFile = createTestFile(blockSize + rand.nextInt(blockSize * 5), 0);

              testCopyFile(localReader, readFile.getName(), remoteWriter, fileName, readFile.getParentFile());

              testHash(remoteHasher, REMOTE_PATH);

              File writeFile = createTestFile();

              testCopyFile(remoteReader, fileName, localWriter, writeFile.getName(), writeFile.getParentFile());

              assertTrue("Files different after copy local", FileUtils.compareFiles(writeFile, readFile));

              testListDirectory(remoteLister, REMOTE_PATH);

              testDeleteFile(remoteWriter, fileName, writeFile.getParentFile());

              readFile.delete();
              writeFile.delete();
            }

            testDeleteDirectory(remoteLister, REMOTE_PATH);
          } finally {
            remoteClient.finished();
          }
        }
      }
    } catch (Throwable ee) {
      logger.error(String.format("Error running test: %s", ee.getMessage()), ee);
    }
  }

  private void testCopyFile(BlockReader reader, String readFileName, BlockWriter writer, String writeFileName, File baseDir)
      throws IOException {
    ReadRequest readRequest = new ReadRequest();
    readRequest.setBaseDir(baseDir);
    readRequest.setFileName(readFileName);
    readRequest.setBlockSize(FileUtils.MIN_BLOCK_SIZE);

    WriteRequest writeRequest = new WriteRequest();
    writeRequest.setBaseDir(baseDir);
    writeRequest.setFileName(writeFileName);

    long offset = 0L;
    while (true) {
      readRequest.setOffset(offset);
      logger.info(String.format("Read Request Copy: %s", readRequest));
      ReadResponse readResponse = reader.readBlock(readRequest);
      logger.info(String.format("Read Response Copy: %s", readResponse));
      if (!readResponse.isSuccess()) {
        throw new ErrorReadingBlockException(String.format("Error reading %d bytes '%s' in '%s' at offset of %d",
            readRequest.getBlockSize(), readRequest.getFileName(), readRequest.getBaseDir(), readRequest.getOffset()));
      }

      if (readResponse.getLength() > 0 || readResponse.isEof()) {
        writeRequest.setData(readResponse.getData());
        writeRequest.setOffset(offset);
        writeRequest.setLength(readResponse.getLength());
        writeRequest.setEof(readResponse.isEof());
        logger.info(String.format("Write Request Copy: %s", writeRequest));
        WriteResponse writeResponse = writer.writeBlock(writeRequest);
        logger.info(String.format("Write Response Copy: %s", writeResponse));
        if (!writeResponse.isSuccess()) {
          throw new ErrorWritingBlockException(String.format("Error writing %d bytes '%s' in '%s' at offset of %d",
              writeRequest.getLength(), writeRequest.getFileName(), writeRequest.getBaseDir(), writeRequest.getOffset()));
        }

        if (writeRequest.getLength() != writeResponse.getLength()) {
          throw new InvalidBlockHashException(String.format(
              "Invalid length while writing what was expected. Exepecting %d but was %d", writeRequest.getLength(),
              writeResponse.getLength()));
        } else if (readResponse.getLength() != writeResponse.getLength()) {
          throw new InvalidBlockHashException(String.format("Invalid length while writing what was read. Exepecting %d but was %d",
              readResponse.getLength(), writeResponse.getLength()));
        }

        if (readResponse.getCrc32() != writeResponse.getCrc32()) {
          throw new InvalidBlockHashException(String.format("Invalid CRC while writing what was read. Exepecting %d but was %d",
              readResponse.getCrc32(), writeResponse.getCrc32()));
        }

        offset += readResponse.getLength();
      }

      if (readResponse.isEof()) {
        break;
      }
    }
  }

  private void testDeleteDirectory(DirectoryLister lister, String path) {
    DirectoryDeleteRequest request = new DirectoryDeleteRequest();
    request.setPath(path);
    request.setFiles(true);
    request.setRecursive(true);

    logger.info(String.format("Delete Directory Request: %s", request));
    DirectoryDeleteResponse response = lister.deleteDirectory(request);
    logger.info(String.format("Delete Directory Response: %s", response));

    if (!response.isSuccess()) {
      throw new ErrorDeletingDirectoryException(String.format("Error deleting directory '%s' in '%s'", request.getPath(),
          request.getBaseDir()));
    }
  }

  private void testDeleteFile(BlockWriter writer, String fileName, File baseDir) throws IOException {
    DeleteRequest deleteRequest = new DeleteRequest();
    deleteRequest.setBaseDir(baseDir);
    deleteRequest.setFileName(fileName);

    logger.info(String.format("Delete Request Copy: %s", deleteRequest));
    DeleteResponse deleteResponse = writer.deleteFile(deleteRequest);
    logger.info(String.format("Delete Response Copy: %s", deleteResponse));

    if (!deleteResponse.isSuccess()) {
      throw new ErrorDeletingFileException(String.format("Error deleting '%s' in '%s'", deleteRequest.getFileName(),
          deleteRequest.getBaseDir()));
    }
  }

  private void testHash(HashRequester hasher, String path) throws IOException {
    HashRequest hashRequest = new HashRequest();
    hashRequest.setPath(path);

    logger.info(String.format("Hash Request: %s", hashRequest));
    HashResponse hashResponse = hasher.hashDirectory(hashRequest);
    logger.info(String.format("Hash Response: %s", hashResponse));

    if (!hashResponse.isQueued()) {
      throw new ErrorDeletingFileException(String.format("Error hashing '%s' in '%s'", hashRequest.getPath(),
          hashRequest.getBaseDir()));
    }

    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(5L));
    } catch (InterruptedException ee) {
    }

    HashStatus hashStatus = hasher.getHashStatus(hashRequest.getId());
    logger.info(String.format("Hash Status: %s", hashStatus));

    if (hashStatus.isFailed()) {
      throw new ErrorHashingDirectoryException(String.format("Hashing failed '%s' in '%s': %s", hashRequest.getPath(),
          hashRequest.getBaseDir(), hashStatus.getFailureMessage()));
    } else if (!hashStatus.isStarted()) {
      throw new ErrorHashingDirectoryException(String.format("Hashing not started '%s' in '%s'", hashRequest.getPath(),
          hashRequest.getBaseDir()));
    }
  }

  private void testListDirectory(DirectoryLister lister, String path) {
    DirectoryListRequest request = new DirectoryListRequest();
    request.setPath(path);

    logger.info(String.format("List Directory Request: %s", request));
    DirectoryListResponse response = lister.listDirectory(request);
    logger.info(String.format("List Directory Response: %s", response));

    if (!response.isSuccess()) {
      throw new ErrorListingDirectoryException(String.format("Error listing directory '%s' in '%s'", request.getPath(),
          request.getBaseDir()));
    }
  }

  private void testMakeDirectory(DirectoryLister lister, String path) {
    DirectoryMakeRequest request = new DirectoryMakeRequest();
    request.setPath(path);

    logger.info(String.format("Make Directory Request: %s", request));
    DirectoryMakeResponse response = lister.makeDirectory(request);
    logger.info(String.format("Make Directory Response: %s", response));

    if (!response.isSuccess()) {
      throw new ErrorMakingDirectoryException(String.format("Error making directory '%s' in '%s'", request.getPath(),
          request.getBaseDir()));
    }
  }

  public static void main(String[] args) {
    RemoteClientTest client = new RemoteClientTest();
    client.run(args);
  }

}