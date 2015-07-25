package com.wstrater.server.fileSync.common.data;

public class DirectoryDeleteResponse {

  private DirectoryDeleteRequest request;
  private boolean                success;

  public DirectoryDeleteRequest getRequest() {
    return request;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setRequest(DirectoryDeleteRequest request) {
    this.request = request;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("DirectoryDeleteResponse [success=").append(success).append(", ");
    if (request != null)
      builder.append("request=").append(request);
    builder.append("]");

    return builder.toString();
  }

}