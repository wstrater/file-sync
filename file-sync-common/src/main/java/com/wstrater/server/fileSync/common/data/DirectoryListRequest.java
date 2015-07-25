package com.wstrater.server.fileSync.common.data;

import java.io.File;

public class DirectoryListRequest {

  private File    baseDir;
  private boolean hiddenDirectories;
  private boolean hiddenFiles;
  private String  path;
  private boolean recursive;

  public File getBaseDir() {
    return baseDir;
  }

  public boolean isHiddenDirectories() {
    return hiddenDirectories;
  }

  public boolean isHiddenFiles() {
    return hiddenFiles;
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

  public void setHiddenDirectories(boolean hiddenDirectories) {
    this.hiddenDirectories = hiddenDirectories;
  }

  public void setHiddenFiles(boolean hiddenFiles) {
    this.hiddenFiles = hiddenFiles;
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

    builder.append("DirectoryListRequest [");
    if (baseDir != null)
      builder.append("baseDir=").append(baseDir).append(", ");
    if (path != null)
      builder.append("path=").append(path).append(", ");
    builder.append("hiddenDirectories=").append(hiddenDirectories).append(", hiddenFiles=").append(hiddenFiles)
        .append(", recursive=").append(recursive).append("]");

    return builder.toString();
  }

}