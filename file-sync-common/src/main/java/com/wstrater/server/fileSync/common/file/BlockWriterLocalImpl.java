package com.wstrater.server.fileSync.common.file;

import com.wstrater.server.fileSync.common.data.DeleteRequest;
import com.wstrater.server.fileSync.common.data.DeleteResponse;
import com.wstrater.server.fileSync.common.data.WriteRequest;
import com.wstrater.server.fileSync.common.data.WriteResponse;
import com.wstrater.server.fileSync.common.utils.FileUtils;

public class BlockWriterLocalImpl implements BlockWriter {

  @Override
  public DeleteResponse deleteFile(DeleteRequest request){
    return FileUtils.deleteFile(request);
  }

  @Override
  public WriteResponse writeBlock(WriteRequest request){
    return FileUtils.writeBlock(request);
  }

}