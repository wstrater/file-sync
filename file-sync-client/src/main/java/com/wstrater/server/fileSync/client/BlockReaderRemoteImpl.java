package com.wstrater.server.fileSync.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import javax.ws.rs.core.MediaType;

import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.wstrater.server.fileSync.common.data.ReadRequest;
import com.wstrater.server.fileSync.common.data.ReadResponse;
import com.wstrater.server.fileSync.common.exceptions.ErrorInflatingBlockException;
import com.wstrater.server.fileSync.common.exceptions.ErrorReadingBlockException;
import com.wstrater.server.fileSync.common.exceptions.ErrorReadingResponse;
import com.wstrater.server.fileSync.common.file.BlockReader;
import com.wstrater.server.fileSync.common.utils.CompressionUtils;
import com.wstrater.server.fileSync.common.utils.CompressionUtils.Inflated;
import com.wstrater.server.fileSync.common.utils.Constants;
import com.wstrater.server.fileSync.common.utils.FileUtils;

/**
 * This a remote implementation of {@link BlockReader}. It connects to the {@link FileController}
 * controller running on the sever.
 * 
 * @author wstrater
 *
 */
public class BlockReaderRemoteImpl implements BlockReader, RequiresRemoteClient {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private RemoteClient   remoteClient;

  public BlockReaderRemoteImpl(RemoteClient remoteClient) {
    super();
    this.remoteClient = remoteClient;
  }

  private byte[] getDataByEntity(ClientResponse clientResponse, int length) {
    return clientResponse.getEntity(byte[].class);
  }

  private byte[] getDataByChannel(ClientResponse clientResponse, int length) {
    byte[] ret = null;

    try {
      ByteBuffer buffer = ByteBuffer.allocate(length);
      ReadableByteChannel channel = Channels.newChannel(clientResponse.getEntityInputStream());
      int count = 0;
      do {
        count = channel.read(buffer);
      } while (count >= 0 && buffer.hasRemaining());
      ret = buffer.array();
    } catch (IOException ee) {
      throw new ErrorReadingResponse(String.format("Unable to read %d bytes from response", length));
    }

    return ret;
  }

  @Override
  public ReadResponse readBlock(ReadRequest request) {
    ReadResponse ret = new ReadResponse();

    if (remoteClient == null) {
      throw new IllegalStateException(
          String.format("%s missing %s", getClass().getSimpleName(), RemoteClient.class.getSimpleName()));
    }

    ret.setRequest(request);

    String uri = remoteClient.getURI(String.format("%s/%s", Constants.FILE_PATH, request.getFileName()));

    WebResource webResource = remoteClient.getClient().resource(uri)
        .queryParam(Constants.OFFSET_PARAM, String.valueOf(request.getOffset()))
        .queryParam(Constants.BLOCK_SIZE_PARAM, String.valueOf(request.getBlockSize()));
    logger.debug(webResource.toString());
    Builder builder = webResource.accept(MediaType.APPLICATION_OCTET_STREAM);
    if (FileUtils.isCompress()) {
      builder = builder.header(Constants.CONTENT_ENCODED_HEADER, Constants.DEFLATE);
    }

    ClientResponse clientResponse = builder.get(ClientResponse.class);
    try {
      remoteClient.checkForException(clientResponse);

      if (clientResponse.getStatus() != HttpStatus.OK_200) {
        throw new ErrorReadingBlockException(String.format("Failed GET %s: HttpStatus: %d/%s", uri, clientResponse.getStatus(),
            clientResponse.getStatusInfo()));
      }

      logger.debug(clientResponse.getHeaders().toString());

      ret.setLength(Integer.parseInt(clientResponse.getHeaders().getFirst(Constants.LENGTH_HEADER)));
      int contentLength = Integer.parseInt(clientResponse.getHeaders().getFirst("Content-Length"));

      byte[] block = null;
      int compressed = -1;
      if (clientResponse.getHeaders().containsKey(Constants.COMPRESSED_HEADER)) {
        compressed = Integer.parseInt(clientResponse.getHeaders().getFirst(Constants.COMPRESSED_HEADER));
        if (contentLength != compressed) {
          throw new ErrorReadingBlockException(String.format("Failed GET %s: Compressed Content-Length: %d/%s", uri, contentLength,
              compressed));
        }

        Inflated inflated = CompressionUtils.inflate(getDataByEntity(clientResponse, compressed), ret.getLength());
        if (inflated == null || inflated.getLength() != ret.getLength()) {
          throw new ErrorInflatingBlockException("Error inflating compressed read response");
        }

        block = inflated.getData();
      } else {
        if (contentLength != ret.getLength()) {
          throw new ErrorReadingBlockException(String.format("Failed GET %s: Content-Length: %d/%s", uri, contentLength,
              ret.getLength()));
        }

        block = getDataByEntity(clientResponse, ret.getLength());
      }

      ret.setCrc32(Long.parseLong(clientResponse.getHeaders().getFirst(Constants.CRC_HEADER)));
      ret.setEof(Boolean.parseBoolean(clientResponse.getHeaders().getFirst(Constants.EOF_HEADER)));
      ret.setSuccess(Boolean.parseBoolean(clientResponse.getHeaders().getFirst(Constants.SUCCESS_HEADER)));
      ret.setData(block);
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