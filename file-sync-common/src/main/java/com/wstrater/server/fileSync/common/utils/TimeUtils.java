package com.wstrater.server.fileSync.common.utils;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Used for converting to and from UTC. This is critical if the client and server are in different
 * time zones to ensure that both copies have the same logical time stamp.
 * 
 * @author wstrater
 *
 */
public abstract class TimeUtils {

  private static TimeZone timeZone = TimeZone.getDefault();

  public Calendar getCalendar() {
    return getCalendar(timeZone);
  }

  public Calendar getCalendar(TimeZone timeZone) {
    Calendar ret = null;

    if (timeZone == null) {
      throw new IllegalArgumentException(TimeUtils.class.getSimpleName() + " requires a " + TimeZone.class.getSimpleName());
    }

    ret = Calendar.getInstance(timeZone);

    return ret;
  }

  public static TimeZone getTimeZone() {
    return timeZone;
  }

  public static long fromUTC(long time) {
    return fromUTC(time, timeZone);
  }

  public static long fromUTC(long time, boolean dayLightSavings) {
    return fromUTC(time, timeZone, dayLightSavings);
  }

  public static long fromUTC(long time, TimeZone timeZone) {
    if (timeZone == null) {
      throw new IllegalArgumentException(TimeUtils.class.getSimpleName() + " requires a " + TimeZone.class.getSimpleName());
    }

    return fromUTC(time, timeZone, timeZone.useDaylightTime());
  }

  public static long fromUTC(long time, TimeZone timeZone, boolean dayLightSavings) {
    long ret = time;

    if (timeZone == null) {
      throw new IllegalArgumentException(TimeUtils.class.getSimpleName() + " requires a " + TimeZone.class.getSimpleName());
    }

    ret += timeZone.getRawOffset();
    if (dayLightSavings) {
      ret += timeZone.getDSTSavings();
    }

    return ret;
  }

  public static void setTimeZone(TimeZone timeZone) {
    if (timeZone == null) {
      throw new IllegalArgumentException(TimeUtils.class.getSimpleName() + " requires a " + TimeZone.class.getSimpleName());
    }

    TimeUtils.timeZone = timeZone;
  }

  public static long toUTC(long time) {
    return toUTC(time, timeZone);
  }

  public static long toUTC(long time, boolean dayLightSavings) {
    return toUTC(time, timeZone, dayLightSavings);
  }

  public static long toUTC(long time, TimeZone timeZone) {
    if (timeZone == null) {
      throw new IllegalArgumentException(TimeUtils.class.getSimpleName() + " requires a " + TimeZone.class.getSimpleName());
    }

    return toUTC(time, timeZone, timeZone.useDaylightTime());
  }

  public static long toUTC(long time, TimeZone timeZone, boolean dayLightSavings) {
    long ret = time;

    if (timeZone == null) {
      throw new IllegalArgumentException(TimeUtils.class.getSimpleName() + " requires a " + TimeZone.class.getSimpleName());
    }

    ret -= timeZone.getRawOffset();
    if (dayLightSavings) {
      ret -= timeZone.getDSTSavings();
    }

    return ret;
  }

}