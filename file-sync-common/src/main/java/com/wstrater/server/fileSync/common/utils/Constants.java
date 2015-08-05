package com.wstrater.server.fileSync.common.utils;

public abstract class Constants {

  public final static String ADMIN_ROLE              = "admin";
  public final static String BLOCK_SIZE_PARAM        = "blockSize";
  public final static String COMPRESSED_HEADER       = "fileSync-compressed";
  public final static String COMPRESSED_PARAM        = "compressed";
  public final static String CONTENT_ENCODED_HEADER  = "Accept-Encoding";
  public final static String CRC_HEADER              = "fileSync-crc";
  public final static String DEFLATE                 = "deflate";
  public final static String DIRECTORY_PATH          = "/dir";
  public final static String EOF_HEADER              = "fileSync-eof";
  public final static String EOF_PARAM               = "eof";
  public final static String EXCEPT_CLASS_HEADER     = "fileSync-exceptionClass";
  public final static String EXCEPT_MSG_HEADER       = "fileSync-exceptionMessage";
  public final static String FILE_PATH               = "/file";
  public final static String FILE_NAME_PARAM         = "fileName";
  public final static String FILES_PARAM             = "files";
  public final static String HASH_PATH               = "/hash";
  public final static String HASH_TYPE_PARAM         = "hashType";
  public final static String HIDDEN_DIRS_PARAM       = "hiddenDirs";
  public final static String HIDDEN_FILES_PARAM      = "hiddenFiles";
  public final static String ID_PARAM                = "id";
  public final static String LENGTH_HEADER           = "fileSync-length";
  public final static String LENGTH_PARAM            = "length";
  public final static int    MINIMUM_FOR_COMPRESSION = 3072;
  public final static String OFFSET_PARAM            = "offset";
  public final static String PATH_PARAM              = "path";
  public final static String REALM                   = "file-sync-server";
  public final static String RECURSIVE_PARAM         = "recursive";
  public final static String REHASH_PARAM            = "rehash";
  public final static String SUCCESS_HEADER          = "fileSync-success";
  public final static String TIME_STAMP_PARAM        = "timeStamp";
  public final static String USER_ROLE               = "user";

  public final static String ID_REST                 = "{" + ID_PARAM + " : .*}";
  // public final static String ID_REST = "{" + ID_PARAM + " : [\\w\\-]*}";
  public final static String PATH_REST               = "{" + PATH_PARAM + " : .*}";
  // public final static String PATH_REST = "{" + PATH_PARAM +
  // " : ([\\w\\.][\\w\\. \\-]*[/\\\\])*[\\w\\.][\\w\\. \\-]*}";
  public final static String FILE_NAME_REST          = "{" + FILE_NAME_PARAM + " : .*}";

  // public final static String FILE_NAME_REST = "{" + FILE_NAME_PARAM +
  // " : ([\\w\\.][\\w\\. \\-]*[/\\\\])*[\\w\\.][\\w\\. \\-]*}";

  public static String tOrF(boolean value) {
    return value ? "T" : "F";
  }

  public enum ActionEnum {
    DeleteFileFromRemote(true, 1), CopyFileToRemote(true, 2), DeleteFileFromLocal(true, 3), CopyFileToLocal(true, 4), DeleteDirFromRemote(
        true, 11), SyncLocalDirToRemote(true, 12), DeleteDirFromLocal(true, 13), SyncRemoteDirToLocal(true, 14), Skip(false, 101), Done(
        false, 102);

    private boolean inProcess;
    private int     order;

    private ActionEnum(boolean inProcess, int order) {
      this.inProcess = inProcess;
      this.order = order;
    }

    public String getName() {
      return name();
    }

    public boolean inProcess() {
      return inProcess;
    }

    public int order() {
      return order;
    }

    @Override
    public String toString() {
      return String.format("%s (%s)", name(), tOrF(inProcess));
    }

  }

  @Deprecated
  public enum FileActionEnum {
    CopyToLocal(true), DeleteFromLocal(true), CopyToRemote(true), DeleteFromRemote(true), Skip(false), Done(false);

    private boolean inProcess;

    private FileActionEnum(boolean inProcess) {
      this.inProcess = inProcess;
    }

