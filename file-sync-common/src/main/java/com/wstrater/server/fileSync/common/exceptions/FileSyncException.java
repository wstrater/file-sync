package com.wstrater.server.fileSync.common.exceptions;

/**
 * These are run-time exceptions used by the system to report errors.
 * 
 * @author wstrater
 *
 */
public class FileSyncException extends RuntimeException {

  private static final long serialVersionUID = 20150704L;

  public FileSyncException(String message) {
    super(message);
  }

  public FileSyncException(String message, Throwable thrown) {
    super(message, thrown);
  }

  public FileSyncException(Throwable thrown) {
    super(thrown);
  }

}