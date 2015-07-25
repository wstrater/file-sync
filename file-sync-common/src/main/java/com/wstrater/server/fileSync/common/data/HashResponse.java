package com.wstrater.server.fileSync.common.data;

public class HashResponse {

  private boolean     queued;
  private HashRequest request;

  public boolean isQueued() {
    return queued;
  }

  public HashRequest getRequest() {
    return request;
  }

  public void setQueued(boolean success) {
    this.queued = success;
  }

  public void setRequest(HashRequest request) {
    this.request = request;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("HashResponse [queued=").append(queued).append(", ");
    if (request != null)
      builder.append("request=").append(request);
    builder.append("]");

    return builder.toString();
  }

}