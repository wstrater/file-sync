package com.wstrater.server.fileSync.common.file;

import com.wstrater.server.fileSync.common.data.DeleteRequest;
import com.wstrater.server.fileSync.common.data.DeleteResponse;
import com.wstrater.server.fileSync.common.data.WriteRequest;
import com.wstrater.server.fileSync.common.data.WriteResponse;

/**
 * This interface represents writing a block to a file. It either encapsulates a local
 * implementation or a call to a remote implementation.
 * 
 * @author wstrater
 *
 */
public interface BlockWriter {

  public DeleteResponse deleteFile(DeleteRequest request);

  public WriteResponse writeBlock(WriteRequest request);

}