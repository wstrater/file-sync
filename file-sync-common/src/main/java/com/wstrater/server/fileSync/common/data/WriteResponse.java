package com.wstrater.server.fileSync.common.data;

/**
 * The response to a {@see WriteRequest}.
 * 
 * @author wstrater
 *
 */
public class WriteResponse {

  private long         crc32;
  private int          length;
  private WriteRequest request;
  private boolean      success;

  public long getCrc32() {
    return crc32;
  }

  public int getLength() {
    return length;
  }

  public WriteRequest getRequest() {
    return request;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setCrc32(long crc32) {
    this.crc32 = crc32;
  }

  public void setLength(int length) {
    this.length = length;
  }

  public void setRequest(WriteRequest request) {
    this.request = request;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("WriteResponse [crc32=").append(crc32).append(", length=").append(length).append(", success=").append(success)
        .append(", ");
    if (request != null)
      builder.append("request=").append(request);
    builder.append("]");

    return builder.toString();
  }

}