package com.wstrater.server.fileSync.common.exceptions;

public class MissingBaseDirectoryException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public MissingBaseDirectoryException(String message) {
    super(message);
  }

}