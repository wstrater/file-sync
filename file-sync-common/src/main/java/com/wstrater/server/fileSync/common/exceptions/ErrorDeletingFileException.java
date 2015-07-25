package com.wstrater.server.fileSync.common.exceptions;

public class ErrorDeletingFileException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public ErrorDeletingFileException(String message) {
    super(message);
  }

}