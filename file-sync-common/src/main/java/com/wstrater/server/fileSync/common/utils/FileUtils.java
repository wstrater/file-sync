package com.wstrater.server.fileSync.common.utils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wstrater.server.fileSync.common.data.DeleteRequest;
import com.wstrater.server.fileSync.common.data.DeleteResponse;
import com.wstrater.server.fileSync.common.data.ReadRequest;
import com.wstrater.server.fileSync.common.data.ReadResponse;
import com.wstrater.server.fileSync.common.data.WriteRequest;
import com.wstrater.server.fileSync.common.data.WriteResponse;
import com.wstrater.server.fileSync.common.exceptions.DeleteNotAllowedException;
import com.wstrater.server.fileSync.common.exceptions.ErrorCreatingDirectoryException;
import com.wstrater.server.fileSync.common.exceptions.ErrorCreatingFileException;
import com.wstrater.server.fileSync.common.exceptions.ErrorReadingBlockException;
import com.wstrater.server.fileSync.common.exceptions.ErrorWritingBlockException;
import com.wstrater.server.fileSync.common.exceptions.FileNotFoundException;
import com.wstrater.server.fileSync.common.exceptions.FileNotReadableException;
import com.wstrater.server.fileSync.common.exceptions.FileNotWritableException;
import com.wstrater.server.fileSync.common.exceptions.InvalidBlockSizeException;
import com.wstrater.server.fileSync.common.exceptions.InvalidDataBlockException;
import com.wstrater.server.fileSync.common.exceptions.InvalidDataLengthException;
import com.wstrater.server.fileSync.common.exceptions.InvalidFileLocationException;
import com.wstrater.server.fileSync.common.exceptions.InvalidFileOffsetException;
import com.wstrater.server.fileSync.common.exceptions.MissingBaseDirectoryException;
import com.wstrater.server.fileSync.common.exceptions.MissingFileNameException;
import com.wstrater.server.fileSync.common.exceptions.MissingRequestException;
import com.wstrater.server.fileSync.common.exceptions.WriteNotAllowedException;

public abstract class FileUtils {

  public static final int        MAX_BLOCK_SIZE = 256 * 1024;
  public static final long       MAX_OFFSET     = 64L * 1024L * 1024L * 1024L;
  public static final int        MIN_BLOCK_SIZE = 4096;

  protected final static Logger  logger         = LoggerFactory.getLogger(FileUtils.class);

  private static int             maxBlockSize   = MAX_BLOCK_SIZE;
  private static long            maxOffset      = MAX_OFFSET;
  private static FilePermissions permissions    = new FilePermissions();

  public static long calcCRC(byte[] data) {
    return calcCRC(data, 0, data == null ? 0 : data.length);
  }

  public static long calcCRC(byte[] data, int offset, int length) {
    long ret = -1;

    if (data != null) {
      CRC32 crc = new CRC32();
      crc.update(data, offset, length);
      ret = crc.getValue();
    }

    // logger.info(String.format("CRC: %d [%d,%d] %s", ret, offset, length, Arrays.toString(data)));

    return ret;
  }

  /**
   * Try to return a canonical file. Returns the absolute file if unable to return the canonical
   * file.
   * 
   * @param in
   * @return
   */
  public static File canonicalFile(File in) {
    File ret = in;

    if (in != null) {
      try {
        ret = in.getCanonicalFile();
      } catch (IOException e) {
        ret = in.getAbsoluteFile();
      }
    }

    return ret;
  }

  /**
   * Compare the contents of two files. Files of different length are considered different.
   * 
   * @param file1
   * @param file2
   * @return
   * @throws IOException
   */
  public static boolean compareFiles(File file1, File file2) throws IOException {
    boolean ret = false;

    if (file1 != null && file1.canRead() && file2 != null && file2.canRead()) {
      if (file1.length() == file2.length()) {
        FileChannel channel1 = FileChannel.open(file1.toPath(), StandardOpenOption.READ);
        try {
          FileChannel channel2 = FileChannel.open(file2.toPath(), StandardOpenOption.READ);
          try {
            int bufferSize = MIN_BLOCK_SIZE * 4;
            ByteBuffer buffer1 = ByteBuffer.allocate(bufferSize);
            ByteBuffer buffer2 = ByteBuffer.allocate(bufferSize);

            int count1 = 0;
            int count2 = 0;
            while (!ret) {
              if (count1 >= 0) {
                count1 = channel1.read(buffer1);
              }
              if (count2 >= 0) {
                count2 = channel2.read(buffer2);
              }

              buffer1.flip();
              buffer2.flip();

              if (Compare.equals(buffer1, buffer2)) {
                ret = count1 < 0 && count2 < 0;
                if (count1 < 0 || count2 < 0) {
                  break;
                } else {
                  buffer1.compact();
                  buffer2.compact();
                }
              } else {
                break;
              }
            }
          } finally {
            channel2.close();
          }
        } finally {
          channel1.close();
        }
      }
    }

    return ret;
  }

