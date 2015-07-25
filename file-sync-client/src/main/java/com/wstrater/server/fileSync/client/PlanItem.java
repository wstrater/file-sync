package com.wstrater.server.fileSync.client;

import java.util.Comparator;

import com.wstrater.server.fileSync.common.data.InfoItem;
import com.wstrater.server.fileSync.common.utils.Compare;
import com.wstrater.server.fileSync.common.utils.Constants.ActionEnum;
import com.wstrater.server.fileSync.common.utils.Constants.ExistsEnum;
import com.wstrater.server.fileSync.common.utils.Constants.NewerEnum;
import com.wstrater.server.fileSync.common.utils.Constants.PlanTypeEnum;

/**
 * This class is used to describe the plan for synchronizing a local directory with a remote
 * directory. It is used for syncing and for producing a plan report.
 * 
 * @author wstrater
 *
 */
public class PlanItem implements Comparable<PlanItem> {

  private ActionEnum action;
  private ExistsEnum exists;
  private InfoItem   local;
  private NewerEnum  newer;
  private InfoItem   remote;
  private PlanTypeEnum type;

  public PlanItem(PlanTypeEnum type, InfoItem local, InfoItem remote, ExistsEnum exists, NewerEnum newer, ActionEnum action) {
    this.type = type;
    this.local = local;
    this.remote = remote;
    this.exists = exists;
    this.newer = newer;
    this.action = action;
  }

  /**
   * Order the items based on {@link ActionEnum} and local or remote {@link InfoItem#getName()}.
   */
  public static Comparator<PlanItem> compareByAction() {
    return new Comparator<PlanItem>() {

      @Override
      public int compare(PlanItem plan1, PlanItem plan2) {
        int ret = 0;

        if (plan1 == null) {
          if (plan2 != null) {
            ret = -1;
          }
        } else if (plan2 != null) {
          ret = Compare.compare(plan1.action == null ? -1 : plan1.action.order(), plan2.action == null ? -1 : plan2.action.order());
          if (ret == 0) {
            ret = plan1.compareTo(plan2);
          }
        }

        return ret;
      }
    };
  }

  /**
   * Order the items based on local or remote {@link InfoItem#getName()}.
   */
  public static Comparator<PlanItem> compareByName() {
    return new Comparator<PlanItem>() {

      @Override
      public int compare(PlanItem plan1, PlanItem plan2) {
        int ret = 0;

        if (plan1 == null) {
          if (plan2 != null) {
            ret = -1;
          }
        } else if (plan2 != null) {
          ret = plan1.compareTo(plan2);
        }

        return ret;
      }
    };
  }

  public static Comparator<PlanItem> compareByType() {
    return new Comparator<PlanItem>() {

      @Override
      public int compare(PlanItem plan1, PlanItem plan2) {
        int ret = 0;

        if (plan1 == null) {
          if (plan2 != null) {
            ret = -1;
          }
        } else if (plan2 != null) {
          ret = Compare.compare(plan1.type == null ? -1 : plan1.type.order(), plan2.type == null ? -1 : plan2.type.order());
          if (ret == 0) {
            ret = plan1.compareTo(plan2);
          }
        }

        return ret;
      }
    };
  }

  /**
   * Order the items based on local or remote {@link InfoItem#getName()}.
   */
  @Override
  public int compareTo(PlanItem that) {
    int ret = 0;

    if (that == null) {
      ret = 1;
    } else {
      ret = Compare.compare(this.local == null ? this.remote == null ? null : this.remote.getName() : this.local.getName(),
          that.local == null ? that.remote == null ? null : that.remote.getName() : that.local.getName());
    }

    return ret;
  }

  public ActionEnum getAction() {
    return action;
  }

  public ExistsEnum getExists() {
    return exists;
  }

  public InfoItem getLocal() {
    return local;
  }

  public NewerEnum getNewer() {
    return newer;
  }

  public InfoItem getRemote() {
    return remote;
  }

  public PlanTypeEnum getType() {
    return type;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("PlanItem [").append(type).append(", ");
    if (local != null)
      builder.append("local=").append(local.getName()).append(", ");
    if (remote != null)
      builder.append("remote=").append(remote.getName()).append(", ");
    if (exists != null)
      builder.append("exists=").append(exists).append(", ");
    if (newer != null)
      builder.append("newer=").append(newer).append(", ");
    if (action != null)
      builder.append("action=").append(action);
    builder.append("]");

    return builder.toString();
  }

}