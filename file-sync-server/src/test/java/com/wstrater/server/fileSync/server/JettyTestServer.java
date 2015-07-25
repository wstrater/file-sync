package com.wstrater.server.fileSync.server;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used for testing Jetty SSL and authentication. It uses the {@link JerseyTestClient}
 * .
 * 
 * @author wstrater
 *
 */
public class JettyTestServer {

  private final static String   KEYSTORE_FILE   = "server-keystore.jks";
  private final static String   KEYSTORE_PASS   = "secret";

  private final static String   TRUSTSTORE_FILE = "server-truststore.jks";
  private final static String   TRUSTSTORE_PASS = "secret";

  private final static String   REALM           = "file-sync-server";

  private final static String   USER_NAME       = "wstrater";
  private final static String   USER_PASS       = "CRYPT:wsusM69Yr/nVw";

  private final static String   ADMIN_ROLE      = "admin";
  private final static String   USER_ROLE       = "user";

  protected final static Logger logger          = LoggerFactory.getLogger(JettyTestServer.class);

  public void run(boolean ssl, boolean needClientAuth, boolean basicAuth) throws Exception {
    Connector connector = null;
    if (ssl) {
      SslContextFactory sslContextFactory = new SslContextFactory(KEYSTORE_FILE);
      sslContextFactory.setKeyStorePassword(KEYSTORE_PASS);
      sslContextFactory.setTrustStore(TRUSTSTORE_FILE);
      sslContextFactory.setTrustStorePassword(TRUSTSTORE_PASS);
      sslContextFactory.setNeedClientAuth(needClientAuth);

      connector = new SslSocketConnector(sslContextFactory);
      connector.setPort(8443);
    } else {
      connector = new SelectChannelConnector();
      connector.setPort(8080);
    }

    Server server = new Server();
    server.setConnectors(new Connector[] { connector });

    ServletContextHandler servletContext = new ServletContextHandler(server, "/");
    servletContext.addServlet(HelloWorldServlet.class, "/");

    if (basicAuth) {
      servletContext.setSecurityHandler(getSecurityHandler());
    }

    server.start();
    server.join();
  }

  private LoginService getHashLoginService() {
    HashLoginService ret = new HashLoginService();

    ret.setName(REALM);
    ret.setRefreshInterval(0);

    try {
      File file = File.createTempFile(String.format("%s-users-", REALM), ".properties");
      file.deleteOnExit();
      PrintWriter writer = new PrintWriter(file);
      try {
        writer.println(String.format("%s:%s,%s", USER_NAME, USER_PASS, USER_ROLE));
      } finally {
        writer.close();
      }
      ret.setConfig(file.getAbsolutePath());
    } catch (IOException ee) {
      ret.putUser(USER_NAME, Credential.getCredential(USER_PASS), new String[] { USER_ROLE });
    }

    return ret;
  }

  private SecurityHandler getSecurityHandler() {
    ConstraintSecurityHandler ret = new ConstraintSecurityHandler();

    Constraint constraint = new Constraint(Constraint.__BASIC_AUTH, USER_ROLE);
    constraint.setAuthenticate(true);
    constraint.setRoles(new String[] { USER_ROLE, ADMIN_ROLE });

    ConstraintMapping constraintMapping = new ConstraintMapping();
    constraintMapping.setConstraint(constraint);
    constraintMapping.setPathSpec("/*");

    ret.setAuthenticator(new BasicAuthenticator());
    ret.setRealmName(REALM);
    ret.addConstraintMapping(constraintMapping);

    ret.setLoginService(getHashLoginService());

    return ret;
  }

  public static class HelloWorldServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      logger.info("Request: {}", request);
      HttpUtils.dump(request);

      response.setContentType("text/html;charset=utf-8");
      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().println("<h1>Hello World Servlet</h1>");
    }

  }

  public static void main(String[] args) throws Exception {
    JettyTestServer test = new JettyTestServer();

    test.run(true, true, true);
  }

}