package com.wstrater.server.fileSync.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;

import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.wstrater.server.fileSync.common.data.Hello;
import com.wstrater.server.fileSync.common.utils.CommandLineUtils;

public class HelloWorldClient {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private String         host   = "localhost";
  private int            port   = 8080;
  private boolean        ssl    = false;

  private void getHello() throws Exception {
    Client client = Client.create();

    String name = urlEncode("wes");

    // Get
    String uri = getURI(String.format("/hello/world/%s", name));
    WebResource webResource = client.resource(uri);
    ClientResponse response = webResource.accept("application/json").get(ClientResponse.class);
    if (response.getStatus() != HttpStatus.OK_200) {
      throw new RuntimeException(String.format("Failed GET %s: %d/%s", uri, response.getStatus(), response.getStatusInfo()));
    }

    // String output = response.getEntity(String.class);
    Hello hello = response.getEntity(Hello.class);
    logger.info(String.format("Hello: %s", hello.toString()));
    response.close();

    /*
     * // Post webResource = client.resource("http://localhost:9999/employee/postEmployee");
     * response = webResource.accept("application/json").post(ClientResponse.class, hello); if
     * (response.getStatus() != HttpStatus.OK_200 && response.getStatus() !=
     * HttpStatus.NO_CONTENT_204) { throw new RuntimeException("Failed : HTTP error code : " +
     * response.getStatus() + "/" + response.getStatusInfo()); }
     */
  }

  private void getFile1() throws Exception {
    Client client = Client.create();

    String name = "wes/strater.dat";

    // Get
    String uri = getURI(String.format("/hello/file1/%s", name));
    WebResource webResource = client.resource(uri).queryParam("block", String.valueOf(11)).queryParam("size", String.valueOf(8192));
    ClientResponse response = webResource.accept("application/json").get(ClientResponse.class);
    if (response.getStatus() != HttpStatus.OK_200) {
      throw new RuntimeException(String.format("Failed GET %s: %d/%s", uri, response.getStatus(), response.getStatusInfo()));
    }

    // String output = response.getEntity(String.class);
    String hello = response.getEntity(String.class);
    logger.info(String.format("Hello File: %s", hello.toString()));
    response.close();
  }

  private void getFile2() throws Exception {
    Client client = Client.create();

    String name = "wes/strater.dat";

    // Get
    String uri = getURI(String.format("/hello/file2/%s", name));
    WebResource webResource = client.resource(uri).queryParam("block", String.valueOf(11)).queryParam("size", String.valueOf(8192));
    ClientResponse response = webResource.accept("application/json").put(ClientResponse.class, name.getBytes());
    if (response.getStatus() != HttpStatus.OK_200) {
      throw new RuntimeException(String.format("Failed GET %s: %d/%s", uri, response.getStatus(), response.getStatusInfo()));
    }

    // String output = response.getEntity(String.class);
    byte[] hello = response.getEntity(byte[].class);
    logger.info(String.format("Hello File: %s", Arrays.toString(hello)));
    response.close();
  }

  private void getTest1() throws Exception {
    Client client = Client.create();

    String name = "wes/strater";

    // Get
    String uri = getURI(String.format("/hello/test1/%s", name));
    WebResource webResource = client.resource(uri);
    ClientResponse response = webResource.accept("application/json").get(ClientResponse.class);
    if (response.getStatus() != HttpStatus.OK_200) {
      throw new RuntimeException(String.format("Failed GET %s: %d/%s", uri, response.getStatus(), response.getStatusInfo()));
    }

    // String output = response.getEntity(String.class);
    Hello hello = response.getEntity(Hello.class);
    logger.info(String.format("Hello: %s", hello.toString()));
    response.close();
  }

  protected String urlEncode(String text) {
    String ret = text;

    try {
      ret = URLEncoder.encode(text, "UTF-8");
    } catch (UnsupportedEncodingException ee) {
      logger.error(String.format("Error URL encoding: %s", text), ee);
    }

    return ret;
  }

  protected String getURI(String path) {
    String ret = String.format("%s://%s:%d%s", ssl ? "https" : "http", host, port, path);

    logger.info(String.format("URI: %s", ret));

    return ret;
  }

  private void run(String[] args) throws Exception {
    CommandLineUtils cli = new CommandLineUtils(getClass());
    cli.useClient();

    if (cli.parseArgs(args)) {
      if (cli.isHelp()) {
        cli.displayHelp();
      } else {
        host = cli.getHost();
        port = cli.getPort();
        ssl = cli.hasSsl();

        getFile2();
        getFile1();
        getTest1();
        getHello();
      }
    }
  }

  public static void main(String[] args) throws Exception {
    HelloWorldClient client = new HelloWorldClient();
    client.run(args);
  }

}