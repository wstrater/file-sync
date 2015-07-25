package com.wstrater.server.fileSync.common.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a wrapper for Base64 to hide the implementation details
 * 
 * @author wstrater
 *
 */
public abstract class Base64Utils {

  public final static String     CHUNK_DELIMITER    = System.lineSeparator();
  public final static int        CHUNK_SIZE         = 76;

  public final static String     WHITESPACE_PATTERN = "[\\s\\n\\r]";

  protected final static Pattern whiteSpacePattern  = Pattern.compile(WHITESPACE_PATTERN);

  public static String addWhiteSpace(String in) {
    String ret = null;

    if (in != null) {
      StringBuilder buf = new StringBuilder(in);
      int at = ((int) (in.length() - 1) / CHUNK_SIZE) * CHUNK_SIZE;
      while (at > 0) {
        buf.insert(at, CHUNK_DELIMITER);
        at -= CHUNK_SIZE;
      }
      ret = buf.toString();
    }

    return ret;
  }

  public static byte[] decodeAsBytes(String in) {
    byte[] ret = null;

    if (in != null) {
      org.apache.commons.codec.binary.Base64 impl = new org.apache.commons.codec.binary.Base64();
      ret = impl.decode(in);
    }

    return ret;
  }

  public static byte[] encodeAsBytes(byte[] in) {
    byte[] ret = null;

    if (in != null) {
      org.apache.commons.codec.binary.Base64 impl = new org.apache.commons.codec.binary.Base64();
      ret = impl.encode(in);
    }

    return ret;
  }

  public static String encodeAsString(byte[] in) {
    return encodeAsString(in, false);
  }

  public static String encodeAsString(byte[] in, boolean chunk) {
    String ret = null;

    if (in != null) {
      byte[] bytes = encodeAsBytes(in);
      if (bytes != null) {
        String encoded = new String(bytes);
        Matcher matcher = whiteSpacePattern.matcher(encoded);
        boolean found = matcher.find();
        if (found) {
          if (chunk) {
            ret = encoded.trim();
          } else {
            ret = stripWhiteSpace(encoded);
          }
        } else {
          if (chunk) {
            ret = addWhiteSpace(encoded);
          } else {
            ret = encoded;
          }
        }
      }
    }

    return ret;
  }

  public final static String stripWhiteSpace(String in) {
    String ret = null;

    if (in != null) {
      ret = whiteSpacePattern.matcher(in).replaceAll("");
    }

    return ret;
  }

}