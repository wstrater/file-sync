package com.wstrater.server.fileSync.server.handlers;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wstrater.server.fileSync.common.data.HashRequest;
import com.wstrater.server.fileSync.common.data.HashResponse;
import com.wstrater.server.fileSync.common.data.HashStatus;
import com.wstrater.server.fileSync.common.exceptions.FileNotFoundException;
import com.wstrater.server.fileSync.common.exceptions.FileSyncException;
import com.wstrater.server.fileSync.common.exceptions.InvalidFileLocationException;
import com.wstrater.server.fileSync.common.file.HashRequester;
import com.wstrater.server.fileSync.common.file.HashRequesterLocalImpl;
import com.wstrater.server.fileSync.common.hash.HashProcessor;
import com.wstrater.server.fileSync.common.utils.Constants;
import com.wstrater.server.fileSync.common.utils.DirectoryUtils;

@Path(Constants.HASH_PATH)
public class HashController {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private HashRequester         hasher = new HashRequesterLocalImpl();

  private ResponseBuilder addException(ResponseBuilder builder, Throwable ee) {
    logger.error(ee.getMessage());

    if (ee instanceof FileSyncException) {
      builder.header(Constants.EXCEPT_CLASS_HEADER, ee.getClass().getName());
    } else {
      builder.header(Constants.EXCEPT_CLASS_HEADER, FileSyncException.class.getName());
    }
    builder.header(Constants.EXCEPT_MSG_HEADER, ee.getMessage());

    return builder;
  }

  @PUT
  @Path(Constants.PATH_REST)
  @Produces(MediaType.APPLICATION_JSON)
  public Response hash(@PathParam(Constants.PATH_PARAM) String path, @QueryParam(Constants.ID_PARAM) String id,
      @QueryParam(Constants.HASH_TYPE_PARAM) @DefaultValue(HashProcessor.DEFAULT_HASH_TYPE) String hashType,
      @QueryParam(Constants.HIDDEN_DIRS_PARAM) @DefaultValue("false") boolean hiddenDirectories,
      @QueryParam(Constants.HIDDEN_FILES_PARAM) @DefaultValue("false") boolean hiddenFiles,
      @QueryParam(Constants.RECURSIVE_PARAM) @DefaultValue("false") boolean recursive,
      @QueryParam(Constants.REHASH_PARAM) @DefaultValue("false") boolean reHashExisting) {
    Response ret = null;

    try {
      HashRequest request = new HashRequest();
      request.setBaseDir(DirectoryUtils.getBaseDir());
      request.setPath(path);
      request.setHashType(hashType);
      request.setHiddenDirectories(hiddenDirectories);
      request.setHiddenFiles(hiddenFiles);
      request.setId(id);
      request.setRecursive(recursive);
      request.setReHashExisting(reHashExisting);

      HashResponse response = hasher.hashDirectory(request);
      ret = Response.noContent().header(Constants.SUCCESS_HEADER, String.valueOf(response.isQueued())).build();
    } catch (InvalidFileLocationException ee) {
      ret = addException(Response.status(Status.FORBIDDEN), ee).build();
    } catch (FileNotFoundException ee) {
      ret = addException(Response.status(Status.NOT_FOUND), ee).build();
    } catch (FileSyncException ee) {
      ret = addException(Response.status(Status.BAD_REQUEST), ee).build();
    }

    return ret;
  }

  @GET
  @Path(Constants.ID_REST)
  @Produces(MediaType.APPLICATION_JSON)
  public Response status(@PathParam(Constants.ID_PARAM) String id) {
    Response ret = null;

    try {
      HashStatus status = hasher.getHashStatus(id);
      if (status == null) {
        throw new FileNotFoundException(String.format("HashRequest %s not found", id));
      }
      ret = Response.ok(status).header(Constants.SUCCESS_HEADER, status != null).build();
    } catch (InvalidFileLocationException ee) {
      ret = addException(Response.status(Status.FORBIDDEN), ee).build();
    } catch (FileNotFoundException ee) {
      ret = addException(Response.status(Status.NOT_FOUND), ee).build();
    } catch (FileSyncException ee) {
      ret = addException(Response.status(Status.BAD_REQUEST), ee).build();
    }

    return ret;
  }

}