package com.wstrater.server.fileSync.common.exceptions;

public class InvalidBlockLengthException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public InvalidBlockLengthException(String message) {
    super(message);
  }

}