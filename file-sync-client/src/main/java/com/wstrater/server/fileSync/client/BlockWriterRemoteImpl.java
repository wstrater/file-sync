package com.wstrater.server.fileSync.client;

import javax.ws.rs.core.MediaType;

import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.wstrater.server.fileSync.common.data.DeleteRequest;
import com.wstrater.server.fileSync.common.data.DeleteResponse;
import com.wstrater.server.fileSync.common.data.WriteRequest;
import com.wstrater.server.fileSync.common.data.WriteResponse;
import com.wstrater.server.fileSync.common.exceptions.ErrorDeflatingBlockException;
import com.wstrater.server.fileSync.common.exceptions.ErrorWritingBlockException;
import com.wstrater.server.fileSync.common.file.BlockReader;
import com.wstrater.server.fileSync.common.file.BlockWriter;
import com.wstrater.server.fileSync.common.utils.CompressionUtils;
import com.wstrater.server.fileSync.common.utils.Constants;
import com.wstrater.server.fileSync.common.utils.FileUtils;
import com.wstrater.server.fileSync.common.utils.TimeUtils;
import com.wstrater.server.fileSync.common.utils.CompressionUtils.Deflated;

/**
 * This a remote implementation of {@link BlockReader}. It connects to the {@link FileController}
 * controller running on the sever.
 * 
 * @author wstrater
 *
 */
public class BlockWriterRemoteImpl implements BlockWriter, RequiresRemoteClient {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private RemoteClient   remoteClient;

  public BlockWriterRemoteImpl(RemoteClient remoteClient) {
    super();
    this.remoteClient = remoteClient;
  }

  @Override
  public DeleteResponse deleteFile(DeleteRequest request) {
    DeleteResponse ret = new DeleteResponse();

    if (remoteClient == null) {
      throw new IllegalStateException(
          String.format("%s missing %s", getClass().getSimpleName(), RemoteClient.class.getSimpleName()));
    }

    ret.setRequest(request);

    String uri = remoteClient.getURI(String.format("%s/%s", Constants.FILE_PATH, request.getFileName()));

    WebResource webResource = remoteClient.getClient().resource(uri);
    logger.debug(webResource.toString());
    ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_OCTET_STREAM).delete(ClientResponse.class);
    try {
      remoteClient.checkForException(clientResponse);

      if (clientResponse.getStatus() != HttpStatus.OK_200 && clientResponse.getStatus() != HttpStatus.NO_CONTENT_204) {
        throw new ErrorWritingBlockException(String.format("Failed DELETE %s: %d/%s", uri, clientResponse.getStatus(),
            clientResponse.getStatusInfo()));
      }

      ret.setSuccess(Boolean.parseBoolean(clientResponse.getHeaders().getFirst(Constants.SUCCESS_HEADER)));
    } finally {
      clientResponse.close();
    }

    return ret;
  }

  /**
   * The timeStamp is adjusted to UTC.
   * 
   * @param request
   * @return
   */
  @Override
  public WriteResponse writeBlock(WriteRequest request) {
    WriteResponse ret = new WriteResponse();

    if (remoteClient == null) {
      throw new IllegalStateException(
          String.format("%s missing %s", getClass().getSimpleName(), RemoteClient.class.getSimpleName()));
    }

    ret.setRequest(request);

    String uri = remoteClient.getURI(String.format("%s/%s", Constants.FILE_PATH, request.getFileName()));

    byte[] block = request.getData();

    Deflated deflated = null;
    if (FileUtils.isCompress() && request.getLength() >= Constants.MINIMUM_FOR_COMPRESSION) {
      deflated = CompressionUtils.deflate(block);
      if (deflated == null) {
        throw new ErrorDeflatingBlockException("Error deflating write request");
      }
      if (deflated.getLength() >= request.getLength()) {
        deflated = null;
      } else {
        block = deflated.getData();
      }
    }

    WebResource webResource = remoteClient.getClient().resource(uri)
        .queryParam(Constants.OFFSET_PARAM, String.valueOf(request.getOffset()))
        .queryParam(Constants.LENGTH_PARAM, String.valueOf(request.getLength()))
        .queryParam(Constants.TIME_STAMP_PARAM, String.valueOf(TimeUtils.toUTC(request.getTimeStamp())))
        .queryParam(Constants.EOF_PARAM, String.valueOf(request.isEof()));
    if (deflated != null) {
      webResource = webResource.queryParam(Constants.COMPRESSED_PARAM, String.valueOf(deflated.getLength()));
    }
    logger.debug(webResource.toString());
    ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_OCTET_STREAM).put(ClientResponse.class,
        block);
    try {
      remoteClient.checkForException(clientResponse);

      if (clientResponse.getStatus() != HttpStatus.OK_200 && clientResponse.getStatus() != HttpStatus.NO_CONTENT_204) {
        throw new ErrorWritingBlockException(String.format("Failed PUT %s: %d/%s", uri, clientResponse.getStatus(),
            clientResponse.getStatusInfo()));
      }

      ret.setLength(Integer.parseInt(clientResponse.getHeaders().getFirst(Constants.LENGTH_HEADER)));
      ret.setCrc32(Long.parseLong(clientResponse.getHeaders().getFirst(Constants.CRC_HEADER)));
      ret.setSuccess(Boolean.parseBoolean(clientResponse.getHeaders().getFirst(Constants.SUCCESS_HEADER)));
    } finally {
      clientResponse.close();
    }

    return ret;
  }

  @Override
  public void setRemoteClient(RemoteClient remoteClient) {
    this.remoteClient = remoteClient;
  }

}