package com.beijunyi.parallelgit.filesystem.commands;

import java.io.IOException;

import com.beijunyi.parallelgit.filesystem.PreSetupGitFileSystemTest;
import com.beijunyi.parallelgit.filesystem.commands.GfsCreateStash.Result;
import com.beijunyi.parallelgit.filesystem.exceptions.UnsuccessfulOperationException;
import com.beijunyi.parallelgit.utils.CommitUtils;
import com.beijunyi.parallelgit.utils.TreeUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

import static com.beijunyi.parallelgit.filesystem.Gfs.*;
import static org.junit.Assert.*;

public class GfsCreateStashTest extends PreSetupGitFileSystemTest {

  @Test
  public void whenFileSystemIsClean_stashShouldBeUnsuccessful() throws IOException {
    Result result = stash(gfs).execute();
    assertFalse(result.isSuccessful());
  }

  @Test(expected = UnsuccessfulOperationException.class)
  public void whenStashIsUnsuccessful_getCommitShouldThrowUnsuccessfulOperationException() throws IOException {
    Result result = stash(gfs).execute();
    result.getCommit();
  }

  @Test
  public void afterChangesAreStashed_fileSystemShouldBecomeClean() throws IOException {
    writeSomeFileToGfs();
    stash(gfs).execute();
    assertFalse(isDirty(gfs));
  }

  @Test
  public void stashWithWorkingDirectoryMessage_theWorkingDirectoryCommitMessageShouldContainTheSpecifiedMessage() throws IOException {
    writeSomeFileToGfs();
    String message = "test working directory message";
    RevCommit workDirCommit = stash(gfs).workingDirectoryMessage(message).execute().getCommit();
    assertTrue(workDirCommit.getFullMessage().equals(message));
  }

  @Test
  public void getParentCountOfTheStashCommit_shouldReturnTwo() throws IOException {
    writeSomeFileToGfs();
    Result result = stash(gfs).execute();
    RevCommit workDirCommit = result.getCommit();
    assertEquals(2, workDirCommit.getParentCount());
  }

  @Test
  public void getFirstParentOfTheWorkDirCommitCommit_shouldReturnTheHeadCommit() throws IOException {
    writeSomeFileToGfs();
    RevCommit head = gfs.getStatusProvider().commit();
    Result result = stash(gfs).execute();
    RevCommit workDirCommit = result.getCommit();
    assertEquals(head, workDirCommit.getParent(0));
  }

  @Test
  public void getSecondParentOfTheWorkDirCommitCommit_shouldReturnTheIndexCommitWhichHasTheSameTreeAsTheStashCommit() throws IOException {
    writeSomeFileToGfs();
    Result result = stash(gfs).execute();
    RevCommit workDirCommit = result.getCommit();
    RevCommit indexCommit = CommitUtils.getCommit(workDirCommit.getParent(1), repo);
    assertEquals(workDirCommit.getTree(), indexCommit.getTree());
  }

  @Test
  public void stashWithIndexMessage_theSecondParentOfTheWorkDirCommitMessageShouldContainTheSpecifiedMessage() throws IOException {
    writeSomeFileToGfs();
    String message = "test index message";
    RevCommit stash = stash(gfs).indexMessage(message).execute().getCommit();
    assertTrue(CommitUtils.getCommit(stash.getParent(1), repo).getFullMessage().equals(message));
  }

  @Test
  public void stashWithCommitter_bothWorkingDirectoryCommitAndIndexCommitShouldBeCommittedByTheSpecifiedCommitter() throws IOException {
    writeSomeFileToGfs();
    PersonIdent committer = somePersonIdent();
    RevCommit workDirCommit = stash(gfs).committer(committer).execute().getCommit();
    assertEquals(committer, workDirCommit.getCommitterIdent());
    RevCommit indexCommit = CommitUtils.getCommit(workDirCommit.getParent(1), repo);
    assertEquals(committer, workDirCommit.getCommitterIdent());
    assertEquals(committer, indexCommit.getCommitterIdent());
  }

  @Test
  public void getTreeOfWorkDirCommit_shouldContainTheStashedChanges() throws IOException {
    byte[] expected = "new file".getBytes();
    writeToGfs("/test_file.txt", expected);
    Result result = stash(gfs).execute();
    RevCommit stash = result.getCommit();
    ObjectId tree = stash.getTree();
    byte[] actual = TreeUtils.readFile("/test_file.txt", tree, repo).getData();
    assertArrayEquals(expected, actual);
  }

}
