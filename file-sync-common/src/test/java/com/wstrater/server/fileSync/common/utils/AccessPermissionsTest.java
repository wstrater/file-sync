package com.wstrater.server.fileSync.common.utils;

import static com.wstrater.server.fileSync.common.utils.AccessUtils.access;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AccessPermissionsTest {

  @Test
  public void testLocalDelete() throws Exception {
    assertTrue("Can't delete local file",
        AccessPermissions.localOnly(access().existsDir().deleteDir().existsFile().deleteFile().get()).isLocalDelete());

    assertFalse("Can delete local file without file", AccessPermissions.localOnly(access().existsDir().deleteDir().get())
        .isLocalDelete());
    assertFalse("Can delete local file without dir", AccessPermissions.localOnly(access().existsFile().deleteFile().get())
        .isLocalDelete());
    assertFalse("Can delete local file without local", AccessPermissions.remoteOnly(access().get()).isLocalDelete());
  }

  @Test
  public void testLocalWrite() throws Exception {
    assertTrue("Can't write local file without file",
        AccessPermissions.both(access().existsDir().writeDir().get(), access().existsDir().readDir().existsFile().readFile().get())
            .isLocalWrite());
    assertTrue(
        "Can't write local file",
        AccessPermissions.both(access().existsDir().writeDir().existsFile().writeFile().get(),
            access().existsDir().readDir().existsFile().readFile().get()).isLocalWrite());

    assertFalse("Can write local file without remote", AccessPermissions.localOnly(access().existsDir().writeDir().get())
        .isLocalWrite());
    assertFalse("Can write local file without local",
        AccessPermissions.remoteOnly(access().existsDir().readDir().existsFile().readFile().get()).isLocalWrite());
    assertFalse(
        "Can write local file without remote read",
        AccessPermissions.both(access().existsDir().writeDir().existsFile().writeFile().get(),
            access().existsDir().readDir().existsFile().get()).isLocalWrite());
    assertFalse(
        "Can write local file without local write",
        AccessPermissions.both(access().existsDir().writeDir().existsFile().get(),
            access().existsDir().readDir().existsFile().readFile().get()).isLocalWrite());
  }

  @Test
  public void testRemoteDelete() throws Exception {
    assertTrue("Can't delete remote file",
        AccessPermissions.remoteOnly(access().existsDir().deleteDir().existsFile().deleteFile().get()).isRemoteDelete());

    assertFalse("Can delete remote file without file", AccessPermissions.remoteOnly(access().existsDir().deleteDir().get())
        .isRemoteDelete());
    assertFalse("Can delete remote file without dir", AccessPermissions.remoteOnly(access().existsFile().deleteFile().get())
        .isRemoteDelete());
    assertFalse("Can delete remote file without remote", AccessPermissions.localOnly(access().get()).isRemoteDelete());
  }

  @Test
  public void testRemoteWrite() throws Exception {
    assertTrue("Can't write remote file without file",
        AccessPermissions.both(access().existsDir().readDir().existsFile().readFile().get(), access().existsDir().writeDir().get())
            .isRemoteWrite());
    assertTrue(
        "Can't write remote file",
        AccessPermissions.both(access().existsDir().readDir().existsFile().readFile().get(),
            access().existsDir().writeDir().existsFile().writeFile().get()).isRemoteWrite());

    assertFalse("Can write remote file without local", AccessPermissions.remoteOnly(access().existsDir().writeDir().get())
        .isRemoteWrite());
    assertFalse("Can write remote file without remote",
        AccessPermissions.localOnly(access().existsDir().readDir().existsFile().readFile().get()).isRemoteWrite());
    assertFalse(
        "Can write remote file without local read",
        AccessPermissions.both(access().existsDir().readDir().existsFile().get(),
            access().existsDir().writeDir().existsFile().writeFile().get()).isRemoteWrite());
    assertFalse(
        "Can write remote file without remote write",
        AccessPermissions.both(access().existsDir().readDir().existsFile().readFile().get(),
            access().existsDir().writeDir().existsFile().get()).isRemoteWrite());
  }

}