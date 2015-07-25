package com.wstrater.server.fileSync.client;

import javax.ws.rs.core.MediaType;

import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.wstrater.server.fileSync.common.data.HashRequest;
import com.wstrater.server.fileSync.common.data.HashResponse;
import com.wstrater.server.fileSync.common.data.HashStatus;
import com.wstrater.server.fileSync.common.exceptions.ErrorHashingDirectoryException;
import com.wstrater.server.fileSync.common.file.HashRequester;
import com.wstrater.server.fileSync.common.utils.Constants;

/**
 * This a remote implementation of {@link HashRequester}. It connects to the
 * {@link HashController} controller running on the sever.
 * 
 * @author wstrater
 *
 */
public class HashRequesterRemoteImpl implements HashRequester, RequiresRemoteClient {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private RemoteClient   remoteClient;

  public HashRequesterRemoteImpl(RemoteClient remoteClient) {
    super();
    this.remoteClient = remoteClient;
  }

  @Override
  public HashStatus getHashStatus(String id) {
    HashStatus ret = null;

    if (remoteClient == null) {
      throw new IllegalStateException(
          String.format("%s missing %s", getClass().getSimpleName(), RemoteClient.class.getSimpleName()));
    }

    String uri = remoteClient.getURI(String.format("%s/%s", Constants.HASH_PATH, id));

    WebResource webResource = remoteClient.getClient().resource(uri);
    logger.debug(webResource.toString());
    ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
    try {
      remoteClient.checkForException(clientResponse);

      if (clientResponse.getStatus() != HttpStatus.OK_200 && clientResponse.getStatus() != HttpStatus.NO_CONTENT_204) {
        throw new ErrorHashingDirectoryException(String.format("Failed GET %s: %d/%s", uri, clientResponse.getStatus(),
            clientResponse.getStatusInfo()));
      }

      ret = clientResponse.getEntity(HashStatus.class);
    } finally {
      clientResponse.close();
    }

    return ret;
  }

  @Override
  public HashResponse hashDirectory(HashRequest request) {
    HashResponse ret = new HashResponse();

    if (remoteClient == null) {
      throw new IllegalStateException(
          String.format("%s missing %s", getClass().getSimpleName(), RemoteClient.class.getSimpleName()));
    }

    ret.setRequest(request);

    String uri = remoteClient.getURI(String.format("%s/%s", Constants.HASH_PATH, request.getPath()));

    WebResource webResource = remoteClient.getClient().resource(uri)
        .queryParam(Constants.HIDDEN_DIRS_PARAM, String.valueOf(request.isHiddenDirectories()))
        .queryParam(Constants.HIDDEN_FILES_PARAM, String.valueOf(request.isHiddenFiles()))
        .queryParam(Constants.ID_PARAM, request.getId())
        .queryParam(Constants.RECURSIVE_PARAM, String.valueOf(request.isRecursive()))
        .queryParam(Constants.REHASH_PARAM, String.valueOf(request.isReHashExisting()));
    if (request.getHashType() != null) {
      webResource.queryParam(Constants.HASH_TYPE_PARAM, request.getHashType());
    }
    logger.debug(webResource.toString());
    ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).put(ClientResponse.class);
    try {
      remoteClient.checkForException(clientResponse);

      if (clientResponse.getStatus() != HttpStatus.OK_200 && clientResponse.getStatus() != HttpStatus.NO_CONTENT_204) {
        throw new ErrorHashingDirectoryException(String.format("Failed PUT %s: %d/%s", uri, clientResponse.getStatus(),
            clientResponse.getStatusInfo()));
      }

      ret.setQueued(Boolean.parseBoolean(clientResponse.getHeaders().getFirst(Constants.SUCCESS_HEADER)));
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