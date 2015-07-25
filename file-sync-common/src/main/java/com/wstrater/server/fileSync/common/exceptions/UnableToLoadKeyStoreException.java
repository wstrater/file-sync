package com.wstrater.server.fileSync.common.exceptions;

public class UnableToLoadKeyStoreException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public UnableToLoadKeyStoreException(String message) {
    super(message);
  }

  public UnableToLoadKeyStoreException(String message, Throwable thrown) {
    super(message, thrown);
  }

}