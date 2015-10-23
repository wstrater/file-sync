package com.wstrater.server.fileSync.common.data;

public class DirectoryPermissionsResponse {

  private boolean                     allowDelete;
  private boolean                     allowWrite;
  private DirectoryPermissionsRequest request;
  private boolean                     success;

  public boolean isAllowDelete() {
    return allowDelete;
  }

  public boolean isAllowWrite() {
    return allowWrite;
  }

  public DirectoryPermissionsRequest getRequest() {
    return request;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setAllowDelete(boolean allowDelete) {
    this.allowDelete = allowDelete;
  }

  public void setAllowWrite(boolean allowWrite) {
    this.allowWrite = allowWrite;
  }

  public void setRequest(DirectoryPermissionsRequest request) {
    this.request = request;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("DirectoryPermissionsResponse [success=").append(success).append(", allowDelete=").append(allowDelete)
        .append(", allowWrite=").append(allowWrite).append(", ");
    if (request != null)
      builder.append("request=").append(request);
    builder.append("]");
    return builder.toString();
  }

}