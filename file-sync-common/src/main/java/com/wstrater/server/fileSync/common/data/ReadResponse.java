package com.wstrater.server.fileSync.common.data;


/**
 * The response of a {@see ReadRequest}
 * 
 * @author wstrater
 *
 */
public class ReadResponse {

  private long        crc32;
  private byte[]      data;
  private boolean     eof;
  private int         length;
  private ReadRequest request;
  private boolean     success;

  public long getCrc32() {
    return crc32;
  }

  public byte[] getData() {
    return data;
  }

  public boolean isEof() {
    return eof;
  }

  public int getLength() {
    return length;
  }

  public ReadRequest getRequest() {
    return request;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setCrc32(long crc32) {
    this.crc32 = crc32;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public void setEof(boolean eof) {
    this.eof = eof;
  }

  public void setLength(int length) {
    this.length = length;
  }

  public void setRequest(ReadRequest request) {
    this.request = request;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("ReadResponse [crc32=").append(crc32).append(", ");
    if (data != null)
      builder.append("data=").append(data.length).append(", ");
    builder.append("eof=").append(eof).append(", length=").append(length).append(", success=").append(success).append(", ");
    if (request != null)
      builder.append("request=").append(request);
    builder.append("]");

    return builder.toString();
  }

}