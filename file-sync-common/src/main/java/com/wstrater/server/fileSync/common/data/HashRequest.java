package com.wstrater.server.fileSync.common.data;

import java.io.File;
import java.util.UUID;

/**
 * A request to hash a path within a directory.
 * 
 * @author wstrater
 *
 */
public class HashRequest {

  private File    baseDir;
  private String  hashType;
  private boolean hiddenDirectories;
  private boolean hiddenFiles;
  private String  id;
  private String  path;
  private boolean recursive;
  private boolean reHashExisting;

  public HashRequest() {
    id = UUID.randomUUID().toString();
  }

  public File getBaseDir() {
    return baseDir;
  }

  public String getHashType() {
    return hashType;
  }

  public boolean isHiddenDirectories() {
    return hiddenDirectories;
  }

  public boolean isHiddenFiles() {
    return hiddenFiles;
  }

  public String getId() {
    return id;
  }

  public String getPath() {
    return path;
  }

  public boolean isRecursive() {
    return recursive;
  }

  public boolean isReHashExisting() {
    return reHashExisting;
  }

  public void setBaseDir(File baseDir) {
    this.baseDir = baseDir;
  }

  public void setHashType(String hashType) {
    this.hashType = hashType;
  }

  public void setHiddenDirectories(boolean hiddenDirectories) {
    this.hiddenDirectories = hiddenDirectories;
  }

  public void setHiddenFiles(boolean hiddenFiles) {
    this.hiddenFiles = hiddenFiles;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setRecursive(boolean recursive) {
    this.recursive = recursive;
  }

  public void setReHashExisting(boolean reHashExisting) {
    this.reHashExisting = reHashExisting;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("HashRequest [");
    if (id != null)
      builder.append("id=").append(id).append(", ");
    if (baseDir != null)
      builder.append("baseDir=").append(baseDir).append(", ");
    if (path != null)
      builder.append("path=").append(path).append(", ");
    if (hashType != null)
      builder.append("hashType=").append(hashType).append(", ");
    builder.append("hiddenDirectories=").append(hiddenDirectories).append(", hiddenFiles=").append(hiddenFiles)
        .append(", recursive=").append(recursive).append(", reHashExisting=").append(reHashExisting).append("]");

    return builder.toString();
  }

}