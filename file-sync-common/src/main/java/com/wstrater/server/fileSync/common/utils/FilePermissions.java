package com.wstrater.server.fileSync.common.utils;

/**
 * This class contains the declared permissions of both the local and remote file systems. It is
 * used when creating the {@link PlanMapper}.
 * 
 * @author wstrater
 *
 */
public class FilePermissions {

  public final static boolean DEFAULT_LOCAL_DELETE  = false;
  public final static boolean DEFAULT_LOCAL_WRITE   = true;
  public final static boolean DEFAULT_REMOTE_DELETE = true;
  public final static boolean DEFAULT_REMOTE_WRITE  = true;

  private boolean             localDelete           = DEFAULT_LOCAL_DELETE;
  private boolean             localWrite            = DEFAULT_LOCAL_WRITE;
  private boolean             remoteDelete          = DEFAULT_REMOTE_DELETE;
  private boolean             remoteWrite           = DEFAULT_REMOTE_WRITE;

  public FilePermissions() {}

  public FilePermissions(boolean localDelete, boolean localWrite, boolean remoteDelete, boolean remoteWrite) {
    this.localDelete = localDelete;
    this.localWrite = localWrite;
    this.remoteDelete = remoteDelete;
    this.remoteWrite = remoteWrite;
  }

  public boolean isLocalDelete() {
    return localDelete;
  }

  public boolean isLocalWrite() {
    return localWrite;
  }

  public boolean isRemoteDelete() {
    return remoteDelete;
  }

  public boolean isRemoteWrite() {
    return remoteWrite;
  }

  public void setLocalDelete(boolean localDelete) {
    this.localDelete = localDelete;
  }

  public void setLocalWrite(boolean localWrite) {
    this.localWrite = localWrite;
  }

  public void setRemoteDelete(boolean remoteDelete) {
    this.remoteDelete = remoteDelete;
  }

  public void setRemoteWrite(boolean remoteWrite) {
    this.remoteWrite = remoteWrite;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("(LD=").append(Constants.tOrF(localDelete)).append(",LW=").append(Constants.tOrF(localWrite)).append(",RD=")
        .append(Constants.tOrF(remoteDelete)).append(",RW=").append(Constants.tOrF(remoteWrite)).append(")");

    return builder.toString();
  }

}