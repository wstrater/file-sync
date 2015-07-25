package com.wstrater.server.fileSync.common.exceptions;

public class DeleteNotAllowedException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public DeleteNotAllowedException(String message) {
    super(message);
  }

}