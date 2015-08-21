package com.beijunyi.parallelgit.runtime;

import java.io.IOException;

import com.beijunyi.parallelgit.AbstractParallelGitTest;
import com.beijunyi.parallelgit.utils.BlobHelper;
import com.beijunyi.parallelgit.utils.RevTreeHelper;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Assert;
import org.junit.Test;

public class ParallelCacheCommandTest extends AbstractParallelGitTest {

  @Test
  public void buildEmptyCacheTest() throws IOException {
    DirCache cache = ParallelCacheCommand.prepare().call();
    Assert.assertEquals(0, cache.getEntryCount());
  }

  @Test
  public void buildCacheFromCommitIdTest() throws IOException {
    initRepository();
    String expectedFilePath = "expected_file.txt";
    ObjectId expectedFileBlob = writeToCache(expectedFilePath);
    ObjectId commitId = commitToMaster();
    DirCache cache = ParallelCacheCommand.prepare(repo)
                       .baseCommit(commitId)
                       .call();
    Assert.assertEquals(1, cache.getEntryCount());
    Assert.assertEquals(expectedFileBlob, cache.getEntry(expectedFilePath).getObjectId());
  }

  @Test
  public void buildCacheFromCommitIdStringTest() throws IOException {
    initRepository();
    String expectedFilePath = "expected_file.txt";
    ObjectId expectedFileBlob = writeToCache(expectedFilePath);
    String commitIdStr = commitToMaster().getName();
    DirCache cache = ParallelCacheCommand.prepare(repo)
                       .baseCommit(commitIdStr)
                       .call();
    Assert.assertEquals(1, cache.getEntryCount());
    Assert.assertEquals(expectedFileBlob, cache.getEntry(expectedFilePath).getObjectId());
  }

  @Test
  public void buildCacheFromTreeIdTest() throws IOException {
    initRepository();
    String expectedFilePath = "expected_file.txt";
    ObjectId expectedFileBlob = writeToCache(expectedFilePath);
    ObjectId treeId = RevTreeHelper.getRootTree(repo, commitToMaster());
    DirCache cache = ParallelCacheCommand.prepare(repo)
                       .baseTree(treeId)
                       .call();
    Assert.assertEquals(1, cache.getEntryCount());
    Assert.assertEquals(expectedFileBlob, cache.getEntry(expectedFilePath).getObjectId());
  }

  @Test
  public void buildCacheFromTreeIdStringTest() throws IOException {
    initRepository();
    String expectedFilePath = "expected_file.txt";
    ObjectId expectedFileBlob = writeToCache(expectedFilePath);
    String treeIdStr = RevTreeHelper.getRootTree(repo, commitToMaster()).getName();
    DirCache cache = ParallelCacheCommand.prepare(repo)
                       .baseTree(treeIdStr)
                       .call();
    Assert.assertEquals(1, cache.getEntryCount());
    Assert.assertEquals(expectedFileBlob, cache.getEntry(expectedFilePath).getObjectId());
  }

  @Test
  public void addBlobTest() throws IOException {
    ObjectId blobId = BlobHelper.getBlobId(getClass().getName());
    String path = "test.txt";
    DirCache cache = ParallelCacheCommand.prepare()
                       .addBlob(blobId, path)
                       .call();
    Assert.assertEquals(1, cache.getEntryCount());
    DirCacheEntry entry = cache.getEntry(path);
    Assert.assertEquals(blobId, entry.getObjectId());
    Assert.assertEquals(FileMode.REGULAR_FILE, entry.getFileMode());
  }

  @Test
  public void addBlobWithSpecifiedFileModeTest() throws IOException {
    ObjectId blobId = BlobHelper.getBlobId(getClass().getName());
    String path = "test.txt";
    FileMode mode = FileMode.EXECUTABLE_FILE;
    DirCache cache = ParallelCacheCommand.prepare()
                       .addBlob(blobId, mode, path)
                       .call();
    Assert.assertEquals(1, cache.getEntryCount());
    DirCacheEntry entry = cache.getEntry(path);
    Assert.assertEquals(blobId, entry.getObjectId());
    Assert.assertEquals(mode, entry.getFileMode());
  }


