package com.wstrater.server.fileSync.common.data;

import java.io.File;

public class DirectoryMakeRequest {

  private File   baseDir;
  private String path;

  public File getBaseDir() {
    return baseDir;
  }

  public String getPath() {
    return path;
  }

  public void setBaseDir(File baseDir) {
    this.baseDir = baseDir;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("DirectoryMakeRequest [");
    if (baseDir != null)
      builder.append("baseDir=").append(baseDir).append(", ");
    if (path != null)
      builder.append("path=").append(path);
    builder.append("]");

    return builder.toString();
  }

}