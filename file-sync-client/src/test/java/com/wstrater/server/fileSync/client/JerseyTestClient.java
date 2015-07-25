package com.wstrater.server.fileSync.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.util.security.Password;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * This class is used for testing Jersey SSL and authentication. 
 * It uses the {@link JettyTestClient}.
 * 
 * @author wstrater
 *
 */
public class JerseyTestClient {

  private final static String   KEY_PASS        = "OBF:1yta1t331v8w1v9q1t331ytc";
  private final static String   KEYSTORE_FILE   = "client-keystore.jks";
  private final static String   KEYSTORE_PASS   = "secret";

  private final static String   TRUST_HOST      = null;
  private final static String   TRUSTSTORE_FILE = "client-truststore.jks";
  private final static String   TRUSTSTORE_PASS = "secret";

  private final static String   USER_NAME       = "wstrater";
  private final static String   USER_PASS       = "password";
  // private final static String USER_PASS = "OBF:1v2j1uum1xtv1zej1zer1xtn1uvk1v1v";

  protected final static Logger logger          = LoggerFactory.getLogger(JerseyTestClient.class);

  public void run(boolean ssl, boolean basicAuth) throws Exception {
    String uri = String.format("%s://localhost:%d/", ssl ? "https" : "http", ssl ? 8443 : 8080);

    logger.debug(String.format("URI: %s", uri));

    Client client = null;
    if (ssl) {
      ClientConfig config = new DefaultClientConfig();
      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES,
          new HTTPSProperties(getHostNameVerifier(), getSSLContext()));

      client = Client.create(config);
    } else {
      client = new Client();
    }

    if (basicAuth) {
      client.addFilter(new HTTPBasicAuthFilter(USER_NAME, new Password(USER_PASS).toString()));
      client.addFilter(new CloseConnectionFilter());
    }

