package com.wstrater.server.fileSync.common.file;

import com.wstrater.server.fileSync.common.data.DeleteRequest;
import com.wstrater.server.fileSync.common.data.DeleteResponse;
import com.wstrater.server.fileSync.common.data.WriteRequest;
import com.wstrater.server.fileSync.common.data.WriteResponse;
import com.wstrater.server.fileSync.common.utils.FilePermissions;
import com.wstrater.server.fileSync.common.utils.FileUtils;

/**
 * This is used for testing where both the remote and local are local. Remote writes could fail
 * despite the remoteWrite being true because localWrite is checked.
 * 
 * @author wstrater
 *
 */
public class BlockWriterLocalAsRemoteImpl extends BlockWriterLocalImpl {

  @Override
  public DeleteResponse deleteFile(DeleteRequest request) {
    DeleteResponse ret;

    FilePermissions permissions = FileUtils.getPermissions();
    try {
      swapPermissions(permissions);

      ret = super.deleteFile(request);
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

  @Override
  public WriteResponse writeBlock(WriteRequest request) {
    WriteResponse ret;

    FilePermissions permissions = FileUtils.getPermissions();
    try {
      swapPermissions(permissions);

      ret = super.writeBlock(request);
    } finally {
      FileUtils.setPermissions(permissions);
    }

    return ret;
  }

}