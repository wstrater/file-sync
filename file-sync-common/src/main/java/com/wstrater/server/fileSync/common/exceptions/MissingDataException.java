package com.wstrater.server.fileSync.common.exceptions;

public class MissingDataException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public MissingDataException(String message) {
    super(message);
  }

}