package com.beijunyi.parallelgit.utils;

import java.io.IOException;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

public final class BranchUtils {

  @Nonnull
  public static List<RevCommit> getBranchHistory(@Nonnull String name, @Nonnull Repository repo) throws IOException {
    String branchRef = RefUtils.ensureBranchRefName(name);
    RevCommit head = CommitUtils.getCommit(repo, branchRef);
    if(head == null)
      throw new IllegalArgumentException("Branch " + name + " does not exist");
    return CommitUtils.getCommitHistory(repo, head);
  }

  public static boolean existsBranch(@Nonnull String name, @Nonnull Repository repo) throws IOException {
    Ref ref = repo.getRef(name);
    return ref != null && RefUtils.isBranchRef(ref);
  }

  @Nullable
  public static AnyObjectId getBranchHeadCommitId(@Nonnull String name, @Nonnull Repository repo) throws IOException {
    return repo.resolve(RefUtils.ensureBranchRefName(name));
  }

  @Nonnull
  public static RefUpdate.Result createBranch(@Nonnull String name, @Nonnull String revision, @Nonnull Repository repo, boolean force) throws IOException {
    String branchRef = RefUtils.ensureBranchRefName(name);
    boolean exists = existsBranch(branchRef, repo);
    if(exists && !force)
      throw new IllegalArgumentException("Branch " + name + " already exists");

    AnyObjectId revisionId = repo.resolve(revision);
    if(revisionId == null)
      throw new IllegalArgumentException("Could not find revision " + revision);

    Ref baseRef = repo.getRef(revision);

    RevWalk revWalk = new RevWalk(repo);
    String refLogMessage;
    if(baseRef == null) {
      RevCommit commit = revWalk.parseCommit(revisionId);
      refLogMessage = "branch: " + (exists ? "Reset start-point to commit" : "Created from commit") + " " + commit.getShortMessage();
    } else {
      if(RefUtils.isBranchRef(baseRef))
        refLogMessage = "branch: " + (exists ? "Reset start-point to branch" : "Created from branch") + " " + baseRef.getName();
      else if (RefUtils.isTagRef(baseRef)) {
        revisionId = revWalk.peel(revWalk.parseAny(revisionId));
        refLogMessage = "branch: " + (exists ? "Reset start-point to tag" : "Created from tag") + " " + baseRef.getName();
      } else
        throw new IllegalArgumentException("Unknown ref " + baseRef);
    }
    RefUpdate update = repo.updateRef(branchRef);
    update.setNewObjectId(revisionId);
    update.setRefLogMessage(refLogMessage, false);
    update.setForceUpdate(force);
    return update.update();
  }

  @Nonnull
  public static RefUpdate.Result setBranchHead(@Nonnull String name, @Nonnull AnyObjectId commitId, @Nonnull Repository repo, @Nonnull String refLogMessage, boolean forceUpdate) throws IOException {
    String refName = RefUtils.ensureBranchRefName(name);
    AnyObjectId currentHead = repo.resolve(refName);
    if(currentHead == null)
      currentHead = ObjectId.zeroId();

    RefUpdate ru = repo.updateRef(refName);
    ru.setRefLogMessage(refLogMessage, false);
    ru.setForceUpdate(forceUpdate);
    ru.setNewObjectId(commitId);
    ru.setExpectedOldObjectId(currentHead);
    return ru.update();
  }

  @Nonnull
  public static RefUpdate.Result resetBranchHead(@Nonnull String name, @Nonnull AnyObjectId commitId, @Nonnull Repository repo) throws IOException {
    return setBranchHead(name, commitId, repo, RefUtils.ensureBranchRefName(name) + ": updating " + Constants.HEAD, true);
  }

  @Nonnull
  public static RefUpdate.Result commitBranchHead(@Nonnull String name, @Nonnull AnyObjectId commitId, @Nonnull Repository repo, @Nonnull String shortMessage) throws IOException {
    return setBranchHead(name, commitId, repo, "commit: " + shortMessage, true);
  }

  @Nonnull
  public static RefUpdate.Result commitBranchHead(@Nonnull String name, @Nonnull AnyObjectId commitId, @Nonnull Repository repo) throws IOException {
    return commitBranchHead(name, commitId, repo, CommitUtils.getCommit(repo, commitId).getShortMessage());
  }

  @Nonnull
  public static RefUpdate.Result amendBranchHead(@Nonnull String name, @Nonnull AnyObjectId commitId, @Nonnull Repository repo, @Nonnull String shortMessage) throws IOException {
    return setBranchHead(name, commitId, repo, "commit (amend): " + shortMessage, true);
  }

  @Nonnull
  public static RefUpdate.Result amendBranchHead(@Nonnull String name, @Nonnull AnyObjectId commitId, @Nonnull Repository repo) throws IOException {
    return amendBranchHead(name, commitId, repo, CommitUtils.getCommit(repo, commitId).getShortMessage());
  }

  @Nonnull
  public static RefUpdate.Result cherryPickBranchHead(@Nonnull String name, @Nonnull AnyObjectId commitId, @Nonnull Repository repo, @Nonnull String shortMessage) throws IOException {
    return setBranchHead(name, commitId, repo, "cherry-pick: " + shortMessage, true);
  }

  @Nonnull
  public static RefUpdate.Result cherryPickBranchHead(@Nonnull String name, @Nonnull AnyObjectId commitId, @Nonnull Repository repo) throws IOException {
    return cherryPickBranchHead(name, commitId, repo, CommitUtils.getCommit(repo, commitId).getShortMessage());
  }

  @Nonnull
  public static RefUpdate.Result initBranchHead(@Nonnull String name, @Nonnull AnyObjectId commitId, @Nonnull Repository repo, @Nonnull String shortMessage, boolean falseUpdate) throws IOException {
    return setBranchHead(name, commitId, repo, "commit (initial): " + shortMessage, falseUpdate);
  }

  @Nonnull
  public static RefUpdate.Result initBranchHead(@Nonnull String name, @Nonnull AnyObjectId commitId, @Nonnull Repository repo, @Nonnull String shortMessage) throws IOException {
    return initBranchHead(name, commitId, repo, shortMessage, false);
  }

  @Nonnull
  public static RefUpdate.Result initBranchHead(@Nonnull String name, @Nonnull AnyObjectId commitId, @Nonnull Repository repo) throws IOException {
    return initBranchHead(name, commitId, repo, CommitUtils.getCommit(repo, commitId).getShortMessage());
  }

  @Nonnull
  public static RefUpdate.Result deleteBranch(@Nonnull String name, @Nonnull Repository repo) throws IOException {
    String refName = RefUtils.ensureBranchRefName(name);
    RefUpdate update = repo.updateRef(refName);
    update.setRefLogMessage("branch deleted", false);
    update.setForceUpdate(true);
    return update.delete();
  }

  public static enum UpdateType {
    COMMIT,
    AMEND,
    INIT,
    CHERRYPICK,
    MERGE
  }

}