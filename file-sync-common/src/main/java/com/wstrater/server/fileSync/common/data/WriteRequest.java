package com.wstrater.server.fileSync.common.data;

import java.io.File;

/**
 * A request to write a block to a file.
 * 
 * @author wstrater
 *
 */
public class WriteRequest {

  private File    baseDir;
  private byte[]  data;
  private boolean eof;
  private String  fileName;
  private int     length;
  private long    offset;
  private long    timeStamp;

  public File getBaseDir() {
    return baseDir;
  }

  public byte[] getData() {
    return data;
  }

  public boolean isEof() {
    return eof;
  }

  public String getFileName() {
    return fileName;
  }

  public int getLength() {
    return length;
  }

  public long getOffset() {
    return offset;
  }

  public long getTimeStamp() {
    return timeStamp;
  }

  public void setBaseDir(File baseDir) {
    this.baseDir = baseDir;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public void setEof(boolean eof) {
    this.eof = eof;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public void setLength(int length) {
    this.length = length;
  }

  public void setOffset(long offset) {
    this.offset = offset;
  }

  public void setTimeStamp(long timeStamp) {
    this.timeStamp = timeStamp;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("WriteRequest [");
    if (baseDir != null)
      builder.append("baseDir=").append(baseDir).append(", ");
    if (fileName != null)
      builder.append("fileName=").append(fileName).append(", ");
    if (data != null)
      builder.append("data=").append(data.length).append(", ");
    builder.append("eof=").append(eof).append(", length=").append(length).append(", offset=").append(offset).append(", timeStamp=")
        .append(timeStamp).append("]");

    return builder.toString();
  }

}