  @Test
  public void addTreeTest() throws IOException {
    initRepository();
    ObjectId blob1 = writeToCache("1.txt");
    ObjectId blob2 = writeToCache("2.txt");
    ObjectId treeId = RevTreeHelper.getRootTree(repo, commitToMaster());
    DirCache cache = ParallelCacheCommand.prepare(repo)
                       .addTree(treeId, "base")
                       .call();
    Assert.assertEquals(2, cache.getEntryCount());
    Assert.assertEquals(blob1, cache.getEntry("base/1.txt").getObjectId());
    Assert.assertEquals(blob2, cache.getEntry("base/2.txt").getObjectId());
  }

  @Test
  public void addTreeFromTreeIdStringTest() throws IOException {
    initRepository();
    ObjectId blob1 = writeToCache("1.txt");
    ObjectId blob2 = writeToCache("2.txt");
    String treeIdStr = RevTreeHelper.getRootTree(repo, commitToMaster()).getName();
    DirCache cache = ParallelCacheCommand.prepare(repo)
                       .addTree(treeIdStr, "base")
                       .call();
    Assert.assertEquals(2, cache.getEntryCount());
    Assert.assertEquals(blob1, cache.getEntry("base/1.txt").getObjectId());
    Assert.assertEquals(blob2, cache.getEntry("base/2.txt").getObjectId());
  }

  @Test(expected = IllegalArgumentException.class)
  public void addTreeWithoutRepositoryTest() throws IOException {
    ParallelCacheCommand
      .prepare()
      .addTree(ObjectId.zeroId(), "somepath")
      .call();
  }

  @Test
  public void deleteBlobTest() throws IOException {
    initRepository();
    writeToCache("1.txt");
    writeToCache("2.txt");
    ObjectId commitId = commitToMaster();
    DirCache cache = ParallelCacheCommand
                       .prepare(repo)
                       .baseCommit(commitId)
                       .deleteBlob("1.txt")
                       .call();
    Assert.assertEquals(1, cache.getEntryCount());
    Assert.assertNull(cache.getEntry("1.txt"));
    Assert.assertNotNull(cache.getEntry("2.txt"));
  }

  @Test
  public void deleteTreeTest() throws IOException {
    initRepository();
    writeToCache("a/b/1.txt");
    writeToCache("a/b/2.txt");
    writeToCache("a/3.txt");
    ObjectId commitId = commitToMaster();
    DirCache cache = ParallelCacheCommand
                       .prepare(repo)
                       .baseCommit(commitId)
                       .deleteTree("a/b")
                       .call();
    Assert.assertEquals(1, cache.getEntryCount());
    Assert.assertNull(cache.getEntry("a/b/1.txt"));
    Assert.assertNull(cache.getEntry("a/b/2.txt"));
    Assert.assertNotNull(cache.getEntry("a/3.txt"));
  }

  @Test
  public void updateBlobTest() throws IOException {
    initRepository();
    writeToCache("1.txt", "some content");
    ObjectId commitId = commitToMaster();
    ObjectId newBlobId = BlobHelper.getBlobId("some other content");
    FileMode newFileMode = FileMode.EXECUTABLE_FILE;
    DirCache cache = ParallelCacheCommand
                       .prepare(repo)
                       .baseCommit(commitId)
                       .updateBlob(newBlobId, newFileMode, "1.txt")
                       .call();
    DirCacheEntry entry = cache.getEntry("1.txt");
    Assert.assertEquals(newBlobId, entry.getObjectId());
    Assert.assertEquals(newFileMode, entry.getFileMode());
  }

  @Test
  public void updateBlobIdTest() throws IOException {
    initRepository();
    writeToCache("1.txt", "some content");
    ObjectId commitId = commitToMaster();
    ObjectId newBlobId = BlobHelper.getBlobId("some other content");
    DirCache cache = ParallelCacheCommand
                       .prepare(repo)
                       .baseCommit(commitId)
                       .updateBlob(newBlobId, "1.txt")
                       .call();
    DirCacheEntry entry = cache.getEntry("1.txt");
    Assert.assertEquals(newBlobId, entry.getObjectId());
  }

  @Test
  public void updateBlobFileModeTest() throws IOException {
    initRepository();
    writeToCache("1.txt", "some content");
    ObjectId commitId = commitToMaster();
    FileMode newFileMode = FileMode.EXECUTABLE_FILE;
    DirCache cache = ParallelCacheCommand
                       .prepare(repo)
                       .baseCommit(commitId)
                       .updateBlob(newFileMode, "1.txt")
                       .call();
    DirCacheEntry entry = cache.getEntry("1.txt");
    Assert.assertEquals(newFileMode, entry.getFileMode());
  }

}