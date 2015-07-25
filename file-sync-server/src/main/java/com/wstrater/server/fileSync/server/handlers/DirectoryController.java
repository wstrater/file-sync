package com.wstrater.server.fileSync.server.handlers;

import javax.ws.rs.DELETE;
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

import com.wstrater.server.fileSync.common.data.DirectoryDeleteRequest;
import com.wstrater.server.fileSync.common.data.DirectoryDeleteResponse;
import com.wstrater.server.fileSync.common.data.DirectoryListRequest;
import com.wstrater.server.fileSync.common.data.DirectoryListResponse;
import com.wstrater.server.fileSync.common.data.DirectoryMakeRequest;
import com.wstrater.server.fileSync.common.data.DirectoryMakeResponse;
import com.wstrater.server.fileSync.common.exceptions.FileNotFoundException;
import com.wstrater.server.fileSync.common.exceptions.FileSyncException;
import com.wstrater.server.fileSync.common.exceptions.InvalidFileLocationException;
import com.wstrater.server.fileSync.common.file.DirectoryLister;
import com.wstrater.server.fileSync.common.file.DirectoryListerLocalImpl;
import com.wstrater.server.fileSync.common.utils.Constants;
import com.wstrater.server.fileSync.common.utils.DirectoryUtils;
import com.wstrater.server.fileSync.common.utils.TimeUtils;

@Path(Constants.DIRECTORY_PATH)
public class DirectoryController {

  protected final Logger  logger = LoggerFactory.getLogger(getClass());

  private DirectoryLister lister = new DirectoryListerLocalImpl();

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

  @DELETE
  @Path(Constants.PATH_REST)
  public Response delete(@PathParam(Constants.PATH_PARAM) String path,
      @QueryParam(Constants.RECURSIVE_PARAM) @DefaultValue("false") boolean recursive,
      @QueryParam(Constants.FILES_PARAM) @DefaultValue("false") boolean files) {
    Response ret = null;

    try {
      DirectoryDeleteRequest request = new DirectoryDeleteRequest();
      request.setBaseDir(DirectoryUtils.getBaseDir());
      request.setPath(path);
      request.setFiles(files);
      request.setRecursive(recursive);

      DirectoryDeleteResponse response = lister.deleteDirectory(request);
      ret = Response.noContent().header(Constants.SUCCESS_HEADER, String.valueOf(response.isSuccess())).build();
    } catch (InvalidFileLocationException ee) {
      ret = addException(Response.status(Status.FORBIDDEN), ee).build();
    } catch (FileNotFoundException ee) {
      ret = addException(Response.status(Status.NOT_FOUND), ee).build();
    } catch (FileSyncException ee) {
      ret = addException(Response.status(Status.BAD_REQUEST), ee).build();
    }

    return ret;
  }

  /**
   * @param path
   * @param recursive
   * @param hiddenDirectories
   * @param hiddenFiles
   * @return The lastModified is adjusted to UTC
   */
  @GET
  @Path(Constants.PATH_REST)
  @Produces(MediaType.APPLICATION_JSON)
  public Response list(@PathParam(Constants.PATH_PARAM) String path,
      @QueryParam(Constants.RECURSIVE_PARAM) @DefaultValue("false") boolean recursive,
      @QueryParam(Constants.HIDDEN_DIRS_PARAM) @DefaultValue("false") boolean hiddenDirectories,
      @QueryParam(Constants.HIDDEN_FILES_PARAM) @DefaultValue("false") boolean hiddenFiles) {
    Response ret = null;

    try {
      DirectoryListRequest request = new DirectoryListRequest();
      request.setBaseDir(DirectoryUtils.getBaseDir());
      request.setPath(path);
      request.setRecursive(recursive);
      request.setHiddenDirectories(hiddenDirectories);
      request.setHiddenFiles(hiddenFiles);

      DirectoryListResponse response = lister.listDirectory(request);
      if (response.getDirectoryInfo() != null) {
        // Adjust from current time zone to UTC.
        response.getDirectoryInfo().adjustLastModified(TimeUtils.fromUTC(0L));
      }
      ret = Response.ok(response.getDirectoryInfo()).header(Constants.SUCCESS_HEADER, String.valueOf(response.isSuccess())).build();
    } catch (InvalidFileLocationException ee) {
      ret = addException(Response.status(Status.FORBIDDEN), ee).build();
    } catch (FileNotFoundException ee) {
      ret = addException(Response.status(Status.NOT_FOUND), ee).build();
    } catch (FileSyncException ee) {
      ret = addException(Response.status(Status.BAD_REQUEST), ee).build();
    }

    return ret;
  }

  @PUT
  @Path(Constants.PATH_REST)
  public Response make(@PathParam(Constants.PATH_PARAM) String path) {
    Response ret = null;

    try {
      DirectoryMakeRequest request = new DirectoryMakeRequest();
      request.setBaseDir(DirectoryUtils.getBaseDir());
      request.setPath(path);

      DirectoryMakeResponse response = lister.makeDirectory(request);
      ret = Response.noContent().header(Constants.SUCCESS_HEADER, String.valueOf(response.isSuccess())).build();
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