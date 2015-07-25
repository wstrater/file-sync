package com.wstrater.server.fileSync.common.data;

import java.io.File;

public class DeleteRequest {

  private File   baseDir;
  private String fileName;

  public File getBaseDir() {
    return baseDir;
  }

  public String getFileName() {
    return fileName;
  }

  public void setBaseDir(File baseDir) {
    this.baseDir = baseDir;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("DeleteRequest [");
    if (baseDir != null)
      builder.append("baseDir=").append(baseDir).append(", ");
    if (fileName != null)
      builder.append("fileName=").append(fileName);
    builder.append("]");

    return builder.toString();
  }
}