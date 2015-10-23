package com.wstrater.server.fileSync.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wstrater.server.fileSync.common.data.DirectoryInfo;
import com.wstrater.server.fileSync.common.data.IndexFile;
import com.wstrater.server.fileSync.common.data.IndexInfo;
import com.wstrater.server.fileSync.common.data.InfoItem;
import com.wstrater.server.fileSync.common.utils.AccessPermissions;
import com.wstrater.server.fileSync.common.utils.AccessUtils;
import com.wstrater.server.fileSync.common.utils.Compare;
import com.wstrater.server.fileSync.common.utils.FileUtils;
import com.wstrater.server.fileSync.common.utils.Constants.ActionEnum;
import com.wstrater.server.fileSync.common.utils.Constants.ExistsEnum;
import com.wstrater.server.fileSync.common.utils.Constants.NewerEnum;
import com.wstrater.server.fileSync.common.utils.Constants.PlanTypeEnum;
import com.wstrater.server.fileSync.common.utils.Constants.SyncEnum;
import com.wstrater.server.fileSync.common.utils.FilePermissions;

/**
 * This class takes a local and remote directory listings and produces a plan for the {@link Syncer}
 * to act on or the {@link Planner} to report on. It relies on @{link PlanDecider} to generate the
 * {@link ActionEnum} for files. It generates the {@link ActionEnum} for directories itself.
 * 
 * @author wstrater
 *
 */
public class PlanMapper {

  private final static String TO_STRING_SEPARATOR = ",\n";

  protected final Logger      logger              = LoggerFactory.getLogger(getClass());
  private List<PlanItem>      planItems           = new ArrayList<>();

  public PlanMapper(SyncEnum sync, DirectoryInfo localDirectory, IndexFile localIndex, DirectoryInfo remoteDirectory,
      IndexFile remoteIndex, boolean recursive, FilePermissions permissions) {
    if (sync == null) {
      throw new IllegalStateException(String.format("%s missing %s", getClass().getSimpleName(), SyncEnum.class.getSimpleName()));
    } else if (localDirectory == null) {
      throw new IllegalStateException(String.format("%s missing local %s", getClass().getSimpleName(),
          DirectoryInfo.class.getSimpleName()));
    } else if (localIndex == null) {
      throw new IllegalStateException(String.format("%s missing local %s", getClass().getSimpleName(),
          IndexFile.class.getSimpleName()));
    } else if (remoteDirectory == null) {
      throw new IllegalStateException(String.format("%s missing remote %s", getClass().getSimpleName(),
          DirectoryInfo.class.getSimpleName()));
    } else if (remoteIndex == null) {
      throw new IllegalStateException(String.format("%s missing remote %s", getClass().getSimpleName(),
          IndexFile.class.getSimpleName()));
    }

    buildMap(sync, localDirectory, localIndex, remoteDirectory, remoteIndex, recursive);
  }

