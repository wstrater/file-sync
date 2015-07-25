package com.wstrater.server.fileSync.common.exceptions;

public class FileNotWritableException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public FileNotWritableException(String message) {
    super(message);
  }

}