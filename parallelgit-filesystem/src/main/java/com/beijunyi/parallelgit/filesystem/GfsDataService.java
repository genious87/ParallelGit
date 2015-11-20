package com.beijunyi.parallelgit.filesystem;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.ClosedFileSystemException;
import javax.annotation.Nonnull;

import org.eclipse.jgit.lib.*;

public class GfsDataService implements Closeable {

  private final Repository repo;
  private final ObjectReader reader;
  private final ObjectInserter inserter;

  private volatile boolean closed = false;

  GfsDataService(@Nonnull final Repository repo) {
    this.repo = repo;
    this.reader = repo.newObjectReader();
    this.inserter = repo.newObjectInserter();
  }

  @Nonnull
  public Repository getRepository() {
    return repo;
  }

  public boolean hasObject(@Nonnull AnyObjectId objectId) throws IOException {
    checkClosed();
    synchronized(reader) {
      return reader.has(objectId);
    }
  }

  @Nonnull
  public byte[] loadObject(@Nonnull AnyObjectId objectId) throws IOException {
    checkClosed();
    synchronized(reader) {
      return reader.open(objectId).getBytes();
    }
  }

  public long getBlobSize(@Nonnull AnyObjectId objectId) throws IOException {
    checkClosed();
    synchronized(reader) {
      return reader.getObjectSize(objectId, Constants.OBJ_BLOB);
    }
  }


  @Nonnull
  public AnyObjectId saveBlob(@Nonnull byte[] bytes) throws IOException {
    checkClosed();
    synchronized(inserter) {
      return inserter.insert(Constants.OBJ_BLOB, bytes);
    }
  }

  @Nonnull
  public AnyObjectId saveTree(@Nonnull TreeFormatter tf) throws IOException {
    checkClosed();
    synchronized(inserter) {
      return inserter.insert(tf);
    }
  }

  public void flush() throws IOException {
    checkClosed();
    synchronized(inserter) {
      inserter.flush();
    }
  }

  @Override
  public synchronized void close() {
    if(!closed) {
      closed = true;
      reader.close();
      inserter.close();
      repo.close();
    }
  }

  private void checkClosed() {
    if(closed)
      throw new ClosedFileSystemException();
  }


}