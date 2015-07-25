package com.wstrater.server.fileSync.common.exceptions;

public class MissingFileNameException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public MissingFileNameException(String message) {
    super(message);
  }

}