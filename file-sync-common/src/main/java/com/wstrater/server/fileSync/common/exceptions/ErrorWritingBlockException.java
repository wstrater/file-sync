package com.wstrater.server.fileSync.common.exceptions;

public class ErrorWritingBlockException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public ErrorWritingBlockException(String message) {
    super(message);
  }

}