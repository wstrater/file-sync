package com.wstrater.server.fileSync.common.file;

import com.wstrater.server.fileSync.common.data.ReadRequest;
import com.wstrater.server.fileSync.common.data.ReadResponse;

/**
 * This interface represents reading a block from a file. It either encapsulates a local
 * implementation or a call to a remote implementation.
 * 
 * @author wstrater
 *
 */
public interface BlockReader {

  public ReadResponse readBlock(ReadRequest request);

}