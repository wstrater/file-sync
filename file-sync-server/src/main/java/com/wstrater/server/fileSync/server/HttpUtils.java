package com.wstrater.server.fileSync.server;

import java.util.Collections;
import java.util.TreeSet;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class for dumping HTTP request details.
 * 
 * @author wstrater
 *
 */
public abstract class HttpUtils {

  protected final static Logger logger = LoggerFactory.getLogger(HttpUtils.class);

  public static void dump(HttpServletRequest request) {
    if (logger.isDebugEnabled()) {
      logger.debug("==================== Request ====================");

      logger.debug("getAuthType=" + request.getAuthType());
      logger.debug("getContextPath=" + request.getContextPath());
      logger.debug("getMethod=" + request.getMethod());
      logger.debug("getPathInfo=" + request.getPathInfo());
      logger.debug("getProtocol=" + request.getProtocol());
      logger.debug("getQueryString=" + request.getQueryString());
      logger.debug("getRemoteAddr=" + request.getRemoteAddr());
      logger.debug("getRemoteHost=" + request.getRemoteHost());
      logger.debug("getRemotePort=" + request.getRemotePort());
      logger.debug("getRemoteUser=" + request.getRemoteUser());
      logger.debug("getRequestURI=" + request.getRequestURI());
      logger.debug("getRequestURL=" + request.getRequestURL());
      logger.debug("getScheme=" + request.getScheme());
      logger.debug("getServerName=" + request.getServerName());
      logger.debug("getServerPort=" + request.getServerPort());
      logger.debug("getUserPrincipal=" + request.getUserPrincipal());

      logger.debug("Headers:");
      for (String name : new TreeSet<String>(Collections.list(request.getHeaderNames()))) {
        Object value = request.getHeader(name);
        if (value != null) {
          logger.debug(name + "=" + String.valueOf(value));
        }
      }

      logger.debug("Attributes:");
      for (String name : new TreeSet<String>(Collections.list(request.getAttributeNames()))) {
        Object value = request.getAttribute(name);
        if (value != null) {
          logger.debug(name + "=" + String.valueOf(value));
        }
      }

      logger.debug("Parameters:");
      for (String name : new TreeSet<String>(Collections.list(request.getParameterNames()))) {
        String[] values = request.getParameterValues(name);
        if (values != null) {
          for (String value : values) {
            if (name.toLowerCase().indexOf("passw") >= 0) {
              logger.debug(name + "=" + (value == null || value.trim().length() < 1 ? "" : "********"));
            } else {
              logger.debug(name + "=" + value);
            }
          }
        }
      }

      logger.debug("Cookies:");
      Cookie[] cookies = request.getCookies();
      if (cookies != null) {
        for (Cookie cookie : cookies) {
          logger.debug(cookie.getName() + "=" + cookie.getValue() + " " + cookie.getPath() + " " + cookie.getMaxAge());
        }
      }

      logger.debug("-------------------- Request --------------------");
    }
  }

}
