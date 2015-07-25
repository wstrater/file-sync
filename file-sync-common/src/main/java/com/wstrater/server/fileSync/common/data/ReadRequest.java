package com.wstrater.server.fileSync.common.data;

import java.io.File;

/**
 * A request to read a block of a file.
 * 
 * @author wstrater
 *
 */
public class ReadRequest {

  private File   baseDir;
  private String fileName;
  private int    blockSize;
  private long   offset;

  public File getBaseDir() {
    return baseDir;
  }

  public String getFileName() {
    return fileName;
  }

  public int getBlockSize() {
    return blockSize;
  }

  public long getOffset() {
    return offset;
  }

  public void setBaseDir(File baseDir) {
    this.baseDir = baseDir;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public void setBlockSize(int blockSize) {
    this.blockSize = blockSize;
  }

  public void setOffset(long offset) {
    this.offset = offset;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("ReadRequest [");
    if (baseDir != null)
      builder.append("baseDir=").append(baseDir).append(", ");
    if (fileName != null)
      builder.append("fileName=").append(fileName).append(", ");
    builder.append("offset=").append(offset).append(", blockSize=").append(blockSize).append("]");

    return builder.toString();
  }

}