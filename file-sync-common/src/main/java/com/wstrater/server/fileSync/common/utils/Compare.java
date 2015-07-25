package com.wstrater.server.fileSync.common.utils;

import java.nio.ByteBuffer;

public abstract class Compare {

  public static int compare(int value1, int value2) {
    int ret = 0;

    if (value1 < value2) {
      ret = -1;
    } else if (value1 > value2) {
      ret = 1;
    }

    return ret;
  }

  /*
   * Null compare equals or less than non-nulls.
   */
  public static int compare(Integer value1, Integer value2) {
    int ret = 0;

    if (value1 == null) {
      if (value2 != null) {
        ret = -1;
      }
    } else {
      if (value2 == null) {
        ret = 1;
      } else {
        ret = value1.compareTo(value2);
      }
    }

    return ret;
  }

  public static int compare(long value1, long value2) {
    int ret = 0;

    if (value1 < value2) {
      ret = -1;
    } else if (value1 > value2) {
      ret = 1;
    }

    return ret;
  }

  /*
   * Null compare equals or less than non-nulls.
   */
  public static int compare(Long value1, Long value2) {
    int ret = 0;

    if (value1 == null) {
      if (value2 != null) {
        ret = -1;
      }
    } else {
      if (value2 == null) {
        ret = 1;
      } else {
        ret = value1.compareTo(value2);
      }
    }

    return ret;
  }

  /*
   * Null compare equals or less than non-nulls.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static int compare(Comparable value1, Comparable value2) {
    int ret = 0;

    if (value1 == null) {
      if (value2 != null) {
        ret = -1;
      }
    } else {
      if (value2 == null) {
        ret = 1;
      } else {
        ret = value1.compareTo(value2);
      }
    }

    return ret;
  }

  public static boolean equals(byte[] value1, byte[] value2) {
    boolean ret = false;

    if (value1 == null && value2 == null) {
      ret = true;
    } else if (value1 != null && value2 != null && value1.length == value2.length) {
      ret = equals(value1, 0, value2, 0, value1.length);
    }

    return ret;
  }

  public static boolean equals(byte[] value1, int offset1, byte[] value2, int offset2, int len) {
    boolean ret = false;

    if (value1 == null && value2 == null) {
      ret = true;
    } else if (value1 != null && value2 != null && len >= 0 && offset1 >= 0 && offset1 + len <= value1.length && offset2 >= 0
        && offset2 + len <= value2.length) {
      ret = true;
      for (int xx = 0; ret && xx < len; xx++) {
        ret = value1[offset1 + xx] == value2[offset2 + xx];
      }
    }

    return ret;
  }

  /**
   * Compares the minimum number of remaining bytes left in the buffer. Moves the position after the
   * comparison.
   * 
   * @param buf1
   * @param buf2
   * @return
   */
  public static boolean equals(ByteBuffer buf1, ByteBuffer buf2) {
    boolean ret = false;

    if (buf1 == null && buf2 == null) {
      ret = true;
    } else if (buf1 != null && buf2 != null) {
      int count = Math.min(buf1.remaining(), buf2.remaining());
      byte[] arr1 = buf1.array();
      byte[] arr2 = buf2.array();

      ret = equals(arr1, buf1.position(), arr2, buf2.position(), count);

      buf1.position(buf1.position() + count);
      buf2.position(buf2.position() + count);
    }

    return ret;
  }

  /*
   * Nulls compare equals.
   */
  public static boolean equals(Integer value1, Integer value2) {
    return compare(value1, value2) == 0;
  }

  /*
   * Nulls compare equals.
   */
  public static boolean equals(Long value1, Long value2) {
    return compare(value1, value2) == 0;
  }

  /*
   * Nulls compare equals.
   */
  @SuppressWarnings("rawtypes")
  public static boolean equals(Comparable value1, Comparable value2) {
    return compare(value1, value2) == 0;
  }

  /*
   * Convenience method for maps that contain Strings but return Object.
   */
  public static boolean isBlank(Object value) {
    return value == null || (value instanceof String && isBlank((String) value));
  }

  public static boolean isBlank(String text) {
    return text == null || text.trim().length() < 1;
  }

  /*
   * Convenience method for maps that contain Strings but return Object.
   */
  public static boolean isNotBlank(Object value) {
    return value != null && value instanceof String && isNotBlank((String) value);
  }

  public static boolean isNotBlank(String text) {
    return !isBlank(text);
  }

}