  @Deprecated
  private void buildLists(SyncEnum sync, DirectoryInfo localDirectory, IndexFile localIndex, DirectoryInfo remoteDirectory,
      IndexFile remoteIndex, FilePermissions permissions) {
    List<String> localDirectiesToDelete = new ArrayList<>();
    List<String> localDirectiesToSync = new ArrayList<>();
    List<IndexInfo> localFilesToDelete = new ArrayList<>();
    List<IndexInfo> localFilesToSync = new ArrayList<>();
    List<String> remoteDirectiesToDelete = new ArrayList<>();
    List<String> remoteDirectiesToSync = new ArrayList<>();
    List<IndexInfo> remoteFilesToDelete = new ArrayList<>();
    List<IndexInfo> remoteFilesToSync = new ArrayList<>();

    // Map files that are local.
    for (Entry<String, IndexInfo> entry : localIndex.getIndexInfos().entrySet()) {
      IndexInfo localInfo = entry.getValue();
      IndexInfo remoteInfo = remoteIndex.removeIndexInfo(localInfo.getName());

      ExistsEnum exists = PlanDecider.chooseExists(localInfo, remoteInfo);
      NewerEnum newer = PlanDecider.chooseNewer(localInfo, remoteInfo);

      logger.debug(String.format("Deciding local %s %b", localInfo.getName(), remoteInfo != null));
      ActionEnum action = PlanDecider.decide(localInfo.getChunkInfo().getAction(), sync, exists, newer,
          AccessPermissions.both(localInfo.getAccess(), remoteInfo.getAccess()));
      switch (action) {
        case CopyFileToLocal: {
          remoteFilesToSync.add(remoteInfo);
          break;
        }
        case CopyFileToRemote: {
          localFilesToSync.add(localInfo);
          break;
        }
        case DeleteFileFromLocal: {
          localFilesToDelete.add(localInfo);
          break;
        }
        case Skip: {
          break;
        }
        default: {
          throw new IllegalStateException(String.format(
              "Unexpected decission from local current: %s, sync: %s, exists: %s, newer: %s, permissions: %s: %s", localInfo
                  .getChunkInfo().getAction(), sync, exists, newer, permissions, action));
        }
      }
    }

    // Map files only remote.
    for (Entry<String, IndexInfo> entry : remoteIndex.getIndexInfos().entrySet()) {
      IndexInfo remoteInfo = entry.getValue();

      logger.debug(String.format("Deciding remote %s", remoteInfo.getName()));
      ActionEnum action = PlanDecider.decide(ActionEnum.Skip, sync, ExistsEnum.Remote, NewerEnum.Remote,
          AccessPermissions.remoteOnly(remoteInfo.getAccess()));
      switch (action) {
        case CopyFileToLocal: {
          remoteFilesToSync.add(remoteInfo);
          break;
        }
        case DeleteFileFromRemote: {
          remoteFilesToDelete.add(remoteInfo);
          break;
        }
        case Skip: {
          break;
        }
        default: {
          throw new IllegalStateException(String.format(
              "Unexpected decission from remote current: %s, sync: %s, exists: %s, newer: %s, permissions: %s: %s", remoteInfo
                  .getChunkInfo().getAction(), sync, ExistsEnum.Remote, NewerEnum.Remote, permissions, action));
        }
      }
    }

    // Map the directories that are local.
    List<DirectoryInfo> remoteDirectories = remoteDirectory.getDirectories();
    for (DirectoryInfo localInfo : localDirectory.getDirectories()) {
      boolean foundRemote = false;
      for (int xx = 0; xx < remoteDirectories.size(); xx++) {
        if (Compare.equals(localInfo.getName(), remoteDirectories.get(xx).getName())) {
          foundRemote = true;
          remoteDirectories.remove(xx);
          break;
        }
      }

      if (foundRemote) {
        localDirectiesToSync.add(localInfo.getName());
      } else if (sync.syncRemoteToLocal()) {
        // It is local but not remote and we are syncing from remote to local so delete local.
        localDirectiesToDelete.add(localInfo.getName());
      }
    }

    // Map the directories that are only remote.
    for (DirectoryInfo remoteInfo : remoteDirectories) {
      if (sync.syncRemoteToLocal()) {
        remoteDirectiesToSync.add(remoteInfo.getName());
      } else if (sync.syncLocalToRemote()) {
        // It is remote but not local and we are syncing from local to remote so delete remote.
        remoteDirectiesToDelete.add(remoteInfo.getName());
      }
    }
  }

