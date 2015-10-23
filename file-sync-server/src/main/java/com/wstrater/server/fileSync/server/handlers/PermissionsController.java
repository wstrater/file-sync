package com.wstrater.server.fileSync.server.handlers;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wstrater.server.fileSync.common.data.DirectoryPermissionsRequest;
import com.wstrater.server.fileSync.common.data.DirectoryPermissionsResponse;
import com.wstrater.server.fileSync.common.exceptions.FileSyncException;
import com.wstrater.server.fileSync.common.file.DirectoryLister;
import com.wstrater.server.fileSync.common.file.DirectoryListerLocalImpl;
import com.wstrater.server.fileSync.common.utils.Constants;

@Path(Constants.PERMISSIONS_PATH)
public class PermissionsController {

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

  @GET
  public Response get() {
    Response ret = null;

    try {
      DirectoryPermissionsRequest request = new DirectoryPermissionsRequest();

      DirectoryPermissionsResponse response = lister.getPermissions(request);
      ret = Response.noContent().header(Constants.ALLOW_DELETE_HEADER, String.valueOf(response.isAllowDelete()))
          .header(Constants.ALLOW_WRITE_HEADER, String.valueOf(response.isAllowWrite()))
          .header(Constants.SUCCESS_HEADER, String.valueOf(response.isSuccess())).build();
    } catch (FileSyncException ee) {
      ret = addException(Response.status(Status.BAD_REQUEST), ee).build();
    }

    return ret;
  }

}