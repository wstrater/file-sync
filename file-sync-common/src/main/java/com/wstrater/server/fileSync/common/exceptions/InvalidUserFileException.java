package com.wstrater.server.fileSync.common.exceptions;

public class InvalidUserFileException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public InvalidUserFileException(String message) {
    super(message);
  }

  public InvalidUserFileException(String message, Throwable thrown) {
    super(message, thrown);
  }

}