  private void buildMap(SyncEnum sync, DirectoryInfo localDirectory, IndexFile localIndex, DirectoryInfo remoteDirectory,
      IndexFile remoteIndex, boolean recursive) {
    planItems.clear();

    // Map files that are local.
    for (Entry<String, IndexInfo> entry : localIndex.getIndexInfos().entrySet()) {
      IndexInfo localInfo = entry.getValue();
      IndexInfo remoteInfo = remoteIndex.removeIndexInfo(localInfo.getName());

      ExistsEnum exists = PlanDecider.chooseExists(localInfo, remoteInfo);
      NewerEnum newer = PlanDecider.chooseNewer(localInfo, remoteInfo);

      logger.debug(String.format("Deciding local file %s %b", localInfo.getName(), remoteInfo != null));
      AccessPermissions permissions = getPermissions(localDirectory, localInfo, remoteDirectory, remoteInfo);
      ActionEnum action = PlanDecider.decide(localInfo.getChunkInfo().getAction(), sync, exists, newer, permissions);
      switch (action) {
        case CopyFileToLocal: {
          // remoteFilesToSync.add(remoteInfo);
          planItems.add(new PlanItem(PlanTypeEnum.File, localInfo, remoteInfo, exists, newer, ActionEnum.CopyFileToLocal));
          break;
        }
        case CopyFileToRemote: {
          // localFilesToSync.add(localInfo);
          planItems.add(new PlanItem(PlanTypeEnum.File, localInfo, remoteInfo, exists, newer, ActionEnum.CopyFileToRemote));
          break;
        }
        case DeleteFileFromLocal: {
          // localFilesToDelete.add(localInfo);
          planItems.add(new PlanItem(PlanTypeEnum.File, localInfo, remoteInfo, exists, newer, ActionEnum.DeleteFileFromLocal));
          break;
        }
        case Skip: {
          planItems.add(new PlanItem(PlanTypeEnum.File, localInfo, remoteInfo, exists, newer, ActionEnum.Skip));
          break;
        }
        default: {
          throw new IllegalStateException(String.format(
              "Unexpected decission from local current: %s, sync: %s, exists: %s, newer: %s, permissions: %s: %s", localInfo
                  .getChunkInfo().getAction(), sync, exists, newer, permissions, action));
        }
      }
    }

    // Map files only remote.
    for (Entry<String, IndexInfo> entry : remoteIndex.getIndexInfos().entrySet()) {
      IndexInfo remoteInfo = entry.getValue();

      logger.debug(String.format("Deciding remote file %s", remoteInfo.getName()));
      AccessPermissions permissions = getPermissions(localDirectory, null, remoteDirectory, remoteInfo);
      ActionEnum action = PlanDecider.decide(ActionEnum.Skip, sync, ExistsEnum.Remote, NewerEnum.Remote, permissions);
      switch (action) {
        case CopyFileToLocal: {
          // remoteFilesToSync.add(remoteInfo);
          planItems.add(new PlanItem(PlanTypeEnum.File, null, remoteInfo, ExistsEnum.Remote, NewerEnum.Remote,
              ActionEnum.CopyFileToLocal));
          break;
        }
        case DeleteFileFromRemote: {
          // remoteFilesToDelete.add(remoteInfo);
          planItems.add(new PlanItem(PlanTypeEnum.File, null, remoteInfo, ExistsEnum.Remote, NewerEnum.Remote,
              ActionEnum.DeleteFileFromRemote));
          break;
        }
        case Skip: {
          planItems.add(new PlanItem(PlanTypeEnum.File, null, remoteInfo, ExistsEnum.Remote, NewerEnum.Remote, ActionEnum.Skip));
          break;
        }
        default: {
          throw new IllegalStateException(String.format(
              "Unexpected decission from remote current: %s, sync: %s, exists: %s, newer: %s, permissions: %s: %s", remoteInfo
                  .getChunkInfo().getAction(), sync, ExistsEnum.Remote, NewerEnum.Remote, permissions, action));
        }
      }
    }

    if (recursive) {
      // Map the directories that are local.
      List<DirectoryInfo> remoteDirectories = remoteDirectory.getDirectories();
      for (DirectoryInfo localInfo : localDirectory.getDirectories()) {
        DirectoryInfo remoteInfo = null;
        for (int xx = 0; xx < remoteDirectories.size(); xx++) {
          if (Compare.equals(localInfo.getName(), remoteDirectories.get(xx).getName())) {
            remoteInfo = remoteDirectories.remove(xx);
            break;
          }
        }

        logger.debug(String.format("Deciding local dir %s %b", localInfo.getName(), remoteInfo != null));

        if (remoteInfo == null) {
          if (sync.syncLocalToRemote()) {
            planItems.add(new PlanItem(PlanTypeEnum.Directory, localInfo, remoteInfo, ExistsEnum.Both, NewerEnum.Different,
                ActionEnum.SyncLocalDirToRemote));
          } else {
            // It is local but not remote and we are syncing from remote to local so delete local.
            // localDirectiesToDelete.add(localInfo.getName());
            AccessPermissions permissions = getPermissions(localDirectory, localInfo, remoteDirectory, remoteInfo);
            if (permissions.isLocalDeleteDirectory()) {
              planItems.add(new PlanItem(PlanTypeEnum.Directory, localInfo, null, ExistsEnum.Local, NewerEnum.Local,
                  ActionEnum.DeleteDirFromLocal));
            } else {
              planItems.add(new PlanItem(PlanTypeEnum.Directory, localInfo, null, ExistsEnum.Local, NewerEnum.Local,
                  ActionEnum.Skip));
            }
          }
        } else {
          // localDirectiesToSync.add(localInfo.getName());
          planItems.add(new PlanItem(PlanTypeEnum.Directory, localInfo, remoteInfo, ExistsEnum.Both, NewerEnum.Different,
              ActionEnum.SyncLocalDirToRemote));
        }
      }

      // Map the directories that are only remote.
      for (DirectoryInfo remoteInfo : remoteDirectories) {
        AccessPermissions permissions = getPermissions(localDirectory, null, remoteDirectory, remoteInfo);
        logger.debug(String.format("Deciding remote dir %s", remoteInfo.getName()));
        if (sync.syncRemoteToLocal()) {
          // remoteDirectiesToSync.add(remoteInfo.getName());
          if (permissions.isLocalWriteDirectory()) {
            planItems.add(new PlanItem(PlanTypeEnum.Directory, null, remoteInfo, ExistsEnum.Remote, NewerEnum.Remote,
                ActionEnum.SyncRemoteDirToLocal));
          } else {
            planItems.add(new PlanItem(PlanTypeEnum.Directory, null, remoteInfo, ExistsEnum.Remote, NewerEnum.Remote,
                ActionEnum.Skip));
          }
        } else {
          // It is remote but not local and we are syncing from local to remote so delete remote.
          // remoteDirectiesToDelete.add(remoteInfo.getName());
          if (permissions.isRemoteDeleteDirectory()) {
            planItems.add(new PlanItem(PlanTypeEnum.Directory, null, remoteInfo, ExistsEnum.Remote, NewerEnum.Remote,
                ActionEnum.DeleteDirFromRemote));
          } else {
            planItems.add(new PlanItem(PlanTypeEnum.Directory, null, remoteInfo, ExistsEnum.Remote, NewerEnum.Remote,
                ActionEnum.Skip));
          }
        }
      }
    }

    Collections.sort(planItems, PlanItem.compareByAction());
  }

