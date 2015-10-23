package com.wstrater.server.fileSync.common.data;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wstrater.server.fileSync.common.utils.AccessUtils.Access;

/**
 * Details of a file within a directory.
 * 
 * @author wstrater
 *
 */
public class FileInfo implements InfoItem {

  private byte      access;
  private ChunkInfo chunkInfo;
  private String    hash;
  private String    hashType;
  private Long      lastModified;
  private Long      length;
  private String    name;

  /**
   * Used for adjusting the lastModified to and from UTC when being remotely transmitted.
   * 
   * @param offset
   */
  public void adjustLastModified(long offset) {
    lastModified += offset;
  }

  public byte getAccess() {
    return access;
  }

  public ChunkInfo getChunkInfo() {
    return chunkInfo;
  }

  public String getHash() {
    return hash;
  }

  public String getHashType() {
    return hashType;
  }

  public Long getLastModified() {
    return lastModified;
  }

  @Override
  @JsonIgnore
  public Date getLastModifiedDate() {
    Date ret = null;

    if (lastModified != null) {
      ret = new Date(lastModified);
    }

    return ret;
  }

  public Long getLength() {
    return length;
  }

  public String getName() {
    return name;
  }

  public void setAccess(byte access) {
    this.access = access;
  }

  public void setChunkInfo(ChunkInfo chunkInfo) {
    this.chunkInfo = chunkInfo;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public void setHashType(String hashType) {
    this.hashType = hashType;
  }

  public void setLastModified(Long lastModified) {
    this.lastModified = lastModified;
  }

  public void setLength(Long length) {
    this.length = length;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("FileInfo [");
    if (name != null)
      builder.append("name=").append(name).append(", ");
    builder.append("access=").append(Access.toString(access)).append(", ");
    if (hash != null)
      builder.append("hash=").append(hash).append(", ");
    if (hashType != null)
      builder.append("hashType=").append(hashType).append(", ");
    if (lastModified != null) {
      DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd.HH:mm:ss.SSS");
      builder.append("lastModified=").append(fmt.format(new Date(lastModified))).append(", ");
    }
    if (length != null)
      builder.append("length=").append(length).append(", ");
    if (chunkInfo != null)
      builder.append("chunkInfo=").append(chunkInfo);
    builder.append("]");

    return builder.toString();
  }

}