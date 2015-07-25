package com.wstrater.server.fileSync.client;

import org.junit.Before;

import com.wstrater.server.fileSync.common.utils.CommandLineUtils;
import com.wstrater.server.fileSync.common.utils.FilePermissions;
import com.wstrater.server.fileSync.common.utils.TimeUtils;

/**
 * This is a {@link SyncerTest} that relies on a running {@link FileSyncServer}. Verification will
 * only work if {@link FileSyncServer} is started with the correct base directory.
 * 
 * @author wstrater
 *
 */
public class SyncerRemoteClientTest extends SyncerTest {

  protected CommandLineUtils cli = new CommandLineUtils(getClass());

  /**
   * Command line parameters need to be defined has properties. <code>--port=8080</code> becomes
   * <code>-Dport=8080</code>.
   */
  @Before
  @Override
  public void before() {
    super.before();
    cli.useClient().useKeyStore().useTimeZone().useTrustStore();
    cli.parseArgs(new String[] {});

    TimeUtils.setTimeZone(cli.getTimeZone());
  }

  @Override
  protected Syncer newSyncer(FilePermissions permissions) {
    RemoteClient.Builder remoteBuilder = RemoteClient.builder().host(cli.getHost()).port(cli.getPort());
    if (cli.getSsl() != null) {
      remoteBuilder.ssl(true);
      remoteBuilder.keyStoreFile(cli.getStoreFile());
      remoteBuilder.keyStorePassword(cli.getStorePass());
      remoteBuilder.privateKeyPassword(cli.getKeyPass());
      remoteBuilder.trustStoreFile(cli.getTrustFile());
      remoteBuilder.trustStorePassword(cli.getTrustPass());
    }
    RemoteClient remoteClient = remoteBuilder.build();

    return Syncer.builder().localBaseDir(localBaseDir).remoteClient(remoteClient).permissions(permissions).recursive(true).build();
  }

}