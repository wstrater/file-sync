package com.wstrater.server.fileSync.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.CRC32;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wstrater.server.fileSync.common.data.ReadRequest;
import com.wstrater.server.fileSync.common.data.ReadResponse;
import com.wstrater.server.fileSync.common.data.WriteRequest;
import com.wstrater.server.fileSync.common.data.WriteResponse;
import com.wstrater.server.fileSync.common.utils.Compare;
import com.wstrater.server.fileSync.common.utils.FileUtils;

public class FileUtilsTest {

  private final static int      BUFFER_SIZE  = 4096;
  private final static int      BUFFER_COUNT = 8;

  protected final static Logger logger       = LoggerFactory.getLogger(FileUtilsTest.class);

  private long[]                crc32s       = new long[BUFFER_COUNT];
  private byte[]                data         = new byte[BUFFER_SIZE * BUFFER_COUNT];
  private boolean[]             flags        = new boolean[BUFFER_COUNT];
  private Random                rand         = new Random();

  private File createFile() throws IOException {
    File ret = File.createTempFile(getClass().getSimpleName() + "_", ".dat");

    logger.info(String.format("Creating File: %s", ret.getAbsoluteFile()));

    ret.deleteOnExit();

    return ret;
  }

  private void initializBuffer() {
    rand.nextBytes(data);

    CRC32 crc = new CRC32();

    for (int xx = 0, offset = 0; xx < BUFFER_COUNT; xx++, offset += BUFFER_SIZE) {
      crc.reset();
      crc.update(data, offset, BUFFER_SIZE);
      crc32s[xx] = crc.getValue();
    }
  }

  /**
   * Verify the entire file.
   * 
   * @param file
   * @throws IOException
   */
  private void verifyFile(File file) throws IOException {
    FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
    ByteBuffer buffer = ByteBuffer.allocate(data.length);

    int length = 0;
    int count;
    do {
      count = channel.read(buffer);
      length += count;
    } while (length >= 0 && buffer.hasRemaining());

    assertEquals("Did not read full data length", data.length, length);
    assertTrue("File does not match data", Compare.equals(data, 0, buffer.array(), 0, length));
  }

  /**
   * Randomly read blocks.
   * 
   * @param file
   * @throws IOException
   */
  private void verifyReads(File file) throws IOException {
    Arrays.fill(flags, false);
    int bufferCount = 0;
    while (bufferCount < BUFFER_COUNT) {
      int buffer = rand.nextInt(BUFFER_COUNT);
      if (!flags[buffer]) {
        int offset = buffer * BUFFER_SIZE;

        File baseDir = file.getParentFile();
        String fileName = file.getName();

        ReadRequest request = new ReadRequest();
        request.setBaseDir(baseDir);
        request.setFileName(fileName);
        request.setOffset(offset);
        request.setBlockSize(BUFFER_SIZE);

        ReadResponse response = FileUtils.readBlock(request);
        assertNotNull("Missing response", response);
        assertTrue("Read not successful", response.isSuccess());
        assertEquals("Read wrong length", BUFFER_SIZE, response.getLength());
        assertNotNull("Missing data", response.getData());
        assertEquals("Data wrong length", BUFFER_SIZE, response.getData().length);
        assertTrue("Invalid data", Compare.equals(data, offset, response.getData(), 0, BUFFER_SIZE));
        assertEquals("Invalid CRC32", crc32s[buffer], response.getCrc32());

        flags[buffer] = true;
        bufferCount++;
      }
    }
  }

  /**
   * Randomly write blocks.
   * 
   * @param file
   * @throws IOException
   */
  private void verifyWrites(File file) throws IOException {
    Arrays.fill(flags, false);
    int bufferCount = 0;
    while (bufferCount < BUFFER_COUNT) {
      int buffer = rand.nextInt(BUFFER_COUNT);
      if (!flags[buffer]) {
        int offset = buffer * BUFFER_SIZE;

        File baseDir = file.getParentFile();
        String fileName = file.getName();

        WriteRequest request = new WriteRequest();
        request.setBaseDir(baseDir);
        request.setFileName(fileName);
        request.setData(new byte[BUFFER_SIZE]);
        request.setOffset(offset);
        request.setLength(BUFFER_SIZE);

        System.arraycopy(data, offset, request.getData(), 0, BUFFER_SIZE);

        WriteResponse response = FileUtils.writeBlock(request);
        assertNotNull("Missing response", response);
        assertTrue("Write not successful", response.isSuccess());
        assertEquals("Write wrong length", BUFFER_SIZE, response.getLength());
        assertEquals("Invalid CRC32", crc32s[buffer], response.getCrc32());

        flags[buffer] = true;
        bufferCount++;
      }
    }
  }

  private void writeFile(File file, boolean append) throws IOException {
    OutputStream out = new FileOutputStream(file, append);
    try {
      out.write(data);
    } finally {
      out.close();
    }

  }

  @Test
  public void testCompareFiles() throws IOException {
    initializBuffer();
    File file1 = createFile();
    writeFile(file1, false);

    File file2 = createFile();
    writeFile(file2, false);

    assertTrue("Same content does not compare", FileUtils.compareFiles(file1, file2));

    writeFile(file2, true);

    assertFalse("More content does compare", FileUtils.compareFiles(file1, file2));

    initializBuffer();
    writeFile(file2, false);

    assertFalse("Different content does compare", FileUtils.compareFiles(file1, file2));
    
    file1.delete();
    file2.delete();
  }

  /**
   * Test with a pre-populated file.
   * 
   * @throws Exception
   */
  @Test
  public void testReadWrite() throws Exception {
    initializBuffer();
    File file = createFile();
    writeFile(file, false);

    verifyReads(file);
    verifyWrites(file);
    verifyReads(file);

    verifyFile(file);

    file.delete();
  }

  /**
   * Test with an empty file.
   * 
   * @throws Exception
   */
  @Test
  public void testWriteRead() throws Exception {
    initializBuffer();
    File file = createFile();

    verifyWrites(file);
    verifyReads(file);

    verifyFile(file);

    file.delete();
  }

}