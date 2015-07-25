package com.wstrater.server.fileSync.common.data;

public class DirectoryListResponse {

  private DirectoryInfo        directoryInfo;
  private DirectoryListRequest request;
  private boolean              success;

  public DirectoryInfo getDirectoryInfo() {
    return directoryInfo;
  }

  public DirectoryListRequest getRequest() {
    return request;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setDirectoryInfo(DirectoryInfo directoryInfo) {
    this.directoryInfo = directoryInfo;
  }

  public void setRequest(DirectoryListRequest request) {
    this.request = request;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("DirectoryListResponse [success=").append(success).append(", ");
    if (directoryInfo != null)
      builder.append("directoryInfo=").append(directoryInfo).append(", ");
    if (request != null)
      builder.append("request=").append(request);
    builder.append("]");

    return builder.toString();
  }

}