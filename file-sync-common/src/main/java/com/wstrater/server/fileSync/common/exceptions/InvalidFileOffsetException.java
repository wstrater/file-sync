package com.wstrater.server.fileSync.common.exceptions;

public class InvalidFileOffsetException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public InvalidFileOffsetException(String message) {
    super(message);
  }

}