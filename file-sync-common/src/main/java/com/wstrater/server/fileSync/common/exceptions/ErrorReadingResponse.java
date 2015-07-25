package com.wstrater.server.fileSync.common.exceptions;

public class ErrorReadingResponse extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public ErrorReadingResponse(String message) {
    super(message);
  }

}