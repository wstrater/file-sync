package com.wstrater.server.fileSync.common.exceptions;

public class InvalidFileLocationException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public InvalidFileLocationException(String message) {
    super(message);
  }

}