package com.wstrater.server.fileSync.common.exceptions;

public class WriteNotAllowedException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public WriteNotAllowedException(String message) {
    super(message);
  }

}