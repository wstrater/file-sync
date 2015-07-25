package com.wstrater.server.fileSync.common.exceptions;

public class InvalidFileHashException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public InvalidFileHashException(String message) {
    super(message);
  }

}