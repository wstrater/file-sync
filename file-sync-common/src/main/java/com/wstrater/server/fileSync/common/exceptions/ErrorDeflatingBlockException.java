package com.wstrater.server.fileSync.common.exceptions;

public class ErrorDeflatingBlockException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public ErrorDeflatingBlockException(String message) {
    super(message);
  }

  public ErrorDeflatingBlockException(String message, Throwable thrown) {
    super(message, thrown);
  }

  public ErrorDeflatingBlockException(Throwable thrown) {
    super(thrown);
  }

}