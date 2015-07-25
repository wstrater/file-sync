package com.wstrater.server.fileSync.common.data;

public class DeleteResponse {

  private DeleteRequest request;
  private boolean       success;

  public DeleteRequest getRequest() {
    return request;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setRequest(DeleteRequest request) {
    this.request = request;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("DeleteResponse [success=").append(success).append(", ");
    if (request != null)
      builder.append("request=").append(request);
    builder.append("]");

    return builder.toString();
  }

}