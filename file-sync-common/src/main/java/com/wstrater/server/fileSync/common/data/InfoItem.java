package com.wstrater.server.fileSync.common.data;

import java.util.Date;

/**
 * This interface is used for creating a {@link PlanItem}. The interface expose methods that are
 * needed producing a plan report but not for syncing.
 * 
 * @author wstrater
 *
 */
public interface InfoItem {

  public byte getAccess();

  public Long getLength();

  public Long getLastModified();

  public Date getLastModifiedDate();

  public String getName();

}