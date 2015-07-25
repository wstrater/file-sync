package com.wstrater.server.fileSync.common.exceptions;

public class InvalidDataLengthException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public InvalidDataLengthException(String message) {
    super(message);
  }

}