package com.wstrater.server.fileSync.common.data;

public class HashStatus {

  private boolean     done;
  private boolean     failed;
  private String      failureMessage;
  private HashRequest request;
  private boolean     started;

  public boolean isDone() {
    return done;
  }

  public boolean isFailed() {
    return failed;
  }

  public String getFailureMessage() {
    return failureMessage;
  }

  public HashRequest getRequest() {
    return request;
  }

  public boolean isStarted() {
    return started;
  }

  public void setDone(boolean done) {
    this.done = done;
  }

  public void setFailed(boolean failed) {
    this.failed = failed;
  }

  public void setFailureMessage(String failureMessage) {
    this.failureMessage = failureMessage;
  }

  public void setRequest(HashRequest request) {
    this.request = request;
  }

  public void setStarted(boolean started) {
    this.started = started;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("HashStatus [started=").append(started).append(", failed=").append(failed).append(", ");
    if (failureMessage != null)
      builder.append("failureMessage=").append(failureMessage).append(", ");
    builder.append("done=").append(done).append(", ");
    if (request != null)
      builder.append("request=").append(request);
    builder.append("]");

    return builder.toString();
  }

}