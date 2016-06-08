package com.beijunyi.parallelgit.utils.io;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.treewalk.TreeWalk;

import static org.eclipse.jgit.lib.FileMode.*;
import static org.eclipse.jgit.lib.ObjectId.zeroId;

public class GitFileEntry {

  private static final GitFileEntry MISSING_ENTRY = new GitFileEntry(zeroId(), MISSING);
  private static final GitFileEntry NEW_DIRECTORY_ENTRY = new GitFileEntry(zeroId(), TREE);

  private final ObjectId id;
  private final FileMode mode;

  private GitFileEntry(ObjectId id, FileMode mode) {
    this.id = id;
    this.mode = mode;
  }

  @Nonnull
  public static GitFileEntry newEntry(ObjectId id, FileMode mode) {
    return new GitFileEntry(id, mode);
  }

  @Nonnull
  public static GitFileEntry newEntry(TreeWalk tw, int index) {
    return newEntry(tw.getObjectId(index), tw.getFileMode(index));
  }

  @Nonnull
  public static GitFileEntry newEntry(TreeWalk tw) {
    return newEntry(tw, 0);
  }

  @Nonnull
  public static GitFileEntry missingEntry() {
    return MISSING_ENTRY;
  }

  @Nonnull
  public static GitFileEntry newTreeEntry(ObjectId id) {
    return newEntry(id, TREE);
  }

  @Nonnull
  public ObjectId getId() {
    return id;
  }

  @Nonnull
  public FileMode getMode() {
    return mode;
  }

  public boolean isDirectory() {
    return mode.equals(TREE);
  }

  public boolean isNewDirectory() {
    return NEW_DIRECTORY_ENTRY.equals(this);
  }

  public boolean isMissing() {
    return MISSING_ENTRY.equals(this);
  }


  @Override
  public boolean equals(@Nullable Object obj) {
    if(this == obj) return true;
    if(obj == null || getClass() != obj.getClass()) return false;
    GitFileEntry that = (GitFileEntry)obj;
    return id.equals(that.id) && mode.equals(that.mode);

  }

  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + mode.hashCode();
    return result;
  }
}
