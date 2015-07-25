package com.wstrater.server.fileSync.common.file;

import com.wstrater.server.fileSync.common.data.DirectoryDeleteRequest;
import com.wstrater.server.fileSync.common.data.DirectoryDeleteResponse;
import com.wstrater.server.fileSync.common.data.DirectoryListRequest;
import com.wstrater.server.fileSync.common.data.DirectoryListResponse;
import com.wstrater.server.fileSync.common.data.DirectoryMakeRequest;
import com.wstrater.server.fileSync.common.data.DirectoryMakeResponse;

public interface DirectoryLister {

  public DirectoryDeleteResponse deleteDirectory(DirectoryDeleteRequest request);

  public DirectoryListResponse listDirectory(DirectoryListRequest request);

  public DirectoryMakeResponse makeDirectory(DirectoryMakeRequest request);

}