  private AccessPermissions getPermissions(DirectoryInfo localDirectory, InfoItem localInfo, DirectoryInfo remoteDirectory,
      InfoItem remoteInfo) {
    byte localAccess = 0;
    byte remoteAccess = 0;

    if (localInfo != null) {
      localAccess = localInfo.getAccess();
    } else if (localDirectory != null) {
      localAccess = localDirectory.getAccess();
    } else {
      // Assume that we are syncing a new directory the parent directory permitted.
      localAccess = AccessUtils.NewDirectory().permissions(FileUtils.getPermissions()).get();
      throw new IllegalStateException("Local directory info should exist");
    }

    if (remoteInfo != null) {
      remoteAccess = remoteInfo.getAccess();
    } else if (remoteDirectory != null) {
      remoteAccess = remoteDirectory.getAccess();
    } else {
      // Assume that we are syncing a new directory the parent directory permitted.
      remoteAccess = AccessUtils.NewDirectory().get();
      throw new IllegalStateException("Remote directory info should exist");
    }

    return AccessPermissions.both(localAccess, remoteAccess);
  }

  public List<PlanItem> getPlanItems() {
    return planItems;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("PlanMapper [");
    if (planItems != null) {
      builder.append("planItems=[\n");
      for (PlanItem planItem : planItems) {
        builder.append(planItem).append(TO_STRING_SEPARATOR);
      }
      builder.append("]");
    }
    builder.append("]");

    return builder.toString();
  }

}