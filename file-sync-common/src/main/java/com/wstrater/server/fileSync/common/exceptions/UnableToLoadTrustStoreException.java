package com.wstrater.server.fileSync.common.exceptions;

public class UnableToLoadTrustStoreException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public UnableToLoadTrustStoreException(String message) {
    super(message);
  }

  public UnableToLoadTrustStoreException(String message, Throwable thrown) {
    super(message, thrown);
  }

}