package com.wstrater.server.fileSync.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

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
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.wstrater.server.fileSync.client.FileSyncClient;
import com.wstrater.server.fileSync.common.exceptions.FileSyncException;
import com.wstrater.server.fileSync.common.exceptions.InvalidUserFileException;
import com.wstrater.server.fileSync.common.exceptions.UnableToLoadKeyStoreException;
import com.wstrater.server.fileSync.common.exceptions.UnableToLoadTrustStoreException;
import com.wstrater.server.fileSync.common.hash.HashProcessor;
import com.wstrater.server.fileSync.common.utils.ChunkUtils;
import com.wstrater.server.fileSync.common.utils.CommandLineUtils;
import com.wstrater.server.fileSync.common.utils.Constants;
import com.wstrater.server.fileSync.common.utils.DirectoryUtils;
import com.wstrater.server.fileSync.common.utils.FileUtils;
import com.wstrater.server.fileSync.common.utils.TimeUtils;
import com.wstrater.server.fileSync.server.handlers.FileController;

/**
 * This is the remote server for file-sync and must be running and accessible for
 * {@link FileSyncClient} to function.
 * 
 * @author wstrater
 *
 */
public class FileSyncServer {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private LoginService getHashLoginService(File userFile) {
    HashLoginService ret = new HashLoginService();

    ret.setName(Constants.REALM);
    ret.setRefreshInterval(0);

    if (userFile == null) {
      throw new InvalidUserFileException("Missing user file");
    } else {
      if (userFile.canRead()) {
        ret.setConfig(userFile.getAbsolutePath());
      } else {
        throw new InvalidUserFileException(String.format("Unable to load user file: ", userFile.getAbsolutePath()));
      }
    }

    return ret;
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

  protected String getLocalHost() {
    String ret = "127.0.0.1";

    try {
      InetAddress localHost = InetAddress.getLocalHost();
      ret = localHost.getHostName();
      if (ret == null || ret.trim().length() < 1) {
        ret = localHost.getHostAddress();
      }
    } catch (UnknownHostException ee) {
      logger.error("Unable to get localhost", ee);
    }

    return ret;
  }

  private SecurityHandler getSecurityHandler(File userFile) {
    ConstraintSecurityHandler ret = new ConstraintSecurityHandler();

    Constraint constraint = new Constraint(Constraint.__BASIC_AUTH, Constants.USER_ROLE);
    constraint.setAuthenticate(true);
    constraint.setRoles(new String[] { Constants.USER_ROLE, Constants.ADMIN_ROLE });

    ConstraintMapping constraintMapping = new ConstraintMapping();
    constraintMapping.setConstraint(constraint);
    constraintMapping.setPathSpec("/*");

    ret.setAuthenticator(new BasicAuthenticator());
    ret.setRealmName(Constants.REALM);
    ret.addConstraintMapping(constraintMapping);

    ret.setLoginService(getHashLoginService(userFile));

    return ret;
  }

  public void run(String[] args) throws Exception {
    CommandLineUtils cli = new CommandLineUtils(getClass());
    cli.useServer().useAllow().useBaseDir().useBlockSize().useEncPass().useHashType().useKeyStore().useMax().useTimeZone()
        .useTrustStore().useUserFile();

    if (cli.parseArgs(args)) {
      if (cli.isHelp() || cli.hasHelp()) {
        cli.displayHelp();
      } else if (cli.hasEncPass()) {
        System.out.println(Password.obfuscate(cli.getEncPass()));
        System.out.println(Credential.MD5.digest(cli.getEncPass()));
        if (cli.hasEncUser()) {
          System.out.println(Credential.Crypt.crypt(cli.getEncUser(), cli.getEncPass()));
        }
      } else {
        ChunkUtils.setBlockSize(cli.getBlockSize());
        DirectoryUtils.setBaseDir(cli.getBaseDir());
        FileUtils.getPermissions().setLocalDelete(cli.isAllowDelete());
        FileUtils.getPermissions().setLocalWrite(cli.isAllowWrite());
        FileUtils.setMaxBlockSize(cli.getMaxBlock());
        FileUtils.setMaxOffset(cli.getMaxOffset());
        HashProcessor.setHashType(cli.getHashType());
        TimeUtils.setTimeZone(cli.getTimeZone());

        startServer(cli);
      }
    }
  }

  protected void startServer(CommandLineUtils cli) throws Exception {
    Connector connector = null;
    if (cli.hasSsl()) {
      SslContextFactory sslContextFactory = new SslContextFactory();
      if (cli.hasStoreFile()) {
        try {
          sslContextFactory.setKeyStore(getKeyStore(cli.getStoreFile(), cli.getStorePass()));
          if (cli.hasKeyPass()) {
            sslContextFactory.setKeyManagerPassword(new Password(cli.getKeyPass()).toString());
          }
        } catch (GeneralSecurityException | IOException ee) {
          throw new UnableToLoadKeyStoreException(String.format("Unable to load key store: %s", ee.getMessage()), ee);
        }
      }
      if (cli.hasTrustFile()) {
        try {
          sslContextFactory.setTrustStore(getKeyStore(cli.getTrustFile(), cli.getTrustPass()));
        } catch (GeneralSecurityException | IOException ee) {
          throw new UnableToLoadTrustStoreException(String.format("Unable to load trust store: %s", ee.getMessage()), ee);
        }
      }
      sslContextFactory.setNeedClientAuth(cli.getSsl().clientAuth());

      connector = new SslSocketConnector(sslContextFactory);
    } else {
      connector = new SelectChannelConnector();
    }
    connector.setPort(cli.getPort());

    Server server = new Server();
    server.addConnector(connector);

    ServletHolder servletHolder = new ServletHolder(ServletContainer.class);
    servletHolder.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
        "com.sun.jersey.api.core.PackagesResourceConfig");
    servletHolder.setInitParameter("com.sun.jersey.config.property.packages", FileController.class.getPackage().getName());
    servletHolder.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");

    ServletContextHandler servletContext = new ServletContextHandler(server, "/", ServletContextHandler.NO_SESSIONS);
    servletContext.addServlet(servletHolder, "/*");

    if (cli.hasUserFile()) {
      servletContext.setSecurityHandler(getSecurityHandler(cli.getUserFile()));
    }

    logger.info("Starting Server: {}:{}", getLocalHost(), cli.getPort());
    try {
      server.start();
      server.join();
    } finally {
      logger.info("Stopping Server: {}:{}", getLocalHost(), cli.getPort());
      server.destroy();
    }

    // HelloWorldHandler helloWorld = new HelloWorldHandler(ssl);
    // jetty.setHandler(helloWorld);

    logger.info("Starting Server: {}:{}", getLocalHost(), cli.getPort());
    try {
      server.start();
      server.join();
    } finally {
      logger.info("Stopping Server: {}:{}", getLocalHost(), cli.getPort());
      server.destroy();
    }
  }

  public static void main(String[] args) throws Exception {
    try {
      FileSyncServer server = new FileSyncServer();
      server.run(args);
    } catch (FileSyncException ee) {
      System.err.println(ee.getMessage());
    }
  }

}