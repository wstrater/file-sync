package com.wstrater.server.fileSync.common.exceptions;

public class InvalidBlockSizeException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public InvalidBlockSizeException(String message) {
    super(message);
  }

}