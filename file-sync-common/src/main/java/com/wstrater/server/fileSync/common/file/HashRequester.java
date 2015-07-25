package com.wstrater.server.fileSync.common.file;

import com.wstrater.server.fileSync.common.data.HashRequest;
import com.wstrater.server.fileSync.common.data.HashResponse;
import com.wstrater.server.fileSync.common.data.HashStatus;

public interface HashRequester {

  public HashStatus getHashStatus(String id);

  public HashResponse hashDirectory(HashRequest request);

}