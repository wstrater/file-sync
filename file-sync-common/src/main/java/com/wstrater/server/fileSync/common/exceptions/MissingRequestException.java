package com.wstrater.server.fileSync.common.exceptions;

public class MissingRequestException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public MissingRequestException(String message) {
    super(message);
  }

}