  public static DeleteResponse deleteFile(DeleteRequest request) {
    DeleteResponse ret = new DeleteResponse();

    ret.setRequest(request);

    File file = validateDeleteRequest(request);

    logger.info(String.format("DeleteFile: %s", file.getAbsolutePath()));

    file.delete();

    ret.setSuccess(!file.exists());

    logger.info(String.format("DeleteFile: %s, Success: %b", file.getAbsolutePath(), ret.isSuccess()));

    return ret;
  }

  public static int getMaxBlockSize() {
    return maxBlockSize < 0 ? Integer.MAX_VALUE : maxBlockSize;
  }

  public static long getMaxOffset() {
    return maxOffset < 0 ? Long.MAX_VALUE : maxOffset;
  }

  public static FilePermissions getPermissions() {
    return permissions;
  }

  /**
   * Read a block of a file.
   * 
   * @param request
   * @return
   * @throws IOException
   */
  public static ReadResponse readBlock(ReadRequest request) {
    ReadResponse ret = new ReadResponse();

    File file = validateReadRequest(request);

    ret.setRequest(request);

    logger.info(String.format("ReadBlock: %s, Offset: %d, Block Size: %s", file.getAbsolutePath(), request.getOffset(),
        request.getBlockSize()));

    if (request.getOffset() > file.length()) {
      ret.setEof(true);
      ret.setSuccess(true);
    } else {
      try {
        FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        try {
          ByteBuffer buffer = ByteBuffer.allocate(request.getBlockSize());

          channel.position(request.getOffset());

          int count = 0;
          int length = 0;
          do {
            count = channel.read(buffer);
            if (count > 0) {
              length += count;
            }
          } while (count >= 0 && buffer.hasRemaining());
          ret.setEof(count < 0);

          // buffer.flip();

          byte[] data = new byte[length];
          System.arraycopy(buffer.array(), 0, data, 0, length);

          ret.setData(data);
          ret.setLength(length);
          ret.setCrc32(calcCRC(data, 0, length));
          ret.setSuccess(true);
        } finally {
          channel.close();
        }
      } catch (IOException ee) {
        throw new ErrorReadingBlockException(ee.getMessage());
      }
    }

    logger.info(String.format("ReadBlock: %s, Length: %s, CRC: %d, EOF: %b, Success: %b", file.getAbsolutePath(), ret.getLength(),
        ret.getCrc32(), ret.isEof(), ret.isSuccess()));

    return ret;
  }

  public static void setMaxBlockSize(int maxBlockSize) {
    FileUtils.maxBlockSize = maxBlockSize;
  }

  /**
   * Set the maximum offset. Setting this too high could allow a poorly coded or malicious call fill
   * up your storage.
   * 
   * @param maxOffset
   */
  public static void setMaxOffset(long maxOffset) {
    FileUtils.maxOffset = maxOffset;
  }

  public static void setPermissions(FilePermissions permissions) {
    if (permissions == null) {
      throw new IllegalStateException(String.format("%s requires a %s", FileUtils.class.getSimpleName(),
          FilePermissions.class.getSimpleName()));
    }
    FileUtils.permissions = permissions;
  }

  private static void validateBlockSize(int blockSize) {
    if (blockSize < MIN_BLOCK_SIZE || blockSize > getMaxBlockSize() || blockSize % MIN_BLOCK_SIZE != 0) {
      throw new InvalidBlockSizeException(String.format("Block size must be between %d and %d and a multiple of %d: %d",
          MIN_BLOCK_SIZE, getMaxBlockSize(), MIN_BLOCK_SIZE, blockSize));
    }
  }

  private static void validateData(byte[] data) {
    if (data == null || data.length > getMaxBlockSize()) {
      throw new InvalidDataBlockException(String.format("Data block must be between %d and %d bytes: %d", 0, getMaxBlockSize(),
          data == null ? -1 : data.length));
    }
  }

  private static File validateDeleteRequest(DeleteRequest request) {
    File ret = null;

    if (permissions == null || !permissions.isLocalDelete()) {
      throw new DeleteNotAllowedException("Delete not allowed");
    } else if (request == null) {
      throw new MissingRequestException("Missing request");
    } else if (request.getBaseDir() == null) {
      throw new MissingBaseDirectoryException("Missing base directory");
    } else if (request.getFileName() == null) {
      throw new MissingFileNameException("Missing file name");
    }

    ret = canonicalFile(new File(request.getBaseDir(), request.getFileName()));

    validateFileLocation(request.getBaseDir(), ret);
    validateWriteFile(ret);

    return ret;
  }

