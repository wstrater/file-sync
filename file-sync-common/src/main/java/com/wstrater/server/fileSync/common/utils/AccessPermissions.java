package com.wstrater.server.fileSync.common.utils;

import com.wstrater.server.fileSync.common.utils.AccessUtils.Access;

/**
 * Used to check the access of the local file and remote file for performing syncing.
 * 
 * @author wstrater
 *
 */
public class AccessPermissions {

  private byte localAccess;
  private byte remoteAccess;

  private AccessPermissions(byte localAccess, byte remoteAccess) {
    this.localAccess = localAccess;
    this.remoteAccess = remoteAccess;
  }

  public static AccessPermissions both(byte localAccess, byte remoteAccess) {
    return new AccessPermissions(localAccess, remoteAccess);
  }

  /**
   * Can you delete from the local directory.
   * 
   * @return
   */
  public boolean isLocalDeleteDirectory() {
    return AccessUtils.canDeleteDir(localAccess);
  }

  /**
   * Can you delete the local file.
   * 
   * @return
   */
  public boolean isLocalDelete() {
    return AccessUtils.canDeleteFile(localAccess);
  }

  /**
   * Can your read from the remote directory and write to the local directory.
   * 
   * @return
   */
  public boolean isLocalWriteDirectory() {
    return AccessUtils.canReadDir(remoteAccess) && AccessUtils.canWriteDir(localAccess);
  }

  /**
   * Can you read the remote file and write the local file.
   * 
   * @return
   */
  public boolean isLocalWrite() {
    return AccessUtils.canReadFile(remoteAccess) && AccessUtils.canWriteFile(localAccess);
  }

  /**
   * Can you delete from the remote directory.
   * 
   * @return
   */
  public boolean isRemoteDeleteDirectory() {
    return AccessUtils.canDeleteDir(remoteAccess);
  }

  /**
   * Can you delete the remote file.
   * 
   * @return
   */
  public boolean isRemoteDelete() {
    return AccessUtils.canDeleteFile(remoteAccess);
  }

  /**
   * Can you read from the local file and write to the remote file.
   * 
   * @return
   */
  public boolean isRemoteWriteDirectory() {
    return AccessUtils.canReadDir(localAccess) && AccessUtils.canWriteDir(remoteAccess);
  }

  /**
   * Can you read the local file and write the remote file.
   * 
   * @return
   */
  public boolean isRemoteWrite() {
    return AccessUtils.canReadFile(localAccess) && AccessUtils.canWriteFile(remoteAccess);
  }

  public static AccessPermissions localOnly(byte localAccess) {
    return new AccessPermissions(localAccess, (byte) 0);
  }

  public static AccessPermissions remoteOnly(byte remoteAccess) {
    return new AccessPermissions((byte) 0, remoteAccess);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("AccessPermissions [local=").append(Access.toString(localAccess)).append(", remote=")
        .append(Access.toString(remoteAccess)).append("]");
    return builder.toString();
  }

}