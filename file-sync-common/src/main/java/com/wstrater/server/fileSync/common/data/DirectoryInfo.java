package com.wstrater.server.fileSync.common.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wstrater.server.fileSync.common.utils.AccessUtils.Access;

/**
 * Details of a directory.
 * 
 * @author wstrater
 *
 */
public class DirectoryInfo implements InfoItem {

  private byte                access;
  private List<DirectoryInfo> directories;
  private List<FileInfo>      files;
  private String              name;

  public void addDirectory(DirectoryInfo directoryInfo) {
    if (directories == null) {
      directories = new ArrayList<>();
    }
    directories.add(directoryInfo);
  }

  public void addFile(FileInfo fileInfo) {
    if (files == null) {
      files = new ArrayList<>();
    }
    files.add(fileInfo);
  }

  /**
   * Used for adjusting the lastModified to and from UTC when being remotely transmitted.
   * 
   * @param offset
   */
  public void adjustLastModified(long offset) {
    for (FileInfo fileInfo : getFiles()) {
      fileInfo.adjustLastModified(offset);
    }
    for (DirectoryInfo directoryInfo : getDirectories()) {
      directoryInfo.adjustLastModified(offset);
    }
  }

  public byte getAccess() {
    return access;
  }

  public List<DirectoryInfo> getDirectories() {
    if (directories == null) {
      return Collections.emptyList();
    } else {
      return directories;
    }
  }

  public List<FileInfo> getFiles() {
    if (files == null) {
      return Collections.emptyList();
    } else {
      return files;
    }
  }

  @Override
  @JsonIgnore
  public Long getLength() {
    return null;
  }

  @Override
  @JsonIgnore
  public Long getLastModified() {
    return null;
  }

  @Override
  @JsonIgnore
  public Date getLastModifiedDate() {
    return null;
  }

  public String getName() {
    return name;
  }

  public void setAccess(byte access) {
    this.access = access;
  }

  public void setDirectories(List<DirectoryInfo> directories) {
    this.directories = directories;
  }

  public void setFiles(List<FileInfo> files) {
    this.files = files;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return toString("");
  }

  public String toString(String indent) {
    StringBuilder builder = new StringBuilder();

    builder.append(indent).append("DirectoryInfo [");
    if (name != null)
      builder.append("name=").append(name);
    builder.append(", ").append("access=").append(Access.toString(access));
    if (directories != null) {
      builder.append(", ").append("directories=[");
      for (DirectoryInfo directoryInfo : directories) {
        builder.append("\n  ").append(indent).append(directoryInfo.toString(indent + "  ")).append(',');
      }
      builder.append("]");
    }
    if (files != null) {
      builder.append(", ").append("files=[");
      for (FileInfo fileInfo : files) {
        builder.append("\n  ").append(indent).append(indent).append(fileInfo).append(',');
      }
      builder.append("]");
    }
    builder.append("]");

    return builder.toString();
  }

}