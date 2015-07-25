package com.wstrater.server.fileSync.common.exceptions;

public class MissingIdException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public MissingIdException(String message) {
    super(message);
  }

}