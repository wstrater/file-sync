package com.wstrater.server.fileSync.server.handlers;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wstrater.server.fileSync.server.HttpUtils;

public class HelloWorldHandler extends AbstractHandler {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private boolean        ssl    = false;

  public HelloWorldHandler(boolean ssl) {
    this.ssl = ssl;
  }

  public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    logger.info("Target: {}", target);
    HttpUtils.dump(request);

    response.setContentType("text/html;charset=utf-8");
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().println("<h1>Hello World</h1>");
    baseRequest.setHandled(true);
  }

}