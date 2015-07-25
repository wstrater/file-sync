package com.wstrater.server.fileSync.common.data;

import java.io.File;

public class DirectoryDeleteRequest {

  private File    baseDir;
  private boolean files;
  private String  path;
  private boolean recursive;

  public File getBaseDir() {
    return baseDir;
  }

  public boolean isFiles() {
    return files;
  }

  public String getPath() {
    return path;
  }

  public boolean isRecursive() {
    return recursive;
  }

  public void setBaseDir(File baseDir) {
    this.baseDir = baseDir;
  }

  public void setFiles(boolean files) {
    this.files = files;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setRecursive(boolean recursive) {
    this.recursive = recursive;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("DirectoryDeleteRequest [");
    if (baseDir != null)
      builder.append("baseDir=").append(baseDir).append(", ");
    if (path != null)
      builder.append("path=").append(path).append(", ");
    builder.append("files=").append(files).append(", recursive=").append(recursive).append("]");

    return builder.toString();
  }

}