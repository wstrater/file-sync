package com.wstrater.server.fileSync.common.file;

import com.wstrater.server.fileSync.common.data.HashRequest;
import com.wstrater.server.fileSync.common.data.HashResponse;
import com.wstrater.server.fileSync.common.data.HashStatus;
import com.wstrater.server.fileSync.common.hash.HashProcessor;

public class HashRequesterLocalImpl implements HashRequester {

  @Override
  public HashStatus getHashStatus(String id) {
    return HashProcessor.getHashStatus(id);
  }

  @Override
  public HashResponse hashDirectory(HashRequest request) {
    return HashProcessor.hashDirectory(request);
  }

}