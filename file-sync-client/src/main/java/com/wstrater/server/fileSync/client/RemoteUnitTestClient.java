package com.wstrater.server.fileSync.client;

/**
 * This extension of {@link RemoteClient} is needed for integration testing with a remote server.
 * The remote server is started with set permissions but the test needs to modify them for the test.
 * This class is used by remote implementation to signal that the permissions need to be overridden
 * and the overridden values.
 * 
 * @author wstrater
 *
 */
class RemoteUnitTestClient extends RemoteClient {

  private boolean allowDelete;
  private boolean allowWrite;

  private RemoteUnitTestClient() {}

  public static Builder builder() {
    return new Builder(new RemoteUnitTestClient());
  }

  public boolean isAllowDelete() {
    return allowDelete;
  }

  public boolean isAllowWrite() {
    return allowWrite;
  }

  static class Builder extends RemoteClient.Builder {

    public Builder(RemoteClient built) {
      super(built);
    }

    @Override
    protected RemoteUnitTestClient built() {
      return (RemoteUnitTestClient) super.built();
    }

    public Builder allowDelete(boolean allowDelete) {
      built().allowDelete = allowDelete;
      return this;
    }

    public Builder allowWrite(boolean allowWrite) {
      built().allowWrite = allowWrite;
      return this;
    }

  }

}