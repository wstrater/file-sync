package com.wstrater.server.fileSync.server.handlers;

import java.util.Arrays;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wstrater.server.fileSync.common.data.Hello;

@Path("/hello")
public class HelloWorldController {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @GET
  @Path("world/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  public Hello hello(@PathParam("name") String name) {
    Hello ret = new Hello();

    logger.info("Hello: {}", name);

    if (name == null) {
      ret.setName("World");
    } else {
      ret.setName(name);
    }

    logger.info("Hello: {}", ret);

    return ret;
  }

  @GET
  @Path("test1/{name : (\\w[\\w\\. \\-]*[/\\\\])*\\w[\\w\\. \\-]*}")
  // @Path("test1/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  public Hello test1(@PathParam("name") String name) {
    Hello ret = new Hello();

    logger.info("Test1: {}", name);

    if (name == null) {
      ret.setName("World");
    } else {
      ret.setName(name);
    }

    logger.info("Test1: {}", ret);

    return ret;
  }

  @GET
  @Path("file1/{name : (\\w[\\w\\. \\-]*[/\\\\])*\\w[\\w\\. \\-]*}")
  @Produces(MediaType.APPLICATION_JSON)
  public String file1(@PathParam("name") String name, @QueryParam("block") long block, @QueryParam("size") int size) {
    Hello ret = new Hello();

    logger.info("File: {}, Block: {}, Size: {}", name, block, size);

    if (name == null) {
      ret.setName("World");
    } else {
      ret.setName(String.format("%s,%d,%d", name, block, size));
    }

    logger.info("File: {}", ret);

    return ret.toString();
  }

  @PUT
  @Path("file2/{name : (\\w[\\w\\. \\-]*[/\\\\])*\\w[\\w\\. \\-]*}")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public byte[] file2(@PathParam("name") String name, @QueryParam("block") long block, @QueryParam("size") int size, byte[] data) {
    Hello ret = new Hello();

    logger.info("file: {}, Block: {}, Size: {}", name, block, size);
    if (data != null) {
      logger.info("file: {}, Data: {}: {}", name, data.length, Arrays.toString(data));
    }

    if (name == null) {
      ret.setName("World");
    } else {
      ret.setName(String.format("%s,%d,%d", name, block, size));
    }

    logger.info("file: {}", ret);

    return ret.toString().getBytes();
  }

}