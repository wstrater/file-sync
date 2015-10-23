package com.wstrater.server.fileSync.common.file;

import com.wstrater.server.fileSync.common.data.DirectoryDeleteRequest;
import com.wstrater.server.fileSync.common.data.DirectoryDeleteResponse;
import com.wstrater.server.fileSync.common.data.DirectoryListRequest;
import com.wstrater.server.fileSync.common.data.DirectoryListResponse;
import com.wstrater.server.fileSync.common.data.DirectoryMakeRequest;
import com.wstrater.server.fileSync.common.data.DirectoryMakeResponse;
import com.wstrater.server.fileSync.common.data.DirectoryPermissionsRequest;
import com.wstrater.server.fileSync.common.data.DirectoryPermissionsResponse;
import com.wstrater.server.fileSync.common.utils.DirectoryUtils;

public class DirectoryListerLocalImpl implements DirectoryLister {

  @Override
  public DirectoryDeleteResponse deleteDirectory(DirectoryDeleteRequest request) {
    return DirectoryUtils.deleteDirectory(request);
  }

  @Override
  public DirectoryPermissionsResponse getPermissions(DirectoryPermissionsRequest request) {
    return DirectoryUtils.getPermissions(request);
  }

  @Override
  public DirectoryListResponse listDirectory(DirectoryListRequest request) {
    return DirectoryUtils.listDirectory(request);
  }

  @Override
  public DirectoryMakeResponse makeDirectory(DirectoryMakeRequest request) {
    return DirectoryUtils.makeDirectory(request);
  }

}