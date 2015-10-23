package com.wstrater.server.fileSync.common.exceptions;

public class ErrorGettingPermissionsException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public ErrorGettingPermissionsException(String message) {
    super(message);
  }

}