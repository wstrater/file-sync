package com.wstrater.server.fileSync.common.file;

import com.wstrater.server.fileSync.common.data.DirectoryDeleteRequest;
import com.wstrater.server.fileSync.common.data.DirectoryDeleteResponse;
import com.wstrater.server.fileSync.common.data.DirectoryListRequest;
import com.wstrater.server.fileSync.common.data.DirectoryListResponse;
import com.wstrater.server.fileSync.common.data.DirectoryMakeRequest;
import com.wstrater.server.fileSync.common.data.DirectoryMakeResponse;
import com.wstrater.server.fileSync.common.data.DirectoryPermissionsRequest;
import com.wstrater.server.fileSync.common.data.DirectoryPermissionsResponse;
import com.wstrater.server.fileSync.common.utils.FilePermissions;
import com.wstrater.server.fileSync.common.utils.FileUtils;

/**
 * This is used for testing where both the remote and local are local. Remote writes could fail
 * despite the remoteWrite being true because localWrite is checked.
 * 
 * @author wstrater
 *
 */
public class DirectoryListerLocalAsRemoteImpl extends DirectoryListerLocalImpl {

  @Override
  public DirectoryDeleteResponse deleteDirectory(DirectoryDeleteRequest request) {
    DirectoryDeleteResponse ret;

    FilePermissions permissions = FileUtils.getPermissions();
    try {
      swapPermissions(permissions);

      ret = super.deleteDirectory(request);
    } finally {
      FileUtils.setPermissions(permissions);
    }

    return ret;
  }

  @Override
  public DirectoryPermissionsResponse getPermissions(DirectoryPermissionsRequest request) {
    DirectoryPermissionsResponse ret;

    FilePermissions permissions = FileUtils.getPermissions();
    try {
      swapPermissions(permissions);

      ret = super.getPermissions(request);
    } finally {
      FileUtils.setPermissions(permissions);
    }

    return ret;
  }

  @Override
  public DirectoryListResponse listDirectory(DirectoryListRequest request) {
    DirectoryListResponse ret;

    FilePermissions permissions = FileUtils.getPermissions();
    try {
      swapPermissions(permissions);

      ret = super.listDirectory(request);
    } finally {
      FileUtils.setPermissions(permissions);
    }

    return ret;
  }

  @Override
  public DirectoryMakeResponse makeDirectory(DirectoryMakeRequest request) {
    DirectoryMakeResponse ret;

    FilePermissions permissions = FileUtils.getPermissions();
    try {
      swapPermissions(permissions);

      ret = super.makeDirectory(request);
    } finally {
      FileUtils.setPermissions(permissions);
    }

    return ret;
  }

  /**
   * Swap the local and remote permissions.
   * 
   * @param permissions
   */
  private void swapPermissions(FilePermissions permissions) {
    FileUtils.setPermissions(new FilePermissions(permissions.isRemoteDelete(), permissions.isRemoteWrite(), permissions
        .isLocalDelete(), permissions.isLocalWrite()));
  }

}