package com.wstrater.server.fileSync.common.exceptions;

public class UnableToConfigureSSLException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public UnableToConfigureSSLException(String message) {
    super(message);
  }

  public UnableToConfigureSSLException(String message, Throwable thrown) {
    super(message, thrown);
  }

}