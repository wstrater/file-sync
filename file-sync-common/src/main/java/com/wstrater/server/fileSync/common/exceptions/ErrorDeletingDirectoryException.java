package com.wstrater.server.fileSync.common.exceptions;

public class ErrorDeletingDirectoryException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public ErrorDeletingDirectoryException(String message) {
    super(message);
  }

}