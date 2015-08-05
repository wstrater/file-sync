package com.wstrater.server.fileSync.common.exceptions;

public class ErrorInflatingBlockException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public ErrorInflatingBlockException(String message) {
    super(message);
  }

  public ErrorInflatingBlockException(String message, Throwable thrown) {
    super(message, thrown);
  }

  public ErrorInflatingBlockException(Throwable thrown) {
    super(thrown);
  }

}