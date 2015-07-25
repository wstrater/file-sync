package com.wstrater.server.fileSync.common.data;

public class DirectoryMakeResponse {

  private DirectoryMakeRequest request;
  private boolean              success;

  public DirectoryMakeRequest getRequest() {
    return request;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setRequest(DirectoryMakeRequest request) {
    this.request = request;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("DirectoryMakeResponse [success=").append(success).append(", ");
    if (request != null)
      builder.append("request=").append(request);
    builder.append("]");

    return builder.toString();
  }

}