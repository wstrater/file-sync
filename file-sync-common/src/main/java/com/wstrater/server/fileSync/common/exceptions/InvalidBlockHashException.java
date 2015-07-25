package com.wstrater.server.fileSync.common.exceptions;

public class InvalidBlockHashException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public InvalidBlockHashException(String message) {
    super(message);
  }

}