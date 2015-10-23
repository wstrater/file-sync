package com.wstrater.server.fileSync.client;

import static com.wstrater.server.fileSync.common.utils.AccessUtils.access;

import com.wstrater.server.fileSync.common.data.DirectoryInfo;
import com.wstrater.server.fileSync.common.data.DirectoryListRequest;
import com.wstrater.server.fileSync.common.data.DirectoryListResponse;
import com.wstrater.server.fileSync.common.data.DirectoryPermissionsRequest;
import com.wstrater.server.fileSync.common.data.DirectoryPermissionsResponse;
import com.wstrater.server.fileSync.common.data.FileInfo;

/**
 * This class is used to override the permissions returned from the server for unit/integration
 * testing. The server will always use the permissions it was started with but the test will want to
 * adjust them to match the test conditions.
 * 
 * @author wstrater
 *
 */
public class DirectoryListerRemoteUnitTestImpl extends DirectoryListerRemoteImpl {

  boolean allowDelete;
  boolean allowWrite;

  public DirectoryListerRemoteUnitTestImpl(RemoteUnitTestClient remoteClient) {
    super(remoteClient);

    allowDelete = remoteClient.isAllowDelete();
    allowWrite = remoteClient.isAllowWrite();
  }

  private void adjustPermissions(DirectoryInfo directoryInfo) {
    if (directoryInfo != null) {
      directoryInfo.setAccess(access(directoryInfo.getAccess()).overrideForTesting(allowDelete, allowWrite).get());

      for (FileInfo child : directoryInfo.getFiles()) {
        child.setAccess(access(child.getAccess()).overrideForTesting(allowDelete, allowWrite).get());
      }

      for (DirectoryInfo child : directoryInfo.getDirectories()) {
        adjustPermissions(child);
      }
    }
  }

  @Override
  public DirectoryPermissionsResponse getPermissions(DirectoryPermissionsRequest request) {
    DirectoryPermissionsResponse ret = super.getPermissions(request);

    ret.setAllowDelete(allowDelete);
    ret.setAllowWrite(allowWrite);

    return ret;
  }

  @Override
  public DirectoryListResponse listDirectory(DirectoryListRequest request) {
    DirectoryListResponse ret = super.listDirectory(request);

    adjustPermissions(ret.getDirectoryInfo());

    return ret;
  }

  public void setRemoteClient(RemoteUnitTestClient remoteClient) {
    super.setRemoteClient(remoteClient);

    allowDelete = remoteClient.isAllowDelete();
    allowWrite = remoteClient.isAllowWrite();
  }

}