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

import com.wstrater.server.fileSync.common.data.DeleteRequest;
import com.wstrater.server.fileSync.common.data.DeleteResponse;
import com.wstrater.server.fileSync.common.data.ReadRequest;
import com.wstrater.server.fileSync.common.data.ReadResponse;
import com.wstrater.server.fileSync.common.data.WriteRequest;
import com.wstrater.server.fileSync.common.data.WriteResponse;
import com.wstrater.server.fileSync.common.exceptions.FileNotFoundException;
import com.wstrater.server.fileSync.common.exceptions.FileSyncException;
import com.wstrater.server.fileSync.common.exceptions.InvalidFileLocationException;
import com.wstrater.server.fileSync.common.file.BlockReader;
import com.wstrater.server.fileSync.common.file.BlockReaderLocalImpl;
import com.wstrater.server.fileSync.common.file.BlockWriter;
import com.wstrater.server.fileSync.common.file.BlockWriterLocalImpl;
import com.wstrater.server.fileSync.common.utils.Constants;
import com.wstrater.server.fileSync.common.utils.DirectoryUtils;
import com.wstrater.server.fileSync.common.utils.TimeUtils;

@Path(Constants.FILE_PATH)
public class FileController {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private BlockReader    reader = new BlockReaderLocalImpl();
  private BlockWriter    writer = new BlockWriterLocalImpl();

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
  // @Path("{fileName : ([\\w\\.][\\w\\. \\-]*[/\\\\])*[\\w\\.][\\w\\. \\-]*}")
  @Path("{fileName : .* }")
  public Response delete(@PathParam("fileName") String fileName) {
    Response ret;

    try {
      DeleteRequest request = new DeleteRequest();
      request.setBaseDir(DirectoryUtils.getBaseDir());
      request.setFileName(fileName);

      DeleteResponse response = writer.deleteFile(request);
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

  @GET
  @Path(Constants.FILE_NAME_REST)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response read(@PathParam(Constants.FILE_NAME_PARAM) String fileName, @QueryParam(Constants.OFFSET_PARAM) @DefaultValue("-1") long offset,
      @QueryParam(Constants.BLOCK_SIZE_PARAM) @DefaultValue("-1") int blockSize) {
    Response ret;

    try {
      ReadRequest request = new ReadRequest();
      request.setBaseDir(DirectoryUtils.getBaseDir());
      request.setFileName(fileName);
      request.setBlockSize(blockSize);
      request.setOffset(offset);

      ReadResponse response = reader.readBlock(request);
      ret = Response.ok(response.getData()).header(Constants.LENGTH_HEADER, String.valueOf(response.getLength()))
          .header(Constants.CRC_HEADER, String.valueOf(response.getCrc32()))
          .header(Constants.EOF_HEADER, String.valueOf(response.isEof()))
          .header(Constants.SUCCESS_HEADER, String.valueOf(response.isSuccess())).build();
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
   * @param fileName
   * @param offset
   * @param length
   * @param eof
   * @param timeStamp Time is expected to be UTC.
   * @param data
   * @return
   */
  @PUT
  // @Path("{fileName : ([\\w\\.][\\w\\. \\-]*[/\\\\])*[\\w\\.][\\w\\. \\-]*}")
  @Path("{fileName : .*}")
  public Response write(@PathParam(Constants.FILE_NAME_PARAM) String fileName, @QueryParam(Constants.OFFSET_PARAM) @DefaultValue("-1") long offset,
      @QueryParam(Constants.LENGTH_PARAM) @DefaultValue("-1") int length, @QueryParam(Constants.EOF_PARAM) boolean eof,
      @QueryParam(Constants.TIME_STAMP_PARAM) @DefaultValue("0") long timeStamp, byte[] data) {
    Response ret;

    try {
      WriteRequest request = new WriteRequest();
      request.setBaseDir(DirectoryUtils.getBaseDir());
      request.setFileName(fileName);
      request.setData(data);
      request.setEof(eof);
      request.setLength(length);
      request.setOffset(offset);
      request.setTimeStamp(TimeUtils.fromUTC(timeStamp));

      WriteResponse response = writer.writeBlock(request);
      ret = Response.noContent().header(Constants.LENGTH_HEADER, String.valueOf(response.getLength()))
          .header(Constants.CRC_HEADER, String.valueOf(response.getCrc32()))
          .header(Constants.SUCCESS_HEADER, String.valueOf(response.isSuccess())).build();
    } catch (InvalidFileLocationException ee) {
      logger.error(ee.getMessage());
      ret = Response.status(Status.FORBIDDEN).build();
    } catch (FileNotFoundException ee) {
      logger.error(ee.getMessage());
      ret = Response.status(Status.NOT_FOUND).build();
    } catch (FileSyncException ee) {
      logger.error(ee.getMessage());
      ret = Response.status(Status.BAD_REQUEST).build();
    }

    return ret;
  }

}