  private static void validateFileLocation(File baseDir, File file) {
    if (baseDir == null || file == null || !DirectoryUtils.isChild(baseDir, file)) {
      throw new InvalidFileLocationException(String.format("'%s' not within '%s'", file, baseDir));
    }
  }

  private static void validateLength(int length) {
    if (length < 0 || length > getMaxBlockSize()) {
      throw new InvalidDataLengthException(String.format("Length must be between %d and %d: %d", 0, getMaxBlockSize(), length));
    }
  }

  private static void validateOffset(long offset) {
    if (offset < 0 || offset > getMaxOffset()) {
      throw new InvalidFileOffsetException(String.format("Offset must be between %d and %d: %d", 0, getMaxOffset(), offset));
    }
  }

  private static void validateReadFile(File file) {
    if (file == null || !file.exists()) {
      throw new FileNotFoundException(String.format("Can not find file '%s'", file));
    } else if (!file.canRead()) {
      throw new FileNotReadableException(String.format("File '%s' is not readable", file));
    }
  }

  private static File validateReadRequest(ReadRequest request) {
    File ret = null;

    if (request == null) {
      throw new MissingRequestException("Missing request");
    } else if (request.getBaseDir() == null) {
      throw new MissingBaseDirectoryException("Missing base directory");
    } else if (request.getFileName() == null) {
      throw new MissingFileNameException("Missing file name");
    }

    validateBlockSize(request.getBlockSize());
    validateOffset(request.getOffset());

    ret = canonicalFile(new File(request.getBaseDir(), request.getFileName()));

    validateFileLocation(request.getBaseDir(), ret);
    validateReadFile(ret);

    return ret;
  }

  private static void validateWriteFile(File file) {
    if (file == null || (file.exists() && !file.canWrite())) {
      throw new FileNotWritableException(String.format("File '%s' is not writable", file));
    }
  }

  private static File validateWriteRequest(WriteRequest request) {
    File ret = null;

    if (permissions == null || !permissions.isLocalWrite()) {
      throw new WriteNotAllowedException("Write not allowed");
    } else if (request == null) {
      throw new MissingRequestException("Missing request");
    } else if (request.getBaseDir() == null) {
      throw new MissingBaseDirectoryException("Missing base directory");
    } else if (request.getFileName() == null) {
      throw new MissingFileNameException("Missing file name");
    }

    validateData(request.getData());
    validateOffset(request.getOffset());
    validateLength(request.getLength());

    ret = canonicalFile(new File(request.getBaseDir(), request.getFileName()));

    validateFileLocation(request.getBaseDir(), ret);
    validateWriteFile(ret);

    return ret;
  }

  /**
   * Write a block of a file.
   * 
   * @param request
   * @return
   * @throws IOException
   */
  public static WriteResponse writeBlock(WriteRequest request) {
    WriteResponse ret = new WriteResponse();

    File file = validateWriteRequest(request);

    ret.setRequest(request);

    if (!file.exists()) {
      File dir = file.getParentFile();
      try {
        dir.mkdirs();
      } catch (Exception ee) {
        throw new ErrorCreatingDirectoryException(String.format("Cant create directory '%s'", dir));
      }
      try {
        file.createNewFile();
      } catch (Exception ee) {
        throw new ErrorCreatingFileException(String.format("Cant create file '%s'", file));
      }
    }

    logger.info(String.format("WriteBlock: %s, Offset: %d, Length: %s, TimeStamp: %d, EOF: %b, Data: %s", file.getAbsolutePath(),
        request.getOffset(), request.getLength(), request.getTimeStamp(), request.isEof(),
        request.getData() == null ? -1 : request.getData().length));

    try {
      FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.WRITE);
      try {
        ByteBuffer buffer = ByteBuffer.wrap(request.getData());
        buffer.limit(request.getLength());

        channel.position(request.getOffset());

        int count = 0;
        int length = 0;
        do {
          count = channel.write(buffer);
          length += count;
        } while (count >= 0 && buffer.hasRemaining());

        if (request.isEof()) {
          channel.truncate(request.getOffset() + request.getLength());
        }

        if (request.getTimeStamp() > 0L) {
          file.setLastModified(request.getTimeStamp());
        }

        ret.setLength(length);
        ret.setCrc32(calcCRC(buffer.array(), 0, length));
        ret.setSuccess(ret.getLength() == request.getLength());
      } finally {
        channel.close();
      }
    } catch (IOException ee) {
      throw new ErrorWritingBlockException(ee.getMessage());
    }

    logger.info(String.format("WriteBlock: %s, Length: %s, CRC: %d, Success: %b", file.getAbsolutePath(), ret.getLength(),
        ret.getCrc32(), ret.isSuccess()));

    return ret;
  }

}