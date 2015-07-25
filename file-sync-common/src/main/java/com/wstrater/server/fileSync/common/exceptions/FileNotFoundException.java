package com.wstrater.server.fileSync.common.exceptions;

public class FileNotFoundException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public FileNotFoundException(String message) {
    super(message);
  }

}