package com.wstrater.server.fileSync.common.hash;

import java.io.File;

public class HashEvent {

  public enum EventType {
    Starting, StartingDirectory, StartingFile, HashingFile, FinishedFile, FinishedDirectory, HashingError, Failed, Done
  }

  private EventType event;
  private String    message;
  private int       progress;
  private String    requestId;

  public HashEvent(EventType event, File file, int progress, String requestId) {
    super();
    this.event = event;
    if (file != null) {
      this.message = file.getAbsolutePath();
    }
    this.progress = progress;
    this.requestId = requestId;
  }

  public HashEvent(EventType event, String message, int progress, String requestId) {
    super();
    this.event = event;
    this.message = message;
    this.progress = progress;
    this.requestId = requestId;
  }

  public EventType getEvent() {
    return event;
  }

  public String getMessage() {
    return message;
  }

  public int getProgress() {
    return progress;
  }

  public static HashEvent newProgress(EventType event, File file, int current, int max, String requestId) {
    return new HashEvent(event, file, (int) (current * 100 / max), requestId);
  }

  public static HashEvent newProgress(EventType event, File file, long current, long max, String requestId) {
    return new HashEvent(event, file, (int) (current * 100L / max), requestId);
  }

  public void setEvent(EventType event) {
    this.event = event;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public void setProgress(int progress) {
    this.progress = progress;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("HashEvent [");
    if (event != null)
      builder.append("event=").append(event).append(", ");
    if (requestId != null)
      builder.append("requestId=").append(requestId).append(", ");
    builder.append("progress=").append(progress).append(", ");
    if (message != null)
      builder.append("message=").append(message);
    builder.append("]");

    return builder.toString();
  }

}