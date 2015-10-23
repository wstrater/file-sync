package com.wstrater.server.fileSync.common.file;

import com.wstrater.server.fileSync.common.data.DirectoryDeleteRequest;
import com.wstrater.server.fileSync.common.data.DirectoryDeleteResponse;
import com.wstrater.server.fileSync.common.data.DirectoryListRequest;
import com.wstrater.server.fileSync.common.data.DirectoryListResponse;
import com.wstrater.server.fileSync.common.data.DirectoryMakeRequest;
import com.wstrater.server.fileSync.common.data.DirectoryMakeResponse;
import com.wstrater.server.fileSync.common.data.DirectoryPermissionsRequest;
import com.wstrater.server.fileSync.common.data.DirectoryPermissionsResponse;

public interface DirectoryLister {

  public DirectoryDeleteResponse deleteDirectory(DirectoryDeleteRequest request);

  public DirectoryPermissionsResponse getPermissions(DirectoryPermissionsRequest request);

  public DirectoryListResponse listDirectory(DirectoryListRequest request);

  public DirectoryMakeResponse makeDirectory(DirectoryMakeRequest request);

}