    try {
      WebResource webResource = client.resource(uri);
      logger.debug(String.format("Resource: %s", webResource));
      ClientResponse clientResponse = webResource.get(ClientResponse.class);
      logger.debug(String.format("Status: %s/%s", clientResponse.getStatus(), clientResponse.getStatusInfo()));
      logger.debug(clientResponse.getEntity(String.class));
    } finally {
      client.destroy();
    }
  }

  private HostnameVerifier getHostNameVerifier() {
    return new HostnameVerifier() {

      @Override
      public boolean verify(String hostName, SSLSession session) {
        boolean ret = false;

        logger.debug(String.format("verify(%s, %s) = %s", hostName, logSession(session), TRUST_HOST));

        if (TRUST_HOST == null) {
          ret = true;
        } else {

        }

        return ret;
      }

      private String logSession(SSLSession session) {
        StringBuilder ret = new StringBuilder();

        if (session != null) {
          ret.append("(");
          ret.append(session.getPeerHost());
          ret.append(")");
        }

        return ret.toString();
      }

    };

  }

  private KeyManager[] getKeyManagers() throws GeneralSecurityException {
    KeyManager[] ret = null;

    KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    try {
      keyFactory.init(getKeyStore(KEYSTORE_FILE, KEYSTORE_PASS), Credential.getCredential(KEY_PASS).toString().toCharArray());
    } catch (IOException ee) {
      throw new GeneralSecurityException("Unable to load key store", ee);
    }

    ret = keyFactory.getKeyManagers();

    if (ret != null && ret.length > 0 && ret[0] instanceof X509KeyManager) {
      // Wrap for logging.
      final X509KeyManager baseManager = (X509KeyManager) ret[0];
      ret[0] = new X509KeyManager() {

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
          logger.debug(String.format("KeyManager.chooseClientAlias(%s, %s, %s)", Arrays.toString(keyType), logPrincipals(issuers),
              logSocket(socket)));
          return baseManager.chooseClientAlias(keyType, issuers, socket);
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
          logger.debug(String
              .format("KeyManager.chooseServerAlias(%s, %s, %s)", keyType, logPrincipals(issuers), logSocket(socket)));
          return baseManager.chooseServerAlias(keyType, issuers, socket);
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
          logger.debug(String.format("KeyManager.getCertificateChain(%s)", alias));
          return baseManager.getCertificateChain(alias);
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
          logger.debug(String.format("KeyManager.getClientAliases(%s, %s)", keyType, logPrincipals(issuers)));
          return baseManager.getClientAliases(keyType, issuers);
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
          logger.debug(String.format("KeyManager.getPrivateKey(%s)", alias));
          return baseManager.getPrivateKey(alias);
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
          logger.debug(String.format("KeyManager.getServerAliases(%s, %s)", keyType, logPrincipals(issuers)));
          return baseManager.getServerAliases(keyType, issuers);
        }

        private String logPrincipals(Principal[] principals) {
          StringBuilder ret = new StringBuilder();

          ret.append("(");
          if (principals != null) {
            boolean first = true;
            for (Principal principal : principals) {
              if (first) {
                first = false;
              } else {
                ret.append(", ");
              }
              ret.append(principal.getName());
            }
          }
          ret.append(")");

          return ret.toString();
        }

        private String logSocket(Socket socket) {
          StringBuilder ret = new StringBuilder();

          ret.append("(");
          if (socket != null) {
            ret.append(socket.getInetAddress().getHostName()).append(",");
            ret.append(socket.getLocalAddress().getHostName()).append(",");
            ret.append(socket.getLocalPort()).append(",");
            ret.append(socket.getLocalSocketAddress()).append(",");
            ret.append(socket.getPort()).append(",");
            ret.append(socket.getRemoteSocketAddress());
          }
          ret.append(")");

          return ret.toString();
        }
      };
    }

    return ret;
  }

  private KeyStore getKeyStore(String fileName, String password) throws GeneralSecurityException, IOException {
    KeyStore ret = KeyStore.getInstance(KeyStore.getDefaultType());

    if (fileName != null) {
      File file = FileUtils.canonicalFile(new File(fileName));
      if (file.canRead()) {
        InputStream in = new FileInputStream(file);
        try {
          ret.load(in, Credential.getCredential(password).toString().toCharArray());
        } finally {
          in.close();
        }
      }
    }

    return ret;
  }

  private SSLContext getSSLContext() throws GeneralSecurityException {
    SSLContext ret = SSLContext.getInstance("TLS");

    ret.init(getKeyManagers(), getTrustManagers(), new SecureRandom());

    return ret;
  }

  private TrustManager[] getTrustManagers() throws GeneralSecurityException {
    TrustManager[] ret = null;

    TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    try {
      trustFactory.init(getKeyStore(TRUSTSTORE_FILE, TRUSTSTORE_PASS));
    } catch (IOException ee) {
      throw new GeneralSecurityException("Unable to load trust store", ee);
    }

    ret = trustFactory.getTrustManagers();

    if (ret != null && ret.length > 0 && ret[0] instanceof X509TrustManager) {
      // Wrap for logging.
      final X509TrustManager baseManager = (X509TrustManager) ret[0];
      ret[0] = new X509TrustManager() {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
          logger.debug(String.format("TrustManager.checkClientTrusted(%s)", authType));
          logChain(chain);
          baseManager.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
          logger.debug(String.format("TrustManager.checkServerTrusted(%s)", authType));
          logChain(chain);
          baseManager.checkServerTrusted(chain, authType);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
          logger.debug("TrustManager.getAcceptedIssuers()");
          X509Certificate[] ret = baseManager.getAcceptedIssuers();
          logChain(ret);
          return ret;
        }

        private void logChain(X509Certificate[] chain) {
          if (chain != null) {
            for (X509Certificate cert : chain) {
              logger.debug(String.format("TrustManager.X509Certificate subject: %s issuer: %s", cert.getSubjectDN().getName(), cert
                  .getIssuerDN().getName()));
            }
          }
        }

      };
    }

    return ret;
  }

  private class CloseConnectionFilter extends ClientFilter {

    @Override
    public ClientResponse handle(ClientRequest clientRequest) throws ClientHandlerException {
      clientRequest.getHeaders().add("Connection", "close");
      return getNext().handle(clientRequest);
    }

  }

  public static void main(String[] args) throws Exception {
    JerseyTestClient test = new JerseyTestClient();
    test.run(true, true);
  }

}