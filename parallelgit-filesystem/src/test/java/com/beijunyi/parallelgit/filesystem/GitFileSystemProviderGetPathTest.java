package com.beijunyi.parallelgit.filesystem;

import java.io.IOException;
import java.net.URI;

import com.beijunyi.parallelgit.filesystem.utils.GitUriBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GitFileSystemProviderGetPathTest extends AbstractGitFileSystemTest {

  @Before
  public void setupFileSystem() throws IOException {
    initGitFileSystem();
  }

  @Test
  public void getPathFromUri() {
    URI uri = GitUriBuilder.prepare()
                .file("/some_file.txt")
                .session(gfs.getSessionId())
                .build();
    GitPath path = provider.getPath(uri);
    Assert.assertEquals(gfs.getPath("/some_file.txt"), path);
  }

  @Test
  public void getRootPathFromUri() {
    URI uri = GitUriBuilder.prepare()
                .file("/")
                .session(gfs.getSessionId())
                .build();
    GitPath path = provider.getPath(uri);
    Assert.assertEquals(gfs.getRoot(), path);
  }

  @Test
  public void getPathFromUriWithNoSessionId() {
    URI uri = GitUriBuilder.prepare()
                .file("/some_file.txt")
                .build();
    GitPath path = provider.getPath(uri);
    Assert.assertNotNull(path);
  }

}
