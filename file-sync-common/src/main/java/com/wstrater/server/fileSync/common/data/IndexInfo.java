package com.wstrater.server.fileSync.common.data;

import java.util.Date;

import com.wstrater.server.fileSync.common.utils.Compare;
import com.wstrater.server.fileSync.common.utils.AccessUtils.Access;

/**
 * Keeps track of the last know state of a file within a directory.
 * 
 * @author wstrater
 *
 */
public class IndexInfo implements Comparable<IndexInfo>, InfoItem {

  private byte      access;
  private ChunkInfo chunkInfo;
  private String    hash;
  private String    hashType;
  private Long      lastModified;
  private Long      length;
  private String    name;

  @Override
  public int compareTo(IndexInfo that) {
    int ret = 0;

    if (that == null) {
      ret = Compare.compare(this, that);
    } else {
      ret = Compare.compare(this.name, that.name);
    }

    return ret;
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

    builder.append("IndexInfo [");
    if (name != null)
      builder.append("name=").append(name).append(", ");
    builder.append("access=").append(Access.toString(access)).append(", ");
    if (lastModified != null)
      builder.append("lastModified=").append(lastModified).append(", ");
    if (length != null)
      builder.append("length=").append(length).append(", ");
    if (hashType != null)
      builder.append("hashType=").append(hashType).append(", ");
    if (hash != null)
      builder.append("hash=").append(hash).append(", ");
    if (chunkInfo != null)
      builder.append("chunkInfo=").append(chunkInfo);
    builder.append("]");

    return builder.toString();
  }

}