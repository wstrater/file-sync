package com.wstrater.server.fileSync.common.exceptions;


public class InvalidDataBlockException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public InvalidDataBlockException(String message) {
    super(message);
  }

}