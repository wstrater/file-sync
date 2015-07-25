package com.wstrater.server.fileSync.client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.floreysoft.jmte.Engine;
import com.floreysoft.jmte.Renderer;
import com.wstrater.server.fileSync.common.data.DirectoryInfo;
import com.wstrater.server.fileSync.common.data.DirectoryListRequest;
import com.wstrater.server.fileSync.common.data.DirectoryListResponse;
import com.wstrater.server.fileSync.common.data.IndexFile;
import com.wstrater.server.fileSync.common.exceptions.ErrorListingDirectoryException;
import com.wstrater.server.fileSync.common.exceptions.ErrorLoadingPlanTemplateException;
import com.wstrater.server.fileSync.common.exceptions.ErrorWritingReportException;
import com.wstrater.server.fileSync.common.exceptions.InvalidFileLocationException;
import com.wstrater.server.fileSync.common.exceptions.InvalidReportFile;
import com.wstrater.server.fileSync.common.exceptions.MissingPlanTemplateException;
import com.wstrater.server.fileSync.common.exceptions.NotValidDirectoryException;
import com.wstrater.server.fileSync.common.file.DirectoryLister;
import com.wstrater.server.fileSync.common.file.DirectoryListerLocalAsRemoteImpl;
import com.wstrater.server.fileSync.common.file.DirectoryListerLocalImpl;
import com.wstrater.server.fileSync.common.utils.Compare;
import com.wstrater.server.fileSync.common.utils.Constants.SyncEnum;
import com.wstrater.server.fileSync.common.utils.DirectoryUtils;
import com.wstrater.server.fileSync.common.utils.FilePermissions;
import com.wstrater.server.fileSync.common.utils.FileUtils;
import com.wstrater.server.fileSync.common.utils.IndexManager;

/**
 * This class is used to process the <code>plan</code> command line option. It uses a
 * {@link DirectoryLister} to list the directories and {@link PlanMapper} to produce a plan. Once
 * all the plans have been generated, it produces are port using {@link Engine} templating engine to
 * produce the report.
 * 
 * @author wstrater
 *
 */
public class Planner {

  protected final Logger  logger = LoggerFactory.getLogger(getClass());

  private String          eol;
  private boolean         hiddenDirectories;
  private boolean         hiddenFiles;
  private File            localBaseDir;
  private DirectoryLister localLister;
  private FilePermissions permissions;
  private boolean         recursive;
  private File            remoteBaseDir;
  private RemoteClient    remoteClient;
  private DirectoryLister remoteLister;
  private String          reportFile;
  private String          templateName;

  private Planner() {}

  public static Builder builder() {
    return new Builder();
  }

  private DirectoryInfo getDirectoryInfo(DirectoryLister lister, File baseDir, String path) {
    DirectoryInfo ret = null;

    DirectoryListRequest request = new DirectoryListRequest();
    request.setBaseDir(baseDir);
    request.setPath(path);
    request.setHiddenDirectories(hiddenDirectories);
    request.setHiddenFiles(hiddenFiles);
    request.setRecursive(false);

    try {
      DirectoryListResponse response = lister.listDirectory(request);
      if (response != null && response.isSuccess()) {
        ret = response.getDirectoryInfo();
      } else {
        throw new ErrorListingDirectoryException(String.format("Unable to list directory: %s", request));
      }
    } catch (NotValidDirectoryException ee) {
      // It does not exist so create an empty structure.
      ret = new DirectoryInfo();
    }

    return ret;
  }

  /**
   * Set up the local and remote workers.
   * 
   * @param remoteClient Missing for unit testing.
   */
  private void init(RemoteClient remoteClient) {
    localLister = new DirectoryListerLocalImpl();

    if (remoteBaseDir != null && remoteClient == null) {
      // This is a special implementation for swapping local and remote permissions while unit
      // tesing.
      remoteLister = new DirectoryListerLocalAsRemoteImpl();
    } else {
      remoteLister = new DirectoryListerRemoteImpl(remoteClient);
    }
  }

  private String loadTemplate(String templateName) {
    String ret = null;

    try {
      InputStream in = null;
      File file = FileUtils.canonicalFile(new File(templateName));
      if (file.canRead()) {
        in = new FileInputStream(file);
      }
      if (in == null) {
        in = getClass().getResourceAsStream(templateName);
      }

      if (in == null) {
        throw new IOException();
      } else {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
          byte[] data = new byte[8096];
          int len = 0;
          while ((len = in.read(data)) >= 0) {
            out.write(data, 0, len);
          }
        } finally {
          out.close();
          in.close();
        }
        ret = out.toString();
      }
    } catch (IOException ee) {
      throw new MissingPlanTemplateException(String.format("Missing plan template %s", templateName));
    }

    if (Compare.isBlank(ret)) {
      throw new ErrorLoadingPlanTemplateException(String.format("Error loading plan template %s", templateName));
    }

    return ret;
  }

  /**
   * Create a new path as the process recursively steps through the sub-directories.
   * 
   * @param path
   * @param name
   * @return
   */
  private String newPath(String path, String name) {
    return String.format("%s/%s", path, name);
  }

  /**
   * Generate the plans and produce the report.
   * 
   * @param plan
   * @param path
   */
  public void plan(SyncEnum plan, String path) {
    if (plan == null) {
      throw new IllegalStateException(String.format("%s missing %s", getClass().getSimpleName(), SyncEnum.class.getSimpleName()));
    } else if (Compare.isBlank(path)) {
      throw new IllegalStateException(String.format("%s missing path", getClass().getSimpleName()));
    }

    File dir = FileUtils.canonicalFile(new File(localBaseDir, path));
    if (!dir.isDirectory()) {
      throw new NotValidDirectoryException(String.format("Not a valid directory '%s'", dir.getAbsolutePath()));
    } else if (!DirectoryUtils.isChild(localBaseDir, dir)) {
      throw new InvalidFileLocationException(String.format("Not a valid directory '%s'", dir.getAbsolutePath()));
    }

    List<DirectoryPlan> plans = new ArrayList<>();
    planContents(plans, plan, path, recursive, hiddenDirectories, hiddenFiles);

    produceReport(plan, plans, templateName, eol, reportFile);
  }

  private void planContents(List<DirectoryPlan> plans, SyncEnum plan, String path, boolean recursive, boolean hiddenDirectories,
      boolean hiddenFiles) {
    File dir = FileUtils.canonicalFile(new File(localBaseDir, path));
    if (!DirectoryUtils.isChild(localBaseDir, dir)) {
      throw new InvalidFileLocationException(String.format("Invalid directory '%s'", dir.getAbsolutePath()));
    }

    DirectoryInfo localDirectory = getDirectoryInfo(localLister, localBaseDir, path);
    logger.debug(String.format("Local Directory: %s", localDirectory));
    IndexFile localIndex = IndexManager.loadIndex(dir);
    IndexManager.updateIndexInfo(localIndex, localDirectory);

    DirectoryInfo remoteDirectory = getDirectoryInfo(remoteLister, remoteBaseDir, path);
    logger.debug(String.format("Remote Directory: %s", remoteDirectory));
    IndexFile remoteIndex = new IndexFile();
    IndexManager.updateIndexInfo(remoteIndex, remoteDirectory);

    PlanMapper planMap = new PlanMapper(plan, localDirectory, localIndex, remoteDirectory, remoteIndex, recursive, permissions);
    Collections.sort(planMap.getPlanItems(), PlanItem.compareByType());
    DirectoryPlan directoryPlan = new DirectoryPlan(path, planMap.getPlanItems());
    plans.add(directoryPlan);

    logger.debug(directoryPlan.toString());

    for (PlanItem planItem : directoryPlan.getPlanItems()) {
      switch (planItem.getAction()) {
        case SyncLocalDirToRemote: {
          if (recursive) {
            planContents(plans, plan, newPath(path, planItem.getLocal().getName()), recursive, hiddenDirectories, hiddenFiles);
          }
          break;
        }
        case SyncRemoteDirToLocal: {
          if (recursive) {
            planContents(plans, plan, newPath(path, planItem.getRemote().getName()), recursive, hiddenDirectories, hiddenFiles);
          }
          break;
        }
        default: {
          break;
        }
      }
    }
  }

  private void produceReport(SyncEnum plan, List<DirectoryPlan> plans, String templateName, String eol, String reportFile) {
    String template = loadTemplate(templateName);

    Map<String, Object> model = new HashMap<>();
    model.put("plan", plan);
    model.put("permissions", permissions);
    model.put("now", new Date());
    model.put("plans", plans);

    Engine engine = new Engine();
    engine.registerRenderer(Date.class, new Renderer<Date>() {

      @Override
      public String render(Date date, Locale locale) {
        String ret = null;

        if (date != null) {
          ret = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSZ").format(date);
        }

        return ret;
      }
    });
    String report = engine.transform(template, model);

    if (report != null && Compare.isNotBlank(eol)) {
      String trimmed = report.replaceAll("\\s*\\n\\s*", "");
      report = trimmed.replace(eol, "\n");
    }

    saveReport(report, reportFile);
  }

  private void saveReport(String report, String reportFile) {
    PrintStream out = System.out;

    try {
      if (Compare.isNotBlank(reportFile)) {
        File file = FileUtils.canonicalFile(new File(reportFile));
        if (file.exists() && !file.canWrite()) {
          throw new InvalidReportFile(String.format("Invalid report file: %s", file.getAbsolutePath()));
        }

        logger.info(String.format("Saving Report: %s", file.getAbsolutePath()));

        file.getParentFile().mkdirs();
        out = new PrintStream(file);
      }

      out.print(report);
      out.close();
    } catch (IOException ee) {
      throw new ErrorWritingReportException(String.format("Error writing plan report: %s", reportFile == null ? "<StdOut>"
          : reportFile));
    }
  }

  /**
   * Allow building a {@link Planner} with a flexible list of arguments that are easily readable.
   * 
   * @author wstrater
   *
   */
  public static class Builder {

    private Planner built = new Planner();

    public Planner build() {
      if (built.remoteClient == null && built.remoteBaseDir == null) {
        throw new IllegalStateException(String.format("% is missing a %s", getClass().getSimpleName(),
            RemoteClient.class.getSimpleName()));
      } else if (built.localBaseDir == null) {
        throw new IllegalStateException(String.format("%s missing localBaseDir", getClass().getSimpleName()));
      } else if (built.permissions == null) {
        throw new IllegalStateException(String.format("%s missing a %s", getClass().getSimpleName(),
            FilePermissions.class.getSimpleName()));
      }

      built.init(built.remoteClient);

      return built;
    }

    public Builder eol(String eol) {
      built.eol = eol;
      return this;
    }

    public Builder hiddenDirectories(boolean hiddenDirectories) {
      built.hiddenDirectories = hiddenDirectories;
      return this;
    }

    public Builder hiddenFiles(boolean hiddenFiles) {
      built.hiddenFiles = hiddenFiles;
      return this;
    }

    public Builder localBaseDir(File localBaseDir) {
      built.localBaseDir = localBaseDir;
      return this;
    }

    public Builder permissions(FilePermissions permissions) {
      built.permissions = permissions;
      return this;
    }

    public Builder recursive(boolean recursive) {
      built.recursive = recursive;
      return this;
    }

    /**
     * Allow for a local "remote" for testing or a real remote. This test relies on the remote using
     * correct base directory. Reset the remoteClient.
     * 
     * @param remoteBaseDir
     * @return
     */
    Builder remoteBaseDir(File remoteBaseDir) {
      built.remoteBaseDir = remoteBaseDir;
      built.remoteClient = null;
      return this;
    }

    /**
     * This is required for accessing a remote file system. Resets the remoteBaseDir used for
     * testing.
     * 
     * @param remoteClient
     * @return
     */
    public Builder remoteClient(RemoteClient remoteClient) {
      built.remoteClient = remoteClient;
      built.remoteBaseDir = null;
      return this;
    }

    public Builder reportFile(String reportFile) {
      built.reportFile = reportFile;
      return this;
    }

    public Builder templateName(String templateName) {
      built.templateName = templateName;
      return this;
    }

  }

}