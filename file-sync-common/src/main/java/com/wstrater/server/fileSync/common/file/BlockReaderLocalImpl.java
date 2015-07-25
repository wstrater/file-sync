package com.wstrater.server.fileSync.common.file;

import com.wstrater.server.fileSync.common.data.ReadRequest;
import com.wstrater.server.fileSync.common.data.ReadResponse;
import com.wstrater.server.fileSync.common.utils.FileUtils;

public class BlockReaderLocalImpl implements BlockReader {

  @Override
  public ReadResponse readBlock(ReadRequest request) {
    return FileUtils.readBlock(request);
  }

}