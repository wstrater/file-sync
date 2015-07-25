package com.wstrater.server.fileSync.client;

import javax.ws.rs.core.MediaType;

import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.wstrater.server.fileSync.common.data.DirectoryDeleteRequest;
import com.wstrater.server.fileSync.common.data.DirectoryDeleteResponse;
import com.wstrater.server.fileSync.common.data.DirectoryInfo;
import com.wstrater.server.fileSync.common.data.DirectoryListRequest;
import com.wstrater.server.fileSync.common.data.DirectoryListResponse;
import com.wstrater.server.fileSync.common.data.DirectoryMakeRequest;
import com.wstrater.server.fileSync.common.data.DirectoryMakeResponse;
import com.wstrater.server.fileSync.common.exceptions.ErrorDeletingDirectoryException;
import com.wstrater.server.fileSync.common.exceptions.ErrorListingDirectoryException;
import com.wstrater.server.fileSync.common.exceptions.ErrorMakingDirectoryException;
import com.wstrater.server.fileSync.common.file.DirectoryLister;
import com.wstrater.server.fileSync.common.utils.Constants;
import com.wstrater.server.fileSync.common.utils.TimeUtils;

/**
 * This a remote implementation of {@link DirectoryLister}. It connects to the
 * {@link DirectoryController} controller running on the sever.
 * 
 * @author wstrater
 *
 */
public class DirectoryListerRemoteImpl implements DirectoryLister, RequiresRemoteClient {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private RemoteClient   remoteClient;

  public DirectoryListerRemoteImpl(RemoteClient remoteClient) {
    super();
    this.remoteClient = remoteClient;
  }

  @Override
  public DirectoryDeleteResponse deleteDirectory(DirectoryDeleteRequest request) {
    DirectoryDeleteResponse ret = new DirectoryDeleteResponse();

    if (remoteClient == null) {
      throw new IllegalStateException(
          String.format("%s missing %s", getClass().getSimpleName(), RemoteClient.class.getSimpleName()));
    }

    ret.setRequest(request);

    String uri = remoteClient.getURI(String.format("%s/%s", Constants.DIRECTORY_PATH, request.getPath()));

    WebResource webResource = remoteClient.getClient().resource(uri)
        .queryParam(Constants.FILES_PARAM, String.valueOf(request.isFiles()))
        .queryParam(Constants.RECURSIVE_PARAM, String.valueOf(request.isRecursive()));
    logger.debug(webResource.toString());
    ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).delete(ClientResponse.class);
    try {
      remoteClient.checkForException(clientResponse);

      if (clientResponse.getStatus() != HttpStatus.OK_200 && clientResponse.getStatus() != HttpStatus.NO_CONTENT_204) {
        throw new ErrorDeletingDirectoryException(String.format("Failed DELETE %s: %d/%s", uri, clientResponse.getStatus(),
            clientResponse.getStatusInfo()));
      }

      ret.setSuccess(Boolean.parseBoolean(clientResponse.getHeaders().getFirst(Constants.SUCCESS_HEADER)));
    } finally {
      clientResponse.close();
    }

    return ret;
  }

  /**
   * The lastModified is ajdusted from UTC
   * 
   * @see com.wstrater.server.fileSync.common.file.DirectoryLister#listDirectory(com.wstrater.server.fileSync.common.data.DirectoryListRequest)
   */
  @Override
  public DirectoryListResponse listDirectory(DirectoryListRequest request) {
    DirectoryListResponse ret = new DirectoryListResponse();

    if (remoteClient == null) {
      throw new IllegalStateException(
          String.format("%s missing %s", getClass().getSimpleName(), RemoteClient.class.getSimpleName()));
    }

    ret.setRequest(request);

    String uri = remoteClient.getURI(String.format("%s/%s", Constants.DIRECTORY_PATH, request.getPath()));

    WebResource webResource = remoteClient.getClient().resource(uri)
        .queryParam(Constants.HIDDEN_DIRS_PARAM, String.valueOf(request.isHiddenDirectories()))
        .queryParam(Constants.HIDDEN_FILES_PARAM, String.valueOf(request.isHiddenFiles()))
        .queryParam(Constants.RECURSIVE_PARAM, String.valueOf(request.isRecursive()));
    logger.debug(webResource.toString());
    ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
    try {
      remoteClient.checkForException(clientResponse);

      if (clientResponse.getStatus() != HttpStatus.OK_200 && clientResponse.getStatus() != HttpStatus.NO_CONTENT_204) {
        throw new ErrorListingDirectoryException(String.format("Failed GET %s: %d/%s", uri, clientResponse.getStatus(),
            clientResponse.getStatusInfo()));
      }

      ret.setDirectoryInfo(clientResponse.getEntity(DirectoryInfo.class));
      if (ret.getDirectoryInfo() != null) {
        // Adjust from UTC to current time zone.
        ret.getDirectoryInfo().adjustLastModified(TimeUtils.toUTC(0L));
      }
      ret.setSuccess(Boolean.parseBoolean(clientResponse.getHeaders().getFirst(Constants.SUCCESS_HEADER)));
    } finally {
      clientResponse.close();
    }

    return ret;
  }

  @Override
  public DirectoryMakeResponse makeDirectory(DirectoryMakeRequest request) {
    DirectoryMakeResponse ret = new DirectoryMakeResponse();

    if (remoteClient == null) {
      throw new IllegalStateException(
          String.format("%s missing %s", getClass().getSimpleName(), RemoteClient.class.getSimpleName()));
    }

    ret.setRequest(request);

    String uri = remoteClient.getURI(String.format("%s/%s", Constants.DIRECTORY_PATH, request.getPath()));

    WebResource webResource = remoteClient.getClient().resource(uri);
    logger.debug(webResource.toString());
    ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).put(ClientResponse.class);
    try {
      remoteClient.checkForException(clientResponse);

      if (clientResponse.getStatus() != HttpStatus.OK_200 && clientResponse.getStatus() != HttpStatus.NO_CONTENT_204) {
        throw new ErrorMakingDirectoryException(String.format("Failed PUT %s: %d/%s", uri, clientResponse.getStatus(),
            clientResponse.getStatusInfo()));
      }

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