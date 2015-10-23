package com.wstrater.server.fileSync.common.utils;

import java.io.File;

import com.wstrater.server.fileSync.common.file.DirectoryLister;

/**
 * Set/check the allowed access to files and directories. Directories access is based on the allowed
 * permissions and the file system permissions. File access is based on directory access and the
 * file system permissions.
 * <p/>
 * This class is a combination of builder and static utility class. The builder is used by the
 * {@link DirectoryLister} to set the flags. The utility methods are used by the {@link PlanMapper}.
 * In the mean time, the flags could be transmitted over the network and creating an object with
 * lots of getter methods without a backing fields just seem "wrong".
 * 
 * @author wstrater
 *
 */
public abstract class AccessUtils {

  final static byte   DIR_EXISTS  = (byte) 0x80;
  final static byte   DIR_READ    = (byte) 0x40;
  final static byte   DIR_WRITE   = (byte) 0x20;
  final static byte   DIR_DELETE  = (byte) 0x10;
  final static byte   DIR_MASK    = (byte) (DIR_EXISTS | DIR_READ | DIR_WRITE | DIR_DELETE);

  final static byte   FILE_EXISTS = (byte) 0x08;
  final static byte   FILE_READ   = (byte) 0x04;
  final static byte   FILE_WRITE  = (byte) 0x02;
  final static byte   FILE_DELETE = (byte) 0x01;
  final static byte   FILE_MASK   = (byte) (FILE_EXISTS | FILE_READ | FILE_WRITE | FILE_DELETE);

  final static byte[] FLAGS       = new byte[] { DIR_EXISTS, DIR_READ, DIR_WRITE, DIR_DELETE, FILE_EXISTS, FILE_READ, FILE_WRITE,
      FILE_DELETE                };

  final static int    NUM_FLAGS   = Byte.SIZE;

  public static Access access() {
    return new Access();
  }

  public static Access access(byte access) {
    return new Access(access);
  }

  /**
   * Does the directory exists and allow delete.
   * 
   * @param access
   * @return
   */
  public static boolean canDeleteDir(byte access) {
    return isFlag(access, DIR_EXISTS) && isFlag(access, DIR_DELETE);
  }

  /**
   * Is {@link #canDeleteDir(byte)} and the file exists and allows deleting.
   * 
   * @param access
   * @return
   */
  public static boolean canDeleteFile(byte access) {
    return canDeleteDir(access) && isFlag(access, FILE_EXISTS) && isFlag(access, FILE_DELETE);
  }

  /**
   * Does the directory exists and allow read.
   * 
   * @param access
   * @return
   */
  public static boolean canReadDir(byte access) {
    return isFlag(access, DIR_EXISTS) && isFlag(access, DIR_READ);
  }

  /**
   * Is {@link #canReadDir(byte)} and the file exist and can be read.
   * 
   * @param access
   * @return
   */
  public static boolean canReadFile(byte access) {
    return canReadDir(access) && isFlag(access, FILE_EXISTS) && isFlag(access, FILE_READ);
  }

  /**
   * Does the directory exists and allow write.
   * 
   * @param access
   * @return
   */
  public static boolean canWriteDir(byte access) {
    return isFlag(access, DIR_EXISTS) && isFlag(access, DIR_WRITE);
  }

  /**
   * Is {@link #canWriteDir(byte)} and file does not exist or can be written.
   * 
   * @param access
   * @return
   */
  public static boolean canWriteFile(byte access) {
    return canWriteDir(access) && (!isFlag(access, FILE_EXISTS) || isFlag(access, FILE_WRITE));
  }

  public static boolean isExistsDir(byte access) {
    return isFlag(access, DIR_EXISTS);
  }

  public static boolean isExistsFile(byte access) {
    return isFlag(access, FILE_EXISTS);
  }

  private static boolean isFlag(byte access, byte mask) {
    return (access & mask) == mask;
  }

  public static Access NewDirectory() {
    return access((byte)(DIR_MASK));
  };

  public static class Access {

    private byte access;

    public Access() {
      this.access = 0;
    }

    public Access(byte access) {
      this.access = access;
    }

    void clearFlag(byte mask) {
      access &= ~mask;
    }

    Access deleteDir() {
      setFlag(DIR_DELETE);
      return this;
    }

    Access deleteFile() {
      setFlag(FILE_DELETE);
      return this;
    }

    /**
     * Set the dir flags based on a {@link File}.
     * 
     * @param dir
     * @return
     */
    public Access dir(File dir) {
      if (dir != null && dir.isFile()) {
        file(dir);
      } else {
        if (dir == null || !dir.exists()) {
          clearFlag(DIR_MASK);
        } else {
          setFlag(DIR_EXISTS);
          if (dir.canRead()) {
            setFlag(DIR_READ);
          } else {
            clearFlag(DIR_READ);
          }
          if (dir.canWrite()) {
            setFlag((byte) (DIR_WRITE | DIR_DELETE));
          } else {
            clearFlag((byte) (DIR_WRITE | DIR_DELETE));
          }
        }
      }

      return this;
    }

    Access existsDir() {
      setFlag(DIR_EXISTS);
      return this;
    }

    Access existsFile() {
      setFlag(FILE_EXISTS);
      return this;
    }

    /**
     * Set the file flags based on a {@link File}.
     * 
     * @param file
     * @return
     */
    public Access file(File file) {
      if (file != null && file.isDirectory()) {
        dir(file);
      } else {
        if (file == null || !file.exists()) {
          clearFlag(FILE_MASK);
        } else {
          setFlag(FILE_EXISTS);

          if (file.canRead()) {
            setFlag(FILE_READ);
          } else {
            clearFlag(FILE_READ);
          }

          if (file.canWrite()) {
            setFlag((byte) (FILE_WRITE | FILE_DELETE));
          } else {
            clearFlag((byte) (FILE_WRITE | FILE_DELETE));
          }
        }
      }

      return this;
    }

    public byte get() {
      return access;
    }

    boolean isFlag(byte mask) {
      return AccessUtils.isFlag(access, mask);
    }

    /**
     * This is used for testing with a {@link RemoteClient}. The local test will need to override
     * the permissions enforced by the remote server.
     * <p/>
     * <strong>Should only be used for unit/integration testing.</strong>
     * 
     * @param allowDelete
     * @param allowWrite
     * @return
     */
    public Access overrideForTesting(boolean allowDelete, boolean allowWrite) {
      if (allowDelete) {
        setFlag(DIR_DELETE);
      } else {
        clearFlag(DIR_DELETE);
      }

      if (allowWrite) {
        setFlag(DIR_WRITE);
      } else {
        clearFlag(DIR_WRITE);
      }

      return this;
    }

    /**
     * Permissions are set on the command line and used to override the file system flags. They can
     * remove local access the user does not want but not add access the file system does not allow.
     * <p/>
     * <strong>Should be called after {@link #dir(File)}.</strong>
     * 
     * @param permissions
     * @return
     */
    public Access permissions(FilePermissions permissions) {
      return permissions(permissions.isLocalDelete(), permissions.isLocalWrite());
    }

    public Access permissions(boolean allowDelete, boolean allowWrite) {
      if (!allowDelete) {
        clearFlag(DIR_DELETE);
      }

      if (!allowWrite) {
        clearFlag(DIR_WRITE);
      }

      return this;
    }

    Access readDir() {
      setFlag(DIR_READ);
      return this;
    }

    Access readFile() {
      setFlag(FILE_READ);
      return this;
    }

    void setFlag(byte mask) {
      access |= mask;
    }

    @Override
    public String toString() {
      return toString(access);
    }

    public static String toString(byte access) {
      String ret = String.format("%" + NUM_FLAGS + "s", Integer.toBinaryString(access)).replace(' ', '0');
      if (ret.length() > NUM_FLAGS) {
        ret = ret.substring(ret.length() - NUM_FLAGS);
      }

      return ret;
    }

    Access writeDir() {
      setFlag(DIR_WRITE);
      return this;
    }

    Access writeFile() {
      setFlag(FILE_WRITE);
      return this;
    }

  }

}