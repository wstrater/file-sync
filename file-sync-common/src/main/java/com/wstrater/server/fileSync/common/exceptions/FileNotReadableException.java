package com.wstrater.server.fileSync.common.exceptions;

public class FileNotReadableException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public FileNotReadableException(String message) {
    super(message);
  }

}