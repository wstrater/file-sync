package com.wstrater.server.fileSync.client;

import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.util.security.Password;

import com.wstrater.server.fileSync.common.exceptions.FileSyncException;
import com.wstrater.server.fileSync.common.hash.HashProcessor;
import com.wstrater.server.fileSync.common.utils.ChunkUtils;
import com.wstrater.server.fileSync.common.utils.CommandLineUtils;
import com.wstrater.server.fileSync.common.utils.DirectoryUtils;
import com.wstrater.server.fileSync.common.utils.FileUtils;
import com.wstrater.server.fileSync.common.utils.TimeUtils;

/**
 * This is the main client program. It communicates with {@link FileSyncServer}
 * 
 * @author wstrater
 *
 */
public class FileSyncClient {

  public void run(String[] args) {
    CommandLineUtils cli = new CommandLineUtils(String.format("%s --%s|--%s|--%s|--%s", getClass(), CommandLineUtils.PLAN_ARG,
        CommandLineUtils.SYNC_ARG, CommandLineUtils.HASH_ARG, CommandLineUtils.ENC_PASS_ARG));
    cli.useClient().useAllow().useBaseDir().useBlockSize().useEncPass().useHash().useHashType().useKeyStore().useMax().usePlan()
        .useSync().useTimeZone().useTrustStore().useUserAuth();

    if (cli.parseArgs(args)) {
      if (cli.isHelp() || cli.hasHelp()) {
        cli.displayHelp();
      } else if (cli.hasEncPass()) {
        System.out.println(Password.obfuscate(cli.getEncPass()));
        System.out.println(Credential.MD5.digest(cli.getEncPass()));
        if (cli.hasEncUser()) {
          System.out.println(Credential.Crypt.crypt(cli.getEncUser(), cli.getEncPass()));
        }
      } else {
        ChunkUtils.setBlockSize(cli.getBlockSize());
        DirectoryUtils.setBaseDir(cli.getBaseDir());
        FileUtils.setCompress(cli.isCompress());
        FileUtils.getPermissions().setLocalDelete(cli.isAllowDelete());
        FileUtils.getPermissions().setLocalWrite(cli.isAllowWrite());
        FileUtils.getPermissions().setRemoteDelete(cli.isRemoteDelete());
        FileUtils.getPermissions().setRemoteWrite(cli.isRemoteWrite());
        FileUtils.setMaxBlockSize(cli.getMaxBlock());
        FileUtils.setMaxOffset(cli.getMaxOffset());
        HashProcessor.setHashType(cli.getHashType());
        TimeUtils.setTimeZone(cli.getTimeZone());

        RemoteClient.Builder remoteBuilder = RemoteClient.builder().ssl(cli.hasSsl()).host(cli.getHost()).port(cli.getPort())
            .trustStoreFile(cli.getTrustFile()).trustStorePassword(cli.getTrustPass()).keyStoreFile(cli.getStoreFile())
            .keyStorePassword(cli.getStorePass()).userName(cli.getUserName()).userPassword(cli.getUserPass());
        remoteBuilder.basicAuth(cli.hasUserName() && cli.hasUserPass());
        RemoteClient remoteClient = remoteBuilder.build();

        if (cli.hasHash()) {
          Hasher hasher = Hasher.builder().localBaseDir(DirectoryUtils.getBaseDir()).remoteClient(remoteClient)
              .hashType(cli.getHashType()).recursive(cli.isRecursive()).hiddenDirectories(cli.isHiddenDirectories())
              .hiddenFiles(cli.isHiddenFiles()).rehash(cli.isReHash()).build();

          hasher.hash(cli.getHash(), cli.getPath());
        } else if (cli.hasPlan()) {
          Planner planner = Planner.builder().localBaseDir(DirectoryUtils.getBaseDir()).remoteClient(remoteClient)
              .permissions(FileUtils.getPermissions()).recursive(cli.isRecursive()).hiddenDirectories(cli.isHiddenDirectories())
              .hiddenFiles(cli.isHiddenFiles()).eol(cli.getPlanEol()).reportFile(cli.getPlanFile())
              .templateName(cli.getPlanTemplate()).build();

          planner.plan(cli.getPlan(), cli.getPath());
        } else if (cli.hasSync()) {
          Syncer syncer = Syncer.builder().localBaseDir(DirectoryUtils.getBaseDir()).remoteClient(remoteClient)
              .permissions(FileUtils.getPermissions()).recursive(cli.isRecursive()).hiddenDirectories(cli.isHiddenDirectories())
              .hiddenFiles(cli.isHiddenFiles()).build();

          syncer.sync(cli.getSync(), cli.getPath());
        } else {
          cli.displayHelp();
        }
      }
    }
  }

  public static void main(String[] args) {
    try {
      FileSyncClient client = new FileSyncClient();
      client.run(args);
    } catch (FileSyncException ee) {
      System.err.println(ee.getMessage());
    }
  }

}