    public boolean inProcess() {
      return inProcess;
    }

    @Override
    public String toString() {
      return String.format("%s (%s)", name(), tOrF(inProcess));
    }

  }

  public enum ExistsEnum {
    Local(true, false), Remote(false, true), Both(true, true), Neither(false, false);

    private boolean onLocal;
    private boolean onRemote;

    private ExistsEnum(boolean onLocal, boolean onRemote) {
      this.onLocal = onLocal;
      this.onRemote = onRemote;
    }

    public String getName() {
      return name();
    }

    public boolean onLocal() {
      return onLocal;
    }

    public boolean onRemote() {
      return onRemote;
    }

    @Override
    public String toString() {
      return String.format("%s (OL=%s,OR=%s)", name(), tOrF(onLocal), tOrF(onRemote));
    }

  };

  public enum InfoTypeEnum {
    Directory, File
  }

  public enum NewerEnum {
    Local(true, false), Remote(false, true), Same(true, true), Different(false, false);

    private boolean localNewer;
    private boolean remoteNewer;

    private NewerEnum(boolean localLatest, boolean remoteLatest) {
      this.localNewer = localLatest;
      this.remoteNewer = remoteLatest;
    }

    public static NewerEnum choose(Long local, Long remote) {
      return choose(local == null ? 0L : local.longValue(), remote == null ? 0L : remote.longValue());
    }

    public String getName() {
      return name();
    }

    public boolean localNewer() {
      return localNewer;
    }

    public boolean remoteNewer() {
      return remoteNewer;
    }

    @Override
    public String toString() {
      return String.format("%s (LN=%s,RN=%s)", name(), tOrF(localNewer), tOrF(remoteNewer));
    }

  };

  public enum PlanTypeEnum {
    Directory(1), File(2);

    private int order;

    private PlanTypeEnum(int order) {
      this.order = order;
    }

    public String getName() {
      return name();
    }

    public int order() {
      return order;
    }

  }

  public enum SslEnum {
    OneWay(false), TwoWay(true);

    private boolean clientAuth = false;

    private SslEnum(boolean clientAuth) {
      this.clientAuth = clientAuth;
    }

    public boolean clientAuth() {
      return clientAuth;
    }

    public String getName() {
      return name();
    }

    public static SslEnum parseSsl(String value) {
      return parseSsl(value, null);
    }

    public static SslEnum parseSsl(String value, SslEnum def) {
      SslEnum ret = def;

      if (value != null) {
        for (SslEnum sync : SslEnum.values()) {
          if (value.equalsIgnoreCase(sync.name())) {
            ret = sync;
            break;
          }
        }
      }

      return ret;
    }

    @Override
    public String toString() {
      return String.format("%s (CA=%s)", name(), tOrF(clientAuth));
    }

  }

  public enum SyncEnum {
    Local(true, false), Remote(false, true), Both(true, true);

    private boolean syncRemoteToLocal;
    private boolean syncLocalToRemote;

    private SyncEnum(boolean syncRemoteToLocal, boolean syncLocalToRemote) {
      this.syncRemoteToLocal = syncRemoteToLocal;
      this.syncLocalToRemote = syncLocalToRemote;
    }

    public String getName() {
      return name();
    }

    public static SyncEnum parseSync(String value) {
      return parseSync(value, null);
    }

    public static SyncEnum parseSync(String value, SyncEnum def) {
      SyncEnum ret = def;

      if (value != null) {
        for (SyncEnum sync : SyncEnum.values()) {
          if (value.equalsIgnoreCase(sync.name())) {
            ret = sync;
            break;
          }
        }
      }

      return ret;
    }

    /**
     * Sync from remote to local
     * 
     * @return
     */
    public boolean syncRemoteToLocal() {
      return syncRemoteToLocal;
    }

    /**
     * Sync from local to remote
     * 
     * @return
     */
    public boolean syncLocalToRemote() {
      return syncLocalToRemote;
    }

    @Override
    public String toString() {
      return String.format("%s (L2R=%s,R2L=%s)", name(), tOrF(syncLocalToRemote), tOrF(syncRemoteToLocal));
    }

  }

}