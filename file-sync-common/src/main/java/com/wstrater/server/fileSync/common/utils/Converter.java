package com.wstrater.server.fileSync.common.utils;

import java.util.Arrays;
import java.util.TimeZone;
import java.util.regex.Pattern;

public abstract class Converter {

  private final static Pattern timeZonePattern = Pattern.compile("GMT[+-]\\d{1,2}:?\\d{0,2}");

  public static boolean parseBoolean(Object in) {
    return parseBoolean(in, false);
  }

  public static boolean parseBoolean(Object value, boolean def) {
    boolean ret = def;

    if (value != null) {
      if (value instanceof Boolean) {
        ret = ((Boolean) value).booleanValue();
      } else if (value instanceof Number) {
        ret = ((Number) value).longValue() != 0;
      } else {
        String stringValue = value.toString().trim();
        if ("false".equalsIgnoreCase(stringValue) || "f".equalsIgnoreCase(stringValue) || "no".equalsIgnoreCase(stringValue)
            || "n".equalsIgnoreCase(stringValue) || "off".equalsIgnoreCase(stringValue)) {
          ret = false;
        } else if ("true".equalsIgnoreCase(stringValue) || "t".equalsIgnoreCase(stringValue) || "yes".equalsIgnoreCase(stringValue)
            || "y".equalsIgnoreCase(stringValue) || "on".equalsIgnoreCase(stringValue)) {
          ret = true;
        } else {
          ret = parseInt(value, 0) != 0;
        }
      }
    }

    return ret;
  }

  public static double parseDouble(Object value) {
    return parseDouble(value, 0D);
  }

  public static double parseDouble(Object value, double def) {
    double ret = def;

    if (value instanceof Double) {
      ret = (Double) value;
    } else if (value instanceof Number) {
      ret = ((Number) value).doubleValue();
    } else {
      try {
        ret = Double.parseDouble(String.valueOf(value));
      } catch (NumberFormatException ee) {
      }
    }

    return ret;
  }

  public static float parseFloat(Object value) {
    return parseFloat(value, 0F);
  }

  public static float parseFloat(Object value, float def) {
    float ret = def;

    if (value instanceof Float) {
      ret = (Float) value;
    } else if (value instanceof Number) {
      ret = ((Number) value).floatValue();
    } else {
      try {
        ret = Float.parseFloat(String.valueOf(value));
      } catch (NumberFormatException ee) {
      }
    }

    return ret;
  }

  public static int parseInt(Object value) {
    return parseInt(value, 0);
  }

  public static int parseInt(Object value, int def) {
    int ret = def;

    if (value != null) {
      Integer temp = parseIntObj(value, null);
      if (temp != null) {
        ret = temp;
      }
    }

    return ret;
  }

  public static Integer parseIntObj(Object value) {
    return parseIntObj(value, null);
  }

  public static Integer parseIntObj(Object value, Integer def) {
    Integer ret = def;

    if (value != null) {
      if (value instanceof Number) {
        ret = ((Number) value).intValue();
      } else {
        try {
          ret = Integer.valueOf(value.toString());
        } catch (Throwable ee) {
        }
      }
    }

    return ret;
  }

  public static long parseLong(Object value) {
    return parseLong(value, 0);
  }

  public static long parseLong(Object value, long def) {
    long ret = def;

    if (value != null) {
      if (value instanceof Number) {
        ret = ((Number) value).longValue();
      } else {
        try {
          ret = Long.parseLong(value.toString());
        } catch (Throwable ee) {
        }
      }
    }

    return ret;
  }

  public static TimeZone parseTimeZone(String id) {
    TimeZone ret = null;

    String[] ids = TimeZone.getAvailableIDs();
    Arrays.sort(ids);
    if (timeZonePattern.matcher(id).matches() || Arrays.binarySearch(ids, id) >= 0) {
      ret = TimeZone.getTimeZone(id);
    } else {
      throw new IllegalArgumentException(String.format("Invalid TimeZone %s. Valid TimeZones: %s", id, Arrays.toString(ids)));
    }

    return ret;
  }

}