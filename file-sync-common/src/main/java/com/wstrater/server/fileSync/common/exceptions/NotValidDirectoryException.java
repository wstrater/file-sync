package com.wstrater.server.fileSync.common.exceptions;

public class NotValidDirectoryException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public NotValidDirectoryException(String message) {
    super(message);
  }

}