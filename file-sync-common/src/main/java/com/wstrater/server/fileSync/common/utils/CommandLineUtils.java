package com.wstrater.server.fileSync.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wstrater.server.fileSync.common.hash.HashProcessor;
import com.wstrater.server.fileSync.common.utils.Constants.SslEnum;
import com.wstrater.server.fileSync.common.utils.Constants.SyncEnum;

/**
 * Common place to process command line arguments. You must "use" and option before it is available.
 * Help is automatically available.
 *
 * @author wstrater
 *
 */
public class CommandLineUtils {

  public final static String ALLOW_DELETE_ARG  = "allow-delete";
  public final static String ALLOW_WRITE_ARG   = "allow-write";
  public final static String BASE_DIR_ARG      = "base-dir";
  public final static String BLOCK_SIZE_ARG    = "block-size";
  public final static String COMPRESS_ARG      = "compress";
  public final static String ENC_PASS_ARG      = "enc-pass";
  public final static String ENC_USER_ARG      = "enc-user";
  public final static String HASH_ARG          = "hash";
  public final static String HASH_TYPE_ARG     = "hash-type";
  public final static String HELP_ARG          = "help";
  public final static String HELP_SHORT_ARG    = "h";
  public final static String HOST_ARG          = "host";
  public final static String HIDDEN_DIRS_ARG   = "hidden-dirs";
  public final static String HIDDEN_FILES_ARG  = "hidden-files";
  public final static String KEY_PASS_ARG      = "key-pass";
  public final static String MAX_BLOCK_ARG     = "max-block";
  public final static String MAX_OFFSET_ARG    = "max-offset";
  public final static String PATH_ARG          = "path";
  public final static String PLAN_ARG          = "plan";
  public final static String PLAN_EOL_ARG      = "plan-eol";
  public final static String PLAN_FILE_ARG     = "plan-file";
  public final static String PLAN_TEMPLATE_ARG = "plan-template";
  public final static String PORT_ARG          = "port";
  public final static String PROPS_ARG         = "props";
  public final static String PROP_PREFIX_ARG   = "prop-prefix";
  public final static String RECURSIVE_ARG     = "recursive";
  public final static String REHASH_ARG        = "rehash";
  public final static String REMOTE_DELETE_ARG = "remote-delete";
  public final static String REMOTE_WRITE_ARG  = "remote-write";
  public final static String SSL_ARG           = "ssl";
  public final static String STORE_FILE_ARG    = "store-file";
  public final static String STORE_PASS_ARG    = "store-pass";
  public final static String SYNC_ARG          = "sync";
  public final static String TIME_ZONE_ARG     = "time-zone";
  public final static String TRUST_FILE_ARG    = "trust-file";
  public final static String TRUST_PASS_ARG    = "trust-pass";
  public final static String USER_FILE_ARG     = "user-file";
  public final static String USER_NAME_ARG     = "user-name";
  public final static String USER_PASS_ARG     = "user-pass";

  protected final Logger     logger            = LoggerFactory.getLogger(getClass());

  private CommandLine        cli;
  private Options            options           = new Options();
  private String             propPrefix        = null;
  private List<Properties>   props             = new LinkedList<>();
  private String             usage             = "Usage";

  private boolean            allowDelete       = FilePermissions.DEFAULT_LOCAL_DELETE;
  private boolean            allowWrite        = FilePermissions.DEFAULT_LOCAL_WRITE;
  private File               baseDir;
  private int                blockSize         = ChunkUtils.DEFAULT_BLOCK_SIZE;
  private boolean            compress          = false;
  private String             encPass;
  private String             encUser;
  private SyncEnum           hash              = SyncEnum.Local;
  private String             hashType          = HashProcessor.DEFAULT_HASH_TYPE;
  private boolean            help              = false;
  private boolean            hiddenDirectories = false;
  private boolean            hiddenFiles       = false;
  private String             host              = "localhost";
  private String             keyPass;
  private int                maxBlock          = FileUtils.MAX_BLOCK_SIZE;
  private long               maxOffset         = FileUtils.MAX_OFFSET;
  private CommandLineParser  parser;
  private String             path              = ".";
  private SyncEnum           plan              = SyncEnum.Local;
  private String             planEol           = null;
  private String             planFile          = null;
  private String             planTemplate      = "planTemplateCSV.jmte";
  private int                port              = 8080;
  private boolean            remoteDelete      = FilePermissions.DEFAULT_REMOTE_DELETE;
  private boolean            remoteWrite       = FilePermissions.DEFAULT_REMOTE_WRITE;
  private boolean            recursive         = true;
  private boolean            reHash            = false;
  private SslEnum            ssl               = null;
  private File               storeFile;
  private String             storePass;
  private SyncEnum           sync              = SyncEnum.Local;
  private TimeZone           timeZone          = TimeUtils.getTimeZone();
  private File               trustFile;
  private String             trustPass;
  private File               userFile;
  private String             userName;
  private String             userPass;

