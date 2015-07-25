package com.wstrater.server.fileSync.client;

import java.security.Permissions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wstrater.server.fileSync.common.data.IndexInfo;
import com.wstrater.server.fileSync.common.utils.Compare;
import com.wstrater.server.fileSync.common.utils.Constants.ActionEnum;
import com.wstrater.server.fileSync.common.utils.Constants.ExistsEnum;
import com.wstrater.server.fileSync.common.utils.Constants.NewerEnum;
import com.wstrater.server.fileSync.common.utils.Constants.SyncEnum;
import com.wstrater.server.fileSync.common.utils.FilePermissions;

/**
 * Generates a {@link ActionEnum} by comparing the state of two files using {@link ExistsEnum},
 * {@link NewerEnum} and {@link FilePermissions}. If the current {@link ActionEnum} is returned if
 * it is in process.
 * 
 * @author wstrater
 *
 */
public abstract class PlanDecider {

  protected final static Logger logger = LoggerFactory.getLogger(PlanDecider.class);

  /**
   * This is a simple chooser. If you have an object that represents an object, then it exists.
   * 
   * @param local
   * @param remote
   * @return
   */
  public static ExistsEnum chooseExists(Object local, Object remote) {
    ExistsEnum ret = null;

    if (local == null && remote != null) {
      ret = ExistsEnum.Remote;
    } else if (local != null && remote == null) {
      ret = ExistsEnum.Local;
    } else if (local != null && remote != null) {
      ret = ExistsEnum.Both;
    } else {
      ret = ExistsEnum.Neither;
    }

    return ret;
  }

  /**
   * This chooser uses the last modified time to determine which is newer. If they are the
   * {@link NewerEnum#Same}, it compares the length and hashes to see if they are
   * {@link NewerEnum#Different}.
   * 
   * @param local
   * @param remote
   * @return
   */
  public static NewerEnum chooseNewer(IndexInfo local, IndexInfo remote) {
    NewerEnum ret = null;

    if (local == null) {
      if (remote == null) {
        ret = NewerEnum.Same;
      } else {
        ret = NewerEnum.Remote;
      }
    } else {
      if (remote == null) {
        ret = NewerEnum.Local;
      } else {
        if (local.getLastModified() < remote.getLastModified()) {
          ret = NewerEnum.Remote;
        } else if (local.getLastModified() > remote.getLastModified()) {
          ret = NewerEnum.Local;
        } else {
          if (!Compare.equals(local.getLength(), remote.getLength())) {
            ret = NewerEnum.Different;
          } else if (Compare.isNotBlank(local.getHash()) && Compare.isNotBlank(remote.getHash())
              && !Compare.equals(local.getHash(), remote.getHash())) {
            ret = NewerEnum.Different;
          } else {
            ret = NewerEnum.Same;
          }
        }
      }
    }

    return ret;
  }

  /**
   * Decide the {@link ActionEnum} based on the {@link Sync}, {@link ExistsEnum}, {@link NewerEnum}
   * and {@link Permissions}. Will return the current {@link ActionEnum} if it is in process.
   * 
   * @param currentAction
   * @param sync
   * @param exists
   * @param newer
   * @param permissions
   * @return
   */
  public static ActionEnum decide(ActionEnum currentAction, SyncEnum sync, ExistsEnum exists, NewerEnum newer,
      FilePermissions permissions) {
    ActionEnum ret = currentAction;

    // Actual value does not matter as long as it is set to some unique value whenever ret is set.
    int decision = 0;

    if (currentAction != null && currentAction.inProcess()) {
      decision = 0;
      ret = currentAction;
    } else {
      if (newer == NewerEnum.Same) {
        // They both exists and have the same time or neither exists.
        decision = 1;
        ret = ActionEnum.Skip;
      } else if (newer == NewerEnum.Local) {
        // Local is newer or remote does not exist.
        if (!exists.onRemote() && !sync.syncLocalToRemote()) {
          // It is not on remote and making local like remote.
          if (permissions.isLocalDelete()) {
            decision = 2;
            ret = ActionEnum.DeleteFileFromLocal;
          } else {
            decision = 3;
            ret = ActionEnum.Skip;
          }
        } else if (sync.syncLocalToRemote()) {
          // Syncing to remote so copy it.
          if (permissions.isRemoteWrite()) {
            decision = 4;
            ret = ActionEnum.CopyFileToRemote;
          } else {
            decision = 5;
            ret = ActionEnum.Skip;
          }
        } else {
          // Don't know so skip. Local could be newer.
          decision = 6;
          ret = ActionEnum.Skip;
        }
      } else if (newer == NewerEnum.Remote) {
        // Remote is newer or local does not exist.
        if (!exists.onLocal() && !sync.syncRemoteToLocal()) {
          // It is not on local and making remote like local.
          if (permissions.isRemoteDelete()) {
            decision = 7;
            ret = ActionEnum.DeleteFileFromRemote;
          } else {
            decision = 8;
            ret = ActionEnum.Skip;
          }
        } else if (sync.syncRemoteToLocal()) {
          // It is on remote do we copy it local.
          if (permissions.isLocalWrite()) {
            // Syncing to local so copy it.
            decision = 9;
            ret = ActionEnum.CopyFileToLocal;
          } else {
            // Not allowed to writes local
            decision = 10;
            ret = ActionEnum.Skip;
          }
        } else {
          // Don't know so skip.
          decision = 11;
          ret = ActionEnum.Skip;
        }
      } else if (newer == NewerEnum.Different) {
        // Both exist but not the same
        if (sync == SyncEnum.Remote) {
          // Local is mater
          if (permissions.isRemoteWrite()) {
            decision = 12;
            ret = ActionEnum.CopyFileToRemote;
          } else {
            decision = 13;
            ret = ActionEnum.Skip;
          }
        } else if (sync == SyncEnum.Local) {
          // Remote is master
          if (permissions.isLocalWrite()) {
            decision = 14;
            ret = ActionEnum.CopyFileToLocal;
          } else {
            decision = 15;
            ret = ActionEnum.Skip;
          }
        }
      } else {
        // Should never get here.
        decision = 16;
        ret = ActionEnum.Skip;
      }
    }

    logger.debug(String.format("currentAction: %s, sync: %s, exists: %s, newer: %s, permissions: %s -> %s (%d)", currentAction,
        sync, exists, newer, permissions, ret, decision));

    return ret;
  }

}