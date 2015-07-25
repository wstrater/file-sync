package com.wstrater.server.fileSync.common.exceptions;

public class ErrorReadingBlockException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public ErrorReadingBlockException(String message) {
    super(message);
  }

}