  public CommandLineUtils(Class<? extends Object> usageClass) {
    this(usageClass == null ? null : usageClass.getName());
  }

  public CommandLineUtils(String usage) {
    options.addOption(Option.builder(HELP_SHORT_ARG).longOpt(HELP_ARG).required(false).desc("Display this help.").build());
    options.addOption(Option.builder().longOpt(PROPS_ARG).required(false).hasArg().argName("file/resource").optionalArg(false)
        .numberOfArgs(Option.UNLIMITED_VALUES).type(String.class).desc("Property file to load.").build());
    options.addOption(Option.builder().longOpt(PROP_PREFIX_ARG).required(false).hasArg().argName("prefix").optionalArg(false)
        .type(String.class).desc("Prefix to append option names with accessing properties.").build());

    this.usage = usage;

    baseDir = FileUtils.canonicalFile(new File(System.getProperty("user.dir")));

    props.add(System.getProperties());
  }

  public void displayHelp() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(usage, options);
  }

  void displayHelp(PrintWriter writer) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(writer, formatter.getWidth(), usage, null, options, formatter.getLeftPadding(), formatter.getDescPadding(),
        null, false);
  }

  public boolean getPropertyBoolean(String argName, boolean value) {
    boolean ret = value;

    if (cli == null || !cli.hasOption(argName)) {
      String temp = getPropertyString(argName);
      if (temp != null) {
        ret = Converter.parseBoolean(temp);
      }
    }

    return ret;
  }

  private File getPropertyFile(String argName, File value) {
    File ret = value;

    if (cli == null || !cli.hasOption(argName)) {
      String temp = getPropertyString(argName);
      if (temp != null) {
        ret = FileUtils.canonicalFile(new File(temp));
      }
    }

    return ret;
  }

  public int getPropertyInt(String argName, int value) {
    int ret = value;

    if (cli == null || !cli.hasOption(argName)) {
      String temp = getPropertyString(argName);
      if (temp != null) {
        ret = Converter.parseInt(temp);
      }
    }

    return ret;
  }

  public long getPropertyLong(String argName, long value) {
    long ret = value;

    if (cli == null || !cli.hasOption(argName)) {
      String temp = getPropertyString(argName);
      if (temp != null) {
        ret = Converter.parseLong(temp);
      }
    }

    return ret;
  }

  public String getPropertyString(String argName) {
    return getPropertyString(argName, null);
  }

  /**
   * Will search the properties for argName unless it was specified as a command line argument. The
   * propPrefix is used if specified.
   *
   * @param argName
   * @param value
   * @return
   */
  public String getPropertyString(String argName, String value) {
    String ret = value;

    if (cli == null || !cli.hasOption(argName)) {
      if (Compare.isNotBlank(argName) && props != null) {
        String name = propPrefix == null ? argName : propPrefix + argName;
        for (int xx = props.size() - 1; xx >= 0; xx--) {
          Properties prop = props.get(xx);
          String temp = prop.getProperty(name);
          if (temp != null) {
            ret = temp;
            break;
          }
        }
      }
    }

    return ret;
  }

  public SyncEnum getPropertySync(String argName, SyncEnum value) {
    SyncEnum ret = value;

    if (cli == null || !cli.hasOption(argName)) {
      String temp = getPropertyString(argName);
      if (Compare.isNotBlank(temp)) {
        ret = SyncEnum.valueOf(temp);
      }
    }

    return ret;
  }

  public TimeZone getPropertyTimeZone(String argName, TimeZone value) {
    TimeZone ret = value;

    if (cli == null || !cli.hasOption(argName)) {
      String temp = getPropertyString(argName);
      if (temp != null) {
        ret = Converter.parseTimeZone(temp);
      }
    }

    return ret;
  }

  public boolean isAllowDelete() {
    boolean ret = getPropertyBoolean(ALLOW_DELETE_ARG, allowDelete);

    logParameter("Allow Delete", ret);

    return ret;
  }

  public boolean isAllowWrite() {
    boolean ret = getPropertyBoolean(ALLOW_WRITE_ARG, allowWrite);

    logParameter("Allow Write", ret);

    return ret;
  }

  public File getBaseDir() {
    File ret = getPropertyFile(BASE_DIR_ARG, baseDir);

    logParameter("Base Dir", ret.getAbsolutePath());

    return ret;
  }

  public int getBlockSize() {
    int ret = getPropertyInt(BLOCK_SIZE_ARG, blockSize);

    logParameter("Block Size", ret);

    return ret;
  }

  public boolean isCompress() {
    boolean ret = getPropertyBoolean(COMPRESS_ARG, compress);

    logParameter("Compress", ret);

    return ret;
  }

  public String getEncPass() {
    String ret = getPropertyString(ENC_PASS_ARG, encPass);

    logParameter("Enc Pass", ret == null ? null : "********");

    return ret;
  }

  public String getEncUser() {
    String ret = getPropertyString(ENC_USER_ARG, encUser);

    logParameter("Enc Name", ret);

    return ret;
  }

  public SyncEnum getHash() {
    SyncEnum ret = getPropertySync(HASH_ARG, hash);

    logParameter("Hash", ret);

    return ret;
  }

  public String getHashType() {
    String ret = getPropertyString(HASH_TYPE_ARG, hashType);

    logParameter("Hash Type", ret);

    return ret;
  }

  public boolean isHelp() {
    return getPropertyBoolean(HELP_ARG, help);
  }

  public boolean isHiddenDirectories() {
    boolean ret = getPropertyBoolean(HIDDEN_DIRS_ARG, hiddenDirectories);

    logParameter("Hidden Directories", ret);

    return ret;
  }

  public boolean isHiddenFiles() {
    boolean ret = getPropertyBoolean(HIDDEN_FILES_ARG, hiddenFiles);

    logParameter("Hidden Files", ret);

    return ret;
  }

  public String getHost() {
    String ret = getPropertyString(HOST_ARG, host);

    logParameter("Host", ret);

    return ret;
  }

  public String getKeyPass() {
    String ret = getPropertyString(KEY_PASS_ARG, keyPass);

    logParameter("Key Pass", ret == null ? null : "********");

    return ret;
  }

  public int getMaxBlock() {
    int ret = getPropertyInt(MAX_BLOCK_ARG, maxBlock);

    logParameter("Max Block", ret);

    return ret;
  }

  public long getMaxOffset() {
    long ret = getPropertyLong(MAX_OFFSET_ARG, maxOffset);

    logParameter("Max Offset", ret);

    return ret;
  }

  public String getPath() {
    String ret = getPropertyString(PATH_ARG, path);

    logParameter("Path", ret);

    return ret;
  }

  public SyncEnum getPlan() {
    SyncEnum ret = getPropertySync(PLAN_ARG, plan);

    logParameter("Plan", ret);

    return ret;
  }

  public String getPlanEol() {
    String ret = getPropertyString(PLAN_EOL_ARG, planEol);

    logParameter("Plan EOL", ret);

    return ret;
  }

  public String getPlanFile() {
    String ret = getPropertyString(PLAN_FILE_ARG, planFile);

    logParameter("Plan File", ret);

    return ret;
  }

  public String getPlanTemplate() {
    String ret = getPropertyString(PLAN_TEMPLATE_ARG, planTemplate);

    logParameter("Plan Template", ret);

    return ret;
  }

  public int getPort() {
    int ret = getPropertyInt(PORT_ARG, port);

    logParameter("Port", ret);

    return ret;
  }

  public boolean isRecursive() {
    boolean ret = getPropertyBoolean(RECURSIVE_ARG, recursive);

    logParameter("Recursive", ret);

    return ret;
  }

  public boolean isReHash() {
    boolean ret = getPropertyBoolean(REHASH_ARG, reHash);

    logParameter("ReHash", ret);

    return ret;
  }

  public boolean isRemoteDelete() {
    boolean ret = getPropertyBoolean(REMOTE_DELETE_ARG, remoteDelete);

    logParameter("Remote Delete", ret);

    return ret;
  }

  public boolean isRemoteWrite() {
    boolean ret = getPropertyBoolean(REMOTE_WRITE_ARG, remoteWrite);

    logParameter("Remote Write", ret);

    return ret;
  }

  public SslEnum getSsl() {
    SslEnum ret = ssl;

    if (cli == null || !cli.hasOption(SSL_ARG)) {
      String temp = getPropertyString(SSL_ARG);
      if (temp != null) {
        ret = SslEnum.parseSsl(temp, ret);
      }
    }

    logParameter("SSL", ret);

    return ret;
  }

  public File getStoreFile() {
    File ret = getPropertyFile(STORE_FILE_ARG, storeFile);

    logParameter("Store File", ret);

    return ret;
  }

  public String getStorePass() {
    String ret = getPropertyString(STORE_PASS_ARG, storePass);

    logParameter("Store Pass", ret == null ? null : "********");

    return ret;
  }

  public SyncEnum getSync() {
    SyncEnum ret = getPropertySync(SYNC_ARG, sync);

    logParameter("Sync", ret);

    return ret;
  }

  public TimeZone getTimeZone() {
    TimeZone ret = getPropertyTimeZone(TIME_ZONE_ARG, timeZone);

    logParameter("Time Zone", String.format("%s (%s)", ret.getDisplayName(), ret.getID()));
    logParameter("Raw Offset", String.format("%d minutes", ret.getRawOffset() / 60000L));
    logParameter("DST Savings", String.format("%d minutes", ret.getDSTSavings() / 60000L));
    logParameter("Daylight Time", ret.useDaylightTime());

    return ret;
  }

  public File getTrustFile() {
    File ret = getPropertyFile(TRUST_FILE_ARG, trustFile);

    logParameter("Trust File", ret);

    return ret;
  }

  public String getTrustPass() {
    String ret = getPropertyString(TRUST_PASS_ARG, trustPass);

    logParameter("Trust Pass", ret == null ? null : "********");

    return ret;
  }

  public File getUserFile() {
    File ret = getPropertyFile(USER_FILE_ARG, userFile);

    logParameter("User File", ret);

    return ret;
  }

  public String getUserName() {
    String ret = getPropertyString(USER_NAME_ARG, userName);

    logParameter("User Name", ret);

    return ret;
  }

  public String getUserPass() {
    String ret = getPropertyString(USER_PASS_ARG, userPass);

    logParameter("User Pass", ret == null ? null : "********");

    return ret;
  }

  public boolean hasAllowDelete() {
    return cli != null && cli.hasOption(ALLOW_DELETE_ARG);
  }

  public boolean hasAllowWrite() {
    return cli != null && cli.hasOption(ALLOW_WRITE_ARG);
  }

  public boolean hasBaseDir() {
    return cli != null && cli.hasOption(BASE_DIR_ARG);
  }

  public boolean hasBlockSize() {
    return cli != null && cli.hasOption(BLOCK_SIZE_ARG);
  }

  public boolean hasCompress() {
    return cli != null && cli.hasOption(COMPRESS_ARG);
  }

  public boolean hasEncPass() {
    return cli != null && cli.hasOption(ENC_PASS_ARG);
  }

  public boolean hasEncUser() {
    return cli != null && cli.hasOption(ENC_USER_ARG);
  }

  public boolean hasHash() {
    return cli != null && cli.hasOption(HASH_ARG);
  }

  public boolean hasHashType() {
    return cli != null && cli.hasOption(HASH_TYPE_ARG);
  }

  public boolean hasHelp() {
    return cli != null && cli.hasOption(HELP_ARG);
  }

  public boolean hasHiddenDirectories() {
    return cli != null && cli.hasOption(HIDDEN_DIRS_ARG);
  }

  public boolean hasHiddenFiles() {
    return cli != null && cli.hasOption(HIDDEN_FILES_ARG);
  }

  public boolean hasHost() {
    return cli != null && cli.hasOption(HOST_ARG);
  }

  public boolean hasKeyPass() {
    return cli != null && cli.hasOption(KEY_PASS_ARG);
  }

  public boolean hasMaxBlock() {
    return cli != null && cli.hasOption(MAX_BLOCK_ARG);
  }

  public boolean hasMaxOffset() {
    return cli != null && cli.hasOption(MAX_OFFSET_ARG);
  }

  public boolean hasPath() {
    return cli != null && cli.hasOption(PATH_ARG);
  }

  public boolean hasPlan() {
    return cli != null && cli.hasOption(PLAN_ARG);
  }

  public boolean hasPlanEol() {
    return cli != null && cli.hasOption(PLAN_EOL_ARG);
  }

  public boolean hasPlanFile() {
    return cli != null && cli.hasOption(PLAN_FILE_ARG);
  }

  public boolean hasPlanTemplate() {
    return cli != null && cli.hasOption(PLAN_TEMPLATE_ARG);
  }

  public boolean hasPort() {
    return cli != null && cli.hasOption(PORT_ARG);
  }

  public boolean hasProps() {
    return cli != null && cli.hasOption(PROPS_ARG);
  }

  public boolean hasRecursive() {
    return cli != null && cli.hasOption(RECURSIVE_ARG);
  }

  public boolean hasReHash() {
    return cli != null && cli.hasOption(REHASH_ARG);
  }

  public boolean hasRemoteDelete() {
    return cli != null && cli.hasOption(REMOTE_DELETE_ARG);
  }

  public boolean hasRemoteWrite() {
    return cli != null && cli.hasOption(REMOTE_WRITE_ARG);
  }

  public boolean hasSsl() {
    return cli != null && cli.hasOption(SSL_ARG);
  }

  public boolean hasStoreFile() {
    return cli != null && cli.hasOption(STORE_FILE_ARG);
  }

  public boolean hasStorePass() {
    return cli != null && cli.hasOption(STORE_PASS_ARG);
  }

  public boolean hasSync() {
    return cli != null && cli.hasOption(SYNC_ARG);
  }

  public boolean hasTimeZone() {
    return cli != null && cli.hasOption(TIME_ZONE_ARG);
  }

  public boolean hasTrustFile() {
    return cli != null && cli.hasOption(TRUST_FILE_ARG);
  }

  public boolean hasTrustPass() {
    return cli != null && cli.hasOption(TRUST_PASS_ARG);
  }

  public boolean hasUserFile() {
    return cli != null && cli.hasOption(USER_FILE_ARG);
  }

  public boolean hasUserName() {
    return cli != null && cli.hasOption(USER_NAME_ARG);
  }

  public boolean hasUserPass() {
    return cli != null && cli.hasOption(USER_PASS_ARG);
  }

  public void logParameter(String label, boolean value) {
    logParameter(label, Boolean.toString(value));
  }

  public void logParameter(String label, int value) {
    logParameter(label, Integer.toString(value));
  }

  public void logParameter(String label, long value) {
    logParameter(label, Long.toString(value));
  }

  public void logParameter(String label, Object value) {
    String text = null;
    if (value == null) {
      text = "";
    } else if (value instanceof File) {
      text = ((File) value).getAbsolutePath();
    } else if (value.getClass().isEnum()) {
      text = ((Enum<?>) value).name();
    } else {
      text = String.valueOf(value);
    }

    logger.info(String.format("%-20s%s", label + ":", text));
  }

  public boolean parseArgs(String[] args) {
    boolean ret = true;

    try {
      parser = new DefaultParser();
      cli = parser.parse(options, args);

      props.clear();

      if (hasHelp()) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(getClass().getName(), options);
      } else {
        allowDelete = parseBoolean(ALLOW_DELETE_ARG, allowDelete);

        allowWrite = parseBoolean(ALLOW_WRITE_ARG, allowWrite);

        if (hasBaseDir()) {
          File dir = new File(cli.getOptionValue(BASE_DIR_ARG));
          dir.mkdirs();
          if (dir.isDirectory()) {
            baseDir = FileUtils.canonicalFile(dir);
          } else {
            throw new ParseException(String.format("Not directory %s: %s", BASE_DIR_ARG, dir.getAbsolutePath()));
          }
        }

        if (hasBlockSize()) {
          try {
            blockSize = Integer.parseInt(cli.getOptionValue(BLOCK_SIZE_ARG));
            if (blockSize < FileUtils.MIN_BLOCK_SIZE || blockSize > getMaxBlock() || blockSize % FileUtils.MIN_BLOCK_SIZE != 0) {
              throw new ParseException(String.format("Invalid %s: %s", BLOCK_SIZE_ARG, cli.getOptionValue(PORT_ARG)));
            }
          } catch (NumberFormatException ee) {
            throw new ParseException(String.format("Invalid %s: %s", BLOCK_SIZE_ARG, cli.getOptionValue(PORT_ARG)));
          }
        }

        compress = parseBoolean(COMPRESS_ARG, compress);

        if (hasEncPass()) {
          encPass = cli.getOptionValue(ENC_PASS_ARG);
          logParameter("Enc Pass", "********");
        }

        if (hasEncUser()) {
          encPass = cli.getOptionValue(ENC_USER_ARG);
          logParameter("Enc User", encUser);
        }

        if (hasHash()) {
          hash = SyncEnum.parseSync(cli.getOptionValue(HASH_ARG), hash);
        }

        if (hasHashType()) {
          hashType = cli.getOptionValue(HASH_TYPE_ARG);
        }

        hiddenDirectories = parseBoolean(HIDDEN_DIRS_ARG, hiddenDirectories);

        hiddenFiles = parseBoolean(HIDDEN_FILES_ARG, hiddenFiles);

        if (hasHost()) {
          host = cli.getOptionValue(HOST_ARG);
        }

        if (hasKeyPass()) {
          keyPass = cli.getOptionValue(KEY_PASS_ARG);
        }

        if (hasMaxBlock()) {
          try {
            maxBlock = Integer.parseInt(cli.getOptionValue(MAX_BLOCK_ARG));
            if (maxBlock < FileUtils.MIN_BLOCK_SIZE || maxBlock % FileUtils.MIN_BLOCK_SIZE != 0) {
              throw new ParseException(String.format("Invalid %s: %s", MAX_BLOCK_ARG, cli.getOptionValue(MAX_BLOCK_ARG)));
            }
          } catch (NumberFormatException ee) {
            throw new ParseException(String.format("Invalid %s: %s", MAX_BLOCK_ARG, cli.getOptionValue(MAX_BLOCK_ARG)));
          }
        }

        if (hasMaxOffset()) {
          try {
            maxOffset = Integer.parseInt(cli.getOptionValue(MAX_OFFSET_ARG));
            if (maxOffset < 0) {
              throw new ParseException(String.format("Invalid %s: %s", MAX_OFFSET_ARG, cli.getOptionValue(MAX_OFFSET_ARG)));
            }
          } catch (NumberFormatException ee) {
            throw new ParseException(String.format("Invalid %s: %s", MAX_OFFSET_ARG, cli.getOptionValue(MAX_OFFSET_ARG)));
          }
        }

        if (hasKeyPass()) {
          encPass = cli.getOptionValue(KEY_PASS_ARG);
        }

        if (hasPath()) {
          path = cli.getOptionValue(PATH_ARG);
        }

        if (hasPlan()) {
          plan = SyncEnum.parseSync(cli.getOptionValue(PLAN_ARG), plan);
        }

        if (hasPlanEol()) {
          planEol = cli.getOptionValue(PLAN_EOL_ARG);
        }

        if (hasPlanFile()) {
          planFile = cli.getOptionValue(PLAN_FILE_ARG);
        }

        if (hasPlanTemplate()) {
          planTemplate = cli.getOptionValue(PLAN_TEMPLATE_ARG);
        }

        if (hasPort()) {
          try {
            port = Integer.parseInt(cli.getOptionValue(PORT_ARG));
            if (port < 80 || port > 64000) {
              throw new ParseException(String.format("Invalid %s: %s", PORT_ARG, cli.getOptionValue(PORT_ARG)));
            }
          } catch (NumberFormatException ee) {
            throw new ParseException(String.format("Invalid %s: %s", PORT_ARG, cli.getOptionValue(PORT_ARG)));
          }
        }

        if (hasProps()) {
          for (String value : cli.getOptionValues(PROPS_ARG)) {
            try {
              InputStream in = null;
              File file = new File(value);
              if (file.exists()) {
                if (file.canRead()) {
                  in = new FileInputStream(file);
                  logParameter("Prop File", FileUtils.canonicalFile(file).getAbsolutePath());
                } else {
                  throw new IOException();
                }
              } else {
                in = getClass().getClassLoader().getResourceAsStream(value);
                if (in != null) {
                  logParameter("Prop Resource", value);
                }
              }
              if (in == null) {
                throw new IOException();
              } else {
                Properties prop = new Properties();
                prop.load(in);
                in.close();
                props.add(prop);
              }
            } catch (IOException ee) {
              throw new ParseException(String.format("Invalid %s: %s", PROPS_ARG, value));
            }
          }
        }

        recursive = parseBoolean(RECURSIVE_ARG, recursive);

        reHash = parseBoolean(REHASH_ARG, reHash);

        remoteDelete = parseBoolean(REMOTE_DELETE_ARG, remoteDelete);

        remoteWrite = parseBoolean(REMOTE_WRITE_ARG, remoteWrite);

        if (hasSsl()) {
          ssl = SslEnum.parseSsl(cli.getOptionValue(SSL_ARG), SslEnum.OneWay);
        }

        if (hasStoreFile()) {
          File file = new File(cli.getOptionValue(STORE_FILE_ARG));
          if (file.canRead()) {
            storeFile = FileUtils.canonicalFile(file);
          } else {
            throw new ParseException(String.format("Unable read %s: %s", STORE_FILE_ARG, file.getAbsolutePath()));
          }
        }

        if (hasStorePass()) {
          storePass = cli.getOptionValue(STORE_PASS_ARG);
        }

        if (hasSync()) {
          sync = SyncEnum.parseSync(cli.getOptionValue(SYNC_ARG), sync);
        }

        if (hasTimeZone()) {
          String id = cli.getOptionValue(TIME_ZONE_ARG);
          try {
            timeZone = Converter.parseTimeZone(id);
          } catch (IllegalArgumentException ee) {
            throw new ParseException(String.format("Invalid %s: %s", TIME_ZONE_ARG, ee.toString()));
          }
        }
        if (hasTrustFile()) {
          File file = new File(cli.getOptionValue(TRUST_FILE_ARG));
          if (file.canRead()) {
            trustFile = FileUtils.canonicalFile(file);
          } else {
            throw new ParseException(String.format("Unable read %s: %s", TRUST_FILE_ARG, file.getAbsolutePath()));
          }
        }

        if (hasTrustPass()) {
          trustPass = cli.getOptionValue(TRUST_PASS_ARG);
        }

        if (hasUserFile()) {
          File file = new File(cli.getOptionValue(USER_FILE_ARG));
          if (file.canRead()) {
            userFile = FileUtils.canonicalFile(file);
          } else {
            throw new ParseException(String.format("Unable read %s: %s", USER_FILE_ARG, file.getAbsolutePath()));
          }
        }

        if (hasUserName()) {
          userName = cli.getOptionValue(USER_NAME_ARG);
        }

        if (hasUserPass()) {
          userPass = cli.getOptionValue(USER_PASS_ARG);
        }

        // This is last and highest priority
        props.add(System.getProperties());
      }
    } catch (ParseException ee) {
      ret = false;

      System.err.println(String.format("%s\n", ee.getMessage()));

      displayHelp();
    }

    return ret;
  }

  private boolean parseBoolean(String argName, boolean value) {
    boolean ret = value;

    if (cli != null && cli.hasOption(argName)) {
      String temp = cli.getOptionValue(argName);
      if (Compare.isBlank(temp)) {
        ret = true;
      } else {
        ret = Converter.parseBoolean(temp, value);
      }
    }

    return ret;
  }

  public CommandLineUtils useAllow() {
    options.addOption(Option.builder().longOpt(ALLOW_DELETE_ARG).required(false).hasArg().argName("allow").optionalArg(true)
        .type(Boolean.class).desc("Allow deleting locally.").build());
    options.addOption(Option.builder().longOpt(ALLOW_WRITE_ARG).required(false).hasArg().argName("allow").optionalArg(true)
        .type(Boolean.class).desc("Allow writing locally.").build());
    options.addOption(Option.builder().longOpt(REMOTE_DELETE_ARG).required(false).hasArg().argName("allow").optionalArg(true)
        .type(Boolean.class).desc("Allow deleting remotely.").build());
    options.addOption(Option.builder().longOpt(REMOTE_WRITE_ARG).required(false).hasArg().argName("allow").optionalArg(true)
        .type(Boolean.class).desc("Allow writing remotely.").build());
    return this;
  }

  public CommandLineUtils useBaseDir() {
    options.addOption(Option.builder().longOpt(BASE_DIR_ARG).required(false).hasArg().argName("dir").optionalArg(false)
        .type(String.class).desc("The base directory for all directory/file requests.").build());
    options.addOption(Option.builder().longOpt(PATH_ARG).required(false).hasArg().argName("path").optionalArg(false)
        .type(String.class).desc("The directory path within the base directory.").build());
    return this;
  }

  public CommandLineUtils useBlockSize() {
    options.addOption(Option.builder().longOpt(BLOCK_SIZE_ARG).required(false).hasArg().argName("bytes").optionalArg(false)
        .type(Integer.class).desc("The size of the block.").build());
    return this;
  }

  public CommandLineUtils useClient() {
    options.addOption(Option.builder().longOpt(SSL_ARG).required(false).hasArg().argName("ssl").optionalArg(true)
        .type(SslEnum.class).desc(String.format("Use ssl. %s", Arrays.toString(SslEnum.values()))).build());
    options.addOption(Option.builder().longOpt(HOST_ARG).required(false).hasArg().argName("host").optionalArg(false)
        .type(String.class).desc("The host name for the remote server").build());
    options.addOption(Option.builder().longOpt(PORT_ARG).required(false).hasArg().argName("port").optionalArg(false)
        .type(Integer.class).desc("The port name for the remote server.").build());
    return this;
  }

  public CommandLineUtils useEncPass() {
    options.addOption(Option.builder().longOpt(ENC_PASS_ARG).required(false).hasArg().argName("password").optionalArg(false)
        .type(String.class).desc("Encrypt a password for Jetty.").build());
    options.addOption(Option.builder().longOpt(ENC_USER_ARG).required(false).hasArg().argName("userName").optionalArg(false)
        .type(String.class).desc("Encrypt a user/password for Jetty using Crypt.").build());
    return this;
  }

  public CommandLineUtils useHash() {
    options.addOption(Option.builder().longOpt(HASH_ARG).required(false).hasArg().argName("where").optionalArg(false)
        .type(SyncEnum.class).desc(String.format("Request hashing. %s", Arrays.toString(SyncEnum.values()))).build());
    options.addOption(Option.builder().longOpt(HIDDEN_DIRS_ARG).required(false).hasArg().argName("include").optionalArg(true)
        .type(Boolean.class).desc("Include hidden directories.").build());
    options.addOption(Option.builder().longOpt(HIDDEN_FILES_ARG).required(false).hasArg().argName("include").optionalArg(true)
        .type(Boolean.class).desc("Include hidden files.").build());
    options.addOption(Option.builder().longOpt(RECURSIVE_ARG).required(false).hasArg().argName("recurse").optionalArg(true)
        .type(Boolean.class).desc("Process directories recursively.").build());
    options.addOption(Option.builder().longOpt(REHASH_ARG).required(false).hasArg().argName("rehash").optionalArg(true)
        .type(Boolean.class).desc("Rehash already hashed files.").build());
    return this;
  }

  public CommandLineUtils useHashType() {
    options.addOption(Option.builder().longOpt(HASH_TYPE_ARG).required(false).hasArg().argName("algorithm").optionalArg(false)
        .type(String.class).desc("The algorithm for generating the hash.").build());
    return this;
  }

  public CommandLineUtils useKeyStore() {
    options.addOption(Option.builder().longOpt(STORE_FILE_ARG).required(false).hasArg().argName("fileName").optionalArg(false)
        .type(String.class).desc("File name for the key store.").build());
    options.addOption(Option.builder().longOpt(STORE_PASS_ARG).required(false).hasArg().argName("password").optionalArg(false)
        .type(String.class).desc("Password for the key store.").build());
    options.addOption(Option.builder().longOpt(KEY_PASS_ARG).required(false).hasArg().argName("password").optionalArg(false)
        .type(String.class).desc("Password for the private key.").build());
    return this;
  }

  public CommandLineUtils useMax() {
    options.addOption(Option.builder().longOpt(MAX_BLOCK_ARG).required(false).hasArg().argName("bytes").optionalArg(false)
        .type(Integer.class).desc("Maximum block size.").build());
    options.addOption(Option.builder().longOpt(MAX_OFFSET_ARG).required(false).hasArg().argName("bytes").optionalArg(false)
        .type(Long.class).desc("Maximum offset.").build());
    return this;
  }

  public CommandLineUtils usePlan() {
    options.addOption(Option
        .builder()
        .longOpt(PLAN_ARG)
        .required(false)
        .hasArg()
        .argName("direction")
        .optionalArg(false)
        .type(String.class)
        .desc(
            String.format("Produce a plan report descibing what running %s would do. %s", SYNC_ARG,
                Arrays.toString(SyncEnum.values()))).build());
    options.addOption(Option.builder().longOpt(PLAN_EOL_ARG).required(false).hasArg().argName("string").optionalArg(false)
        .type(String.class).desc("Trim existing eol from report and replace string with new line.").build());
    options.addOption(Option.builder().longOpt(PLAN_FILE_ARG).required(false).hasArg().argName("path/resource").optionalArg(false)
        .type(String.class).desc("Output file for plan report.").build());
    options.addOption(Option.builder().longOpt(PLAN_TEMPLATE_ARG).required(false).hasArg().argName("path/resource")
        .optionalArg(false).type(String.class).desc("JMTE template for the plan report.").build());
    options.addOption(Option.builder().longOpt(HIDDEN_DIRS_ARG).required(false).hasArg().argName("include").optionalArg(true)
        .type(Boolean.class).desc("Include hidden directories.").build());
    options.addOption(Option.builder().longOpt(HIDDEN_FILES_ARG).required(false).hasArg().argName("include").optionalArg(true)
        .type(Boolean.class).desc("Include hidden files.").build());
    options.addOption(Option.builder().longOpt(RECURSIVE_ARG).required(false).hasArg().argName("recurse").optionalArg(true)
        .type(Boolean.class).desc("Process directories recursively.").build());
    return this;
  }

  public CommandLineUtils useServer() {
    options.addOption(Option.builder().longOpt(SSL_ARG).required(false).hasArg().argName("ssl").optionalArg(true)
        .type(SslEnum.class).desc(String.format("Use ssl. %s", Arrays.toString(SslEnum.values()))).build());
    options.addOption(Option.builder().longOpt(PORT_ARG).required(false).hasArg().argName("port").optionalArg(false)
        .type(Integer.class).desc("The port name for the remote server.").build());
    return this;
  }

  public CommandLineUtils useSync() {
    options.addOption(Option.builder().longOpt(COMPRESS_ARG).required(false).hasArg().argName("compress").optionalArg(true)
        .type(Boolean.class).desc("Compress remote blocks.").build());
    options.addOption(Option.builder().longOpt(HIDDEN_DIRS_ARG).required(false).hasArg().argName("include").optionalArg(true)
        .type(Boolean.class).desc("Include hidden directories.").build());
    options.addOption(Option.builder().longOpt(HIDDEN_FILES_ARG).required(false).hasArg().argName("include").optionalArg(true)
        .type(Boolean.class).desc("Include hidden files.").build());
    options.addOption(Option.builder().longOpt(RECURSIVE_ARG).required(false).hasArg().argName("recurse").optionalArg(true)
        .type(Boolean.class).desc("Process directories recursively.").build());
    options.addOption(Option.builder().longOpt(SYNC_ARG).required(false).hasArg().argName("direction").optionalArg(false)
        .type(String.class).desc(String.format("Perform a synchronization. %s", Arrays.toString(SyncEnum.values()))).build());
    return this;
  }

  public CommandLineUtils useTimeZone() {
    options.addOption(Option.builder().longOpt(TIME_ZONE_ARG).required(false).hasArg().argName("timeZone").optionalArg(false)
        .type(String.class).desc("Local time zone. Supports GMT(+|-)HH[[:]MM]").build());
    return this;
  }

  public CommandLineUtils useTrustStore() {
    options.addOption(Option.builder().longOpt(TRUST_FILE_ARG).required(false).hasArg().argName("fileName").optionalArg(false)
        .type(String.class).desc("File name for the trust store.").build());
    options.addOption(Option.builder().longOpt(TRUST_PASS_ARG).required(false).hasArg().argName("password").optionalArg(false)
        .type(String.class).desc("Password for the trust store.").build());
    return this;
  }

  public CommandLineUtils useUserAuth() {
    options.addOption(Option.builder().longOpt(USER_NAME_ARG).required(false).hasArg().argName("userName").optionalArg(false)
        .type(String.class).desc("User name for authentication.").build());
    options.addOption(Option.builder().longOpt(USER_PASS_ARG).required(false).hasArg().argName("password").optionalArg(false)
        .type(String.class).desc("User password for authentication.").build());
    return this;
  }

  public CommandLineUtils useUserFile() {
    options.addOption(Option.builder().longOpt(USER_FILE_ARG).required(false).hasArg().argName("fileName").optionalArg(false)
        .type(String.class)
        .desc(String.format("File containing 'userName:password,%s' for user authentication.", Constants.USER_ROLE)).build());
    return this;
  }

}