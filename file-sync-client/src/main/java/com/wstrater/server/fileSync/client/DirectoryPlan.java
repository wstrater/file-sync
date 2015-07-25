package com.wstrater.server.fileSync.client;

import java.util.List;

public class DirectoryPlan {

  private final static String TO_STRING_SEPARATOR = ",\n";

  private String              path;
  private List<PlanItem>      planItems;

  public DirectoryPlan(String path, List<PlanItem> planItems) {
    super();
    this.path = path;
    this.planItems = planItems;
  }

  public String getPath() {
    return path;
  }

  public List<PlanItem> getPlanItems() {
    return planItems;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append("DirectoryPlan [");
    if (path != null)
      builder.append("path=").append(path).append(", ");
    if (planItems != null) {
      builder.append("planItems=[\n");
      for (PlanItem planItem : planItems) {
        builder.append("  ").append(planItem).append(TO_STRING_SEPARATOR);
      }
      builder.append("]");
    }
    builder.append("]");

    return builder.toString();
  }

}