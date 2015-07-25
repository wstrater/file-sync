package com.wstrater.server.fileSync.common.exceptions;

public class MissingPathException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public MissingPathException(String message) {
    super(message);
  }

}