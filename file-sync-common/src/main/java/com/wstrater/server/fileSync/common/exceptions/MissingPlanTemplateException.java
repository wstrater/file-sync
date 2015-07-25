package com.wstrater.server.fileSync.common.exceptions;

public class MissingPlanTemplateException extends FileSyncException {

  private static final long serialVersionUID = 20150704L;

  public MissingPlanTemplateException(String message) {
    super(message);
  }

}