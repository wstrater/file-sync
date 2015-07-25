package com.wstrater.server.fileSync.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

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
import com.wstrater.server.fileSync.common.exceptions.FileSyncException;
import com.wstrater.server.fileSync.common.exceptions.UnableToConfigureSSLException;
import com.wstrater.server.fileSync.common.exceptions.UnableToLoadKeyStoreException;
import com.wstrater.server.fileSync.common.exceptions.UnableToLoadTrustStoreException;
import com.wstrater.server.fileSync.common.utils.Compare;
import com.wstrater.server.fileSync.common.utils.Constants;

/**
 * This is used to configure `Jersey` for accessing the remote server.
 * 
 * @author wstrater
 *
 */
public class RemoteClient {

  protected final static Logger logger          = LoggerFactory.getLogger(RemoteClient.class);

  private boolean               basicAuth;
  private Client                client;
  private boolean               closeConnection = true;
  private String                host;
  private HostnameVerifier      hostNameVerifier;
  private KeyManager[]          keyManagers;
  private File                  keyStoreFile;
  private transient String      keyStorePassword;
  private int                   port;
  private transient String      privateKeyPassword;
  private boolean               ssl;
  private SSLContext            sslContext;
  private TrustManager[]        trustManagers;
  private File                  trustStoreFile;
  private transient String      trustStorePassword;
  private String                userName;
  private transient String      userPassword;

  private RemoteClient() {}

  public static Builder builder() {
    return new Builder();
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void checkForException(ClientResponse clientResponse) {
    logger.debug(String.valueOf(clientResponse.getHeaders()));

    String message = clientResponse.getHeaders().getFirst(Constants.EXCEPT_MSG_HEADER);
    String className = clientResponse.getHeaders().getFirst(Constants.EXCEPT_CLASS_HEADER);

    if (Compare.isNotBlank(message) && Compare.isBlank(className)) {
      throw new FileSyncException(message);
    } else if (Compare.isNotBlank(message) || Compare.isNotBlank(className)) {
      try {
        Class clazz = Class.forName(className);
        Constructor<? extends RuntimeException> constructor = clazz.getConstructor(new Class[] { String.class });
        throw constructor.newInstance(new Object[] { message });
      } catch (ReflectiveOperationException ee) {
        throw new FileSyncException(message);
      }
    }
  }

  public void finished() {
    if (client != null) {
      client.destroy();
      client = null;
    }
  }

  /**
   * The Jersey client used to build a {@link WebResource}. Besure to call {@link Client#destroy()}
   * when done to clean up resources.
   * 
   * @return
   */
  public Client getClient() {
    if (client == null) {
      if (ssl) {
        ClientConfig config = new DefaultClientConfig();
        config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES,
            new HTTPSProperties(getHostNameVerifier(), getSSLContext()));

        client = Client.create(config);
      } else {
        client = new Client();
      }

      if (basicAuth) {
        client.addFilter(new HTTPBasicAuthFilter(userName, new Password(userPassword).toString()));
      }

      if (closeConnection) {
        client.addFilter(new ClientFilter() {

          /**
           * It appears that Jersey uses a keep-alive connection but does not close it. This leaves
           * it open on the server until it times out.
           */
          @Override
          public ClientResponse handle(ClientRequest clientRequest) throws ClientHandlerException {
            clientRequest.getHeaders().add("Connection", "close");
            return getNext().handle(clientRequest);
          }
        });
      }
    }

    return client;
  }

  private HostnameVerifier getHostNameVerifier() {
    if (hostNameVerifier == null) {
      hostNameVerifier = new HostnameVerifier() {

        @Override
        public boolean verify(String hostName, SSLSession session) {
          return true;
        }
      };
    }

    return hostNameVerifier;
  }

  private KeyManager[] getKeyManagers() {
    if (keyManagers == null) {

      try {
        KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        if (keyStoreFile != null) {
          keyFactory.init(getKeyStore(keyStoreFile, keyStorePassword), Credential.getCredential(privateKeyPassword).toString()
              .toCharArray());
        }

        keyManagers = keyFactory.getKeyManagers();
      } catch (GeneralSecurityException | IOException ee) {
        throw new UnableToLoadKeyStoreException(String.format("Unable to load key store: %s", ee.getMessage()), ee);
      }
    }

    return keyManagers;
  }

  private KeyStore getKeyStore(File file, String password) throws GeneralSecurityException, IOException {
    KeyStore ret = KeyStore.getInstance(KeyStore.getDefaultType());

    if (file != null) {
      InputStream in = new FileInputStream(file);
      try {
        ret.load(in, Credential.getCredential(password).toString().toCharArray());
      } finally {
        in.close();
      }
    }

    return ret;
  }

  private SSLContext getSSLContext() {
    if (sslContext == null) {
      try {
        sslContext = SSLContext.getInstance("TLS");

        sslContext.init(getKeyManagers(), getTrustManagers(), new SecureRandom());
      } catch (GeneralSecurityException ee) {
        throw new UnableToConfigureSSLException(String.format("Unable to configure SSL: %s", ee.getMessage()), ee);
      }
    }

    return sslContext;
  }

  private TrustManager[] getTrustManagers() {
    if (trustManagers == null) {
      try {
        TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        if (trustStoreFile != null) {
          trustFactory.init(getKeyStore(trustStoreFile, trustStorePassword));
        }

        trustManagers = trustFactory.getTrustManagers();
      } catch (GeneralSecurityException | IOException ee) {
        throw new UnableToLoadTrustStoreException(String.format("Unable to load trust store: %s", ee.getMessage()), ee);
      }

    }

    return trustManagers;
  }

  public String getURI(String path) {
    String ret = String.format("%s://%s:%d%s", ssl ? "https" : "http", host, port, path);

    logger.debug(String.format("URI: %s", ret));

    return ret;
  }

  /**
   * Allow building a {@link RemoteClient} with a flexible list of arguments that are easily
   * readable.
   * 
   * @author wstrater
   *
   */
  public static class Builder {

    private RemoteClient built = new RemoteClient();

    public RemoteClient build() {
      logger.info(String.format("Remote Host: %s",
          String.format("%s://%s:%d/", built.ssl ? "https" : "http", built.host, built.port)));
      return built;
    }

    public Builder basicAuth(boolean basicAuth) {
      built.basicAuth = basicAuth;
      return this;
    }

    public Builder closeConnection(boolean closeConnection) {
      built.closeConnection = closeConnection;
      return this;
    }

    public Builder host(String host) {
      built.host = host;
      return this;
    }

    public Builder keyStoreFile(File keyStoreFile) {
      built.keyStoreFile = keyStoreFile;
      return this;
    }

    public Builder keyStorePassword(String keyStorePassword) {
      built.keyStorePassword = keyStorePassword;
      return this;
    }

    public Builder port(int port) {
      built.port = port;
      return this;
    }

    public Builder privateKeyPassword(String privateKeyPassword) {
      built.privateKeyPassword = privateKeyPassword;
      return this;
    }

    public Builder ssl(boolean ssl) {
      built.ssl = ssl;
      return this;
    }

    public Builder trustStoreFile(File trustStoreFile) {
      built.trustStoreFile = trustStoreFile;
      return this;
    }

    public Builder trustStorePassword(String trustStorePassword) {
      built.trustStorePassword = trustStorePassword;
      return this;
    }

    public Builder userName(String userName) {
      built.userName = userName;
      return this;
    }

    public Builder userPassword(String userPassword) {
      built.userPassword = userPassword;
      return this;
    }

  }

}