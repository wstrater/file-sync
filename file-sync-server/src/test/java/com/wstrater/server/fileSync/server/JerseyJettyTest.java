package com.wstrater.server.fileSync.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.wstrater.server.fileSync.common.utils.FileUtils;

/**
 * This is a combined class for testing Jersey/Jetty SSL.
 * 
 * @author wstrater
 *
 */
public class JerseyJettyTest {

  private final static String CLIENT_KEY_PASS        = "secret";
  private final static String CLIENT_KEYSTORE_FILE   = "../file-sync-client/client-keystore.jks";
  private final static String CLIENT_KEYSTORE_PASS   = "secret";

  private final static String CLIENT_TRUSTSTORE_FILE = "../file-sync-client/client-truststore.jks";
  private final static String CLIENT_TRUSTSTORE_PASS = "secret";

  private final static String SERVER_KEYSTORE_FILE   = "server-keystore.jks";
  private final static String SERVER_KEYSTORE_PASS   = "secret";

  private final static String SERVER_TRUSTSTORE_FILE = "server-truststore.jks";
  private final static String SERVER_TRUSTSTORE_PASS = "secret";

  private final static String USER_NAME              = "wstrater";
  private final static String USER_PASS              = "password";

  private final static Logger LOG                    = Log.getLogger(JerseyJettyTest.class);

  private KeyStore getKeyStore(String fileName, String password) throws GeneralSecurityException, IOException {
    KeyStore ret = KeyStore.getInstance(KeyStore.getDefaultType());

    if (fileName != null) {
      File file = FileUtils.canonicalFile(new File(fileName));
      if (file.canRead()) {
        InputStream in = new FileInputStream(file);
        try {
          ret.load(in, new Password(password).toString().toCharArray());
        } finally {
          in.close();
        }
      }
    }

    return ret;
  }

  public void runClient(boolean ssl, boolean basicAuth) throws Exception {
    Client client = null;
    if (ssl) {
      TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustFactory.init(getKeyStore(CLIENT_TRUSTSTORE_FILE, CLIENT_TRUSTSTORE_PASS));
      TrustManager[] trustManagers = trustFactory.getTrustManagers();

      KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyFactory.init(getKeyStore(CLIENT_KEYSTORE_FILE, CLIENT_KEYSTORE_PASS), CLIENT_KEY_PASS.toCharArray());
      KeyManager[] keyManagers = keyFactory.getKeyManagers();

      HostnameVerifier hostNameVerifier = new HostnameVerifier() {

        @Override
        public boolean verify(String hostName, SSLSession session) {
          return true;
        }

      };

      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(keyManagers, trustManagers, new SecureRandom());

      ClientConfig config = new DefaultClientConfig();
      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hostNameVerifier, sslContext));

      client = Client.create(config);
    } else {
      client = new Client();
    }

    client.addFilter(new CloseConnectionFilter());

    if (basicAuth) {
      client.addFilter(new HTTPBasicAuthFilter(USER_NAME, Credential.getCredential(USER_PASS).toString().toString()));
    }

    try {
      LOG.debug(String.format("*****Client***** - Starting"));
      String uri = String.format("%s://localhost:%d/", ssl ? "https" : "http", ssl ? 8443 : 8080);
      WebResource webResource = client.resource(uri);
      LOG.debug(String.format("*****Client***** - Resource: %s", webResource));

      for (int xx = 0; xx < 3; xx++) {
        ClientResponse clientResponse = webResource.get(ClientResponse.class);
        LOG.debug(String.format("*****Client***** - Status: %s/%s", clientResponse.getStatus(), clientResponse.getStatusInfo()));
        LOG.debug(String.format("*****Client***** - Response: %s", clientResponse.getEntity(String.class)));
        LOG.debug(String.format("*****Client***** - Done"));
      }
    } finally {
      client.destroy();
    }
  }

  public Server startServer(boolean ssl, boolean needClientAuth) throws Exception {
    Server ret = null;

    Connector connector = null;
    if (ssl) {
      SslContextFactory sslContextFactory = new SslContextFactory(SERVER_KEYSTORE_FILE);
      sslContextFactory.setKeyStorePassword(SERVER_KEYSTORE_PASS);
      sslContextFactory.setTrustStore(SERVER_TRUSTSTORE_FILE);
      sslContextFactory.setTrustStorePassword(SERVER_TRUSTSTORE_PASS);
      sslContextFactory.setNeedClientAuth(needClientAuth);

      connector = new SslSocketConnector(sslContextFactory);
      connector.setPort(8443);
    } else {
      connector = new SelectChannelConnector();
      connector.setPort(8080);
    }

    ret = new Server();
    ret.setConnectors(new Connector[] { connector });

    ServletContextHandler servletContext = new ServletContextHandler(ret, "/");
    servletContext.addServlet(HelloWorldServlet.class, "/");

    LOG.debug(String.format("*****Server***** - Starting"));
    ret.start();
    // server.join();

    return ret;
  }

  private void runTest(boolean ssl, boolean needClientAuth, boolean basicAuth) throws Exception {
    Server server = startServer(ssl, needClientAuth);
    try {
      runClient(ssl, basicAuth);
    } finally {
      LOG.debug(String.format("*****Server***** - Stopping"));
      server.stop();
      LOG.debug(String.format("*****Server***** - Stopped"));
      server.destroy();
      LOG.debug(String.format("*****Server***** - Destroyed"));
    }
  }

  private class CloseConnectionFilter extends ClientFilter {

    @Override
    public ClientResponse handle(ClientRequest clientRequest) throws ClientHandlerException {
      clientRequest.getHeaders().add("Connection", "close");
      return getNext().handle(clientRequest);
    }

  }

  public static class HelloWorldServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      LOG.debug(String.format("*****Servlet***** - Starting"));

      // HttpUtils.dump(request);

      LOG.debug(String.format("*****Servlet***** - getRequestURL:   %s", request.getRequestURL()));
      LOG.debug(String.format("*****Servlet***** - getRemotePort:   %s", request.getRemotePort()));
      LOG.debug(String.format("*****Servlet***** - Authorization:   %s", request.getHeader("Authorization")));
      LOG.debug(String.format("*****Servlet***** - SSL Session Id:  %s",
          request.getAttribute("javax.servlet.request.ssl_session_id")));

      X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
      if (certs != null && certs.length > 0) {
        LOG.debug(String.format("*****Servlet***** - X509Certificate: %s", certs[0].getSubjectDN().getName()));
      }

      response.setContentType("text/html;charset=utf-8");
      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().println("<h1>Hello World Servlet</h1>");

      LOG.debug(String.format("*****Servlet***** - Done"));
    }

  }

  public static void main(String[] args) throws Exception {
    new JerseyJettyTest().runTest(true, true, true);
  }

}