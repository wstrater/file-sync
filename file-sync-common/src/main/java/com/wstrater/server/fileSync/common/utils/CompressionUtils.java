package com.wstrater.server.fileSync.common.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

import com.wstrater.server.fileSync.common.exceptions.ErrorDeflatingBlockException;
import com.wstrater.server.fileSync.common.exceptions.ErrorInflatingBlockException;

/**
 * @author wstrater
 *
 */
public abstract class CompressionUtils {

  public final static int  DEFAULT_LEVEL = Deflater.BEST_COMPRESSION;
  private final static int EXTRA_BYTES   = 256;

  /**
   * Deflate with highest compression, {@link Deflater.BEST_COMPRESSION}, data using ZLib
   * implementation.
   * 
   * @param data
   * @return
   */
  public static Deflated deflate(byte[] data) {
    return deflate(data, DEFAULT_LEVEL);
  }

  /**
   * Deflate data using ZLib implementation.
   * 
   * @param data
   * @param level
   * @return
   */
  public static Deflated deflate(byte[] data, int level) {
    valdiate(data, 0, 0);

    return deflate(data, 0, data.length, level);
  }

  /**
   * Deflate with highest compression, {@link Deflater.BEST_COMPRESSION}, data using ZLib
   * implementation.
   * 
   * @param data
   * @param offset
   * @param length
   * @return
   */
  public static Deflated deflate(byte[] data, int offset, int length) {
    return deflate(data, offset, length, DEFAULT_LEVEL);
  }

  /**
   * Deflate data using ZLib implementation.
   * 
   * @param data
   * @param offset
   * @param length
   * @param level
   * @return
   */
  public static Deflated deflate(byte[] data, int offset, int length, int level) {
    Deflated ret = new Deflated();

    valdiate(data, offset, length);

    Deflater deflater = new Deflater(level);
    deflater.setInput(data, offset, length);
    deflater.finish();

    byte[] buf = new byte[length + EXTRA_BYTES];
    ret.setLength(deflater.deflate(buf));
    deflater.end();
    ret.setData(new byte[ret.getLength()]);
    System.arraycopy(buf, 0, ret.getData(), 0, ret.getLength());

    return ret;
  }

  /**
   * Inflate data compressed using ZLib deflation.
   * 
   * @param data
   * @param inflatedLength This is the expected output size. Used for allocating memory for inflated
   *          data.
   * @return
   */
  public static Inflated inflate(byte[] data, int inflatedLength) {
    valdiate(data, 0, 0);

    return inflate(data, 0, data.length, inflatedLength);
  }

  /**
   * Inflate data compressed using ZLib deflation.
   * 
   * @param data
   * @param offset
   * @param length
   * @param inflatedLength This is the expected output size. Used for allocating memory for inflated
   *          data.
   * @return
   */
  public static Inflated inflate(byte[] data, int offset, int length, int inflatedLength) {
    Inflated ret = new Inflated();

    valdiate(data, offset, length);

    Inflater inflater = new Inflater();
    inflater.setInput(data, offset, length);

    byte[] buf = new byte[inflatedLength + EXTRA_BYTES];
    try {
      ret.setLength(inflater.inflate(buf));
      inflater.end();
      ret.setData(new byte[ret.getLength()]);
      System.arraycopy(buf, 0, ret.getData(), 0, ret.getLength());
    } catch (DataFormatException ee) {
      throw new ErrorInflatingBlockException("Error inflating block", ee);
    }

    return ret;
  }

  /**
   * Validate the data parameters. Throws a {@link IndexOutOfBoundsException}.
   * 
   * @param data
   * @param offset
   * @param length
   */
  static void valdiate(byte[] data, int offset, int length) {
    if ((data == null) || (offset < 0) || (offset > data.length) || (length < 0) || ((offset + length) - data.length > 0)) {
      throw new IndexOutOfBoundsException();
    }
  }

  /**
   * Unzip data compressed using GZip.
   * 
   * @param data
   * @param inflatedLength This is the expected output size. Used for allocating memory for unzipped
   *          data.
   * @return
   */
  public static Inflated unzip(byte[] data, int inflatedLength) {
    valdiate(data, 0, 0);

    return unzip(data, 0, data.length, inflatedLength);
  }

  /**
   * Unzip data compressed using GZip.
   * 
   * @param data
   * @param offset
   * @param length
   * @param inflatedLength This is the expected output size. Used for allocating memory for unzipped
   *          data.
   * @return
   */
  public static Inflated unzip(byte[] data, int offset, int length, int inflatedLength) {
    Inflated ret = new Inflated();

    valdiate(data, offset, length);

    try {
      ByteArrayInputStream bis = new ByteArrayInputStream(data, offset, length);
      try {
        GZIPInputStream gis = new GZIPInputStream(bis);
        try {
          byte[] buf = new byte[inflatedLength + EXTRA_BYTES];
          int off = 0, len = 0;
          while ((len = gis.read(buf, off, buf.length - off)) >= 0) {
            off += len;
            ret.setLength(ret.getLength() + len);
          }
          ret.setData(new byte[ret.getLength()]);
          System.arraycopy(buf, 0, ret.getData(), 0, ret.getLength());
        } finally {
          gis.close();
        }
      } finally {
        bis.close();
      }
    } catch (IOException ee) {
      throw new ErrorDeflatingBlockException("Error unzipping block", ee);
    }

    return ret;
  }

  /**
   * Zip data using GZip.
   * 
   * @param data
   * @return
   */
  public static Deflated zip(byte[] data) {
    valdiate(data, 0, 0);

    return zip(data, 0, data.length);
  }

  /**
   * Zip data using GZip.
   * 
   * @param data
   * @param offset
   * @param length
   * @return
   */
  public static Deflated zip(byte[] data, int offset, int length) {
    Deflated ret = new Deflated();

    valdiate(data, offset, length);

    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(length + EXTRA_BYTES);
      try {
        GZIPOutputStream gos = new GZIPOutputStream(bos);
        try {
          gos.write(data, offset, length);
        } finally {
          gos.close();
        }
      } finally {
        bos.close();
      }
      ret.setData(bos.toByteArray());
      ret.setLength(ret.getData().length);
    } catch (IOException ee) {
      throw new ErrorDeflatingBlockException("Error ziping block", ee);
    }

    return ret;
  }

  public static class Deflated {

    private byte[] data;
    private int    length;

    public byte[] getData() {
      return data;
    }

    public int getLength() {
      return length;
    }

    public void setData(byte[] data) {
      this.data = data;
    }

    public void setLength(int length) {
      this.length = length;
    }

  }

  public static class Inflated {

    private byte[] data;
    private int    length;

    public byte[] getData() {
      return data;
    }

    public int getLength() {
      return length;
    }

    public void setData(byte[] data) {
      this.data = data;
    }

    public void setLength(int length) {
      this.length = length;
    }

  }

}