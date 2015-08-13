package com.beijunyi.parallelgit.filesystem.io;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.beijunyi.parallelgit.filesystem.*;
import com.beijunyi.parallelgit.filesystem.utils.FileAttributeReader;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.TreeFormatter;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

public final class GfsIO {

  @Nonnull
  private static GitPath getParent(@Nonnull GitPath child) {
    if(child.isRoot())
      throw new IllegalArgumentException(child.toString());
    GitPath parent = child.getParent();
    if(parent == null)
      throw new IllegalStateException(child.toString());
    return parent;
  }

  @Nonnull
  private static Node[] findNodes(@Nonnull GitPath path) throws IOException {
    if(!path.isAbsolute())
      throw new IllegalArgumentException(path.toString());
    GitFileStore store = path.getFileStore();
    int total = path.getNameCount() + 1;
    Node[] nodes = new Node[total];
    nodes[total - 1] = store.getRoot();
    for(int i = 0; i < path.getNameCount(); i++) {
      int index = total - i - 2;
      GitPath name = path.getName(i);
      Node parent = nodes[index + 1];
      if(parent instanceof DirectoryNode)
        nodes[index] = prepareDirectory((DirectoryNode) parent, path.getFileSystem()).getChild(name.toString());
      else
        break;
    }
    return nodes;
  }

  @Nonnull
  private static FileNode asFile(@Nullable Node node, @Nonnull GitPath path) throws NoSuchFileException, AccessDeniedException {
    if(node == null)
      throw new NoSuchFileException(path.toString());
    if(node instanceof FileNode)
      return (FileNode) node;
    throw new AccessDeniedException(path.toString());
  }

  @Nonnull
  private static FileNode firstAsFile(@Nonnull Node[] nodes, @Nonnull GitPath path) throws NoSuchFileException, AccessDeniedException {
    return asFile(nodes[0], path);
  }

  @Nonnull
  private static DirectoryNode firstAsDirectory(@Nonnull Node[] nodes, @Nonnull GitPath path) throws NotDirectoryException {
    if(nodes[0] instanceof DirectoryNode)
      return (DirectoryNode) nodes[0];
    throw new NotDirectoryException(path.toString());
  }

  @Nonnull
  private static Node findNode(@Nonnull GitPath path) throws IOException {
    Node node = findNodes(path)[0];
    if(node == null)
      throw new NoSuchFileException(path.toString());
    return node;
  }

  @Nonnull
  static FileNode findFile(@Nonnull GitPath dir) throws IOException {
    return firstAsFile(findNodes(dir), dir);
  }

  @Nonnull
  static DirectoryNode findDirectory(@Nonnull GitPath dir) throws IOException {
    return firstAsDirectory(findNodes(dir), dir);
  }

  private static void setParentsDirty(@Nonnull Node[] nodes) {
    for(Node node : nodes) {
      if(node == null)
        throw new IllegalStateException();
      node.setDirty(true);
    }
  }

  private static void checkNotRootPath(@Nonnull GitPath path) {
    if(path.isRoot())
      throw new IllegalArgumentException(path.toString());
  }

  @Nonnull
  private static String getFileName(@Nonnull GitPath path) throws IOException {
    checkNotRootPath(path);
    GitPath name = path.getFileName();
    if(name == null)
      throw new IllegalStateException(path.toString());
    return name.toString();
  }

  @Nonnull
  private static byte[] readBlobObject(@Nonnull AnyObjectId blobObjectId, @Nonnull GitFileSystem gfs) throws IOException {
    if(blobObjectId.equals(ObjectId.zeroId()))
      return new byte[0];
    return gfs.loadObject(blobObjectId);
  }

  @Nonnull
  private static byte[] loadFileData(@Nonnull FileNode file, @Nonnull GitFileSystem gfs) throws IOException {
    byte[] bytes = readBlobObject(file.getObject(), gfs);
    file.loadContent(bytes);
    return bytes;
  }

  @Nonnull
  public static byte[] getFileData(@Nonnull FileNode file, @Nonnull GitFileSystem gfs) throws IOException {
    byte[] bytes = file.getBytes();
    if(bytes == null)
      bytes = loadFileData(file, gfs);
    return bytes;
  }

  private static long readBlobSize(@Nonnull AnyObjectId blobObjectId, @Nonnull GitFileSystem gfs) throws IOException {
    if(blobObjectId.equals(ObjectId.zeroId()))
      return 0;
    return gfs.getBlobSize(blobObjectId);
  }

  private static long loadFileSize(@Nonnull FileNode file, @Nonnull GitFileSystem gfs) throws IOException {
    long size = readBlobSize(file.getObject(), gfs);
    file.setSize(size);
    return size;
  }

  @Nonnull
  private static Map<String, Node> readTreeObject(@Nonnull AnyObjectId treeObjectId, @Nonnull GitFileSystem gfs, @Nonnull DirectoryNode parent) throws IOException {
    Map<String, Node> children = new HashMap<>();
    if(!treeObjectId.equals(ObjectId.zeroId())) {
      byte[] treeData = gfs.loadObject(treeObjectId);
      CanonicalTreeParser treeParser = new CanonicalTreeParser();
      treeParser.reset(treeData);
      while(!treeParser.eof()) {
        children.put(treeParser.getEntryPathString(), Node.forObject(treeParser.getEntryObjectId(), treeParser.getEntryFileMode(), parent));
        treeParser.next();
      }
    }
    return children;
  }

  @Nonnull
  private static Map<String, Node> loadChildren(@Nonnull DirectoryNode dir, @Nonnull GitFileSystem gfs) throws IOException {
    Map<String, Node> children = readTreeObject(dir.getObject(), gfs, dir);
    dir.loadChildren(children);
    return children;
  }

  @Nonnull
  private static DirectoryNode prepareDirectory(@Nonnull DirectoryNode dir, @Nonnull GitFileSystem gfs) throws IOException {
    if(dir.getChildren() == null)
      loadChildren(dir, gfs);
    return dir;
  }

  @Nonnull
  public static Map<String, Node> getChildren(@Nonnull DirectoryNode dir, @Nonnull GitFileSystem gfs) throws IOException {
    Map<String, Node> children = prepareDirectory(dir, gfs).getChildren();
    if(children == null)
      throw new IllegalStateException();
    return children;
  }

  public static long getSize(@Nonnull Node node, @Nonnull GitFileSystem gfs) throws IOException {
    long size;
    if(node instanceof FileNode) {
      FileNode file = (FileNode) node;
      size = file.getSize();
      if(size == -1L)
        size = loadFileSize(file, gfs);
    } else
      size = 0;
    return size;
  }

  @Nonnull
  public static GfsSeekableByteChannel newByteChannel(@Nonnull GitPath file, @Nonnull Set<OpenOption> options, @Nonnull Collection<FileAttribute> attrs) throws IOException {
    FileNode node;
    if(options.contains(StandardOpenOption.CREATE) || options.contains(StandardOpenOption.CREATE_NEW)) {
      GitPath parent = getParent(file);
      Node[] parentNodes = findNodes(parent);
      DirectoryNode parentNode = prepareDirectory(firstAsDirectory(parentNodes, parent), file.getFileSystem());
      String name = getFileName(file);
      if(options.contains(StandardOpenOption.CREATE_NEW) || !parentNode.hasChild(name)) {
        node = FileNode.newFile(FileAttributeReader.read(attrs).isExecutable(), parentNode);
        if(!parentNode.addChild(name, node, false))
          throw new FileAlreadyExistsException(file.toString());
      } else {
        node = asFile(parentNode.getChild(name), file);
      }
    } else
      node = findFile(file);
    return new GfsSeekableByteChannel(node, file.getFileSystem(), options);
  }

  @Nonnull
  public static GfsDirectoryStream newDirectoryStream(@Nonnull GitPath dir, @Nullable DirectoryStream.Filter<? super Path> filter) throws IOException {
    DirectoryNode node = prepareDirectory(findDirectory(dir), dir.getFileSystem());
    return new GfsDirectoryStream(node, dir, filter);
  }

  public static void createDirectory(@Nonnull GitPath dir) throws IOException {
    if(dir.isRoot())
      throw new FileAlreadyExistsException(dir.toString());
    GitPath parent = getParent(dir);
    Node[] parentNodes = findNodes(parent);
    DirectoryNode parentNode = prepareDirectory(firstAsDirectory(parentNodes, parent), dir.getFileSystem());
    if(!parentNode.addChild(getFileName(dir), DirectoryNode.newDirectory(parentNode), false))
      throw new FileAlreadyExistsException(dir.toString());
    setParentsDirty(parentNodes);
  }

  private static void copyFile(@Nonnull FileNode source, @Nonnull GitFileSystem sourceFs, @Nonnull FileNode target) throws IOException {
    byte[] bytes = source.getBytes();
    if(bytes == null)
      bytes = sourceFs.loadObject(source.getObject());
    target.updateContent(bytes);
  }

  private static void copyDirectory(@Nonnull DirectoryNode source, @Nonnull GitFileSystem sourceFs, @Nonnull DirectoryNode target, @Nonnull GitFileSystem targetFs) throws IOException {
    Map<String, Node> children = source.getChildren();
    if(children == null)
      children = readTreeObject(source.getObject(), sourceFs, source);
    Map<String, Node> clonedChildren = new HashMap<>();
    for(Map.Entry<String, Node> child : children.entrySet()) {
      Node clonedChild = Node.ofSameType(child.getValue(), target);
      clonedChildren.put(child.getKey(), clonedChild);
      copyNode(child.getValue(), sourceFs, clonedChild, targetFs);
    }
    target.setChildren(clonedChildren);
  }

  private static void copyNode(@Nonnull Node source, @Nonnull GitFileSystem sourceFs, @Nonnull Node target, @Nonnull GitFileSystem targetFs) throws IOException {
    if(!source.isDirty() && targetFs.hasObject(source.getObject())) {
      target.setObject(source.getObject());
      return;
    }
    if(source instanceof FileNode && target instanceof FileNode)
      copyFile((FileNode) source, sourceFs, (FileNode) target);
    else if(source instanceof DirectoryNode && target instanceof DirectoryNode)
      copyDirectory((DirectoryNode) source, sourceFs, (DirectoryNode) target, targetFs);
    else
      throw new IllegalStateException();
  }

  public static boolean copy(@Nonnull GitPath source, @Nonnull GitPath target, @Nonnull Set<CopyOption> options) throws IOException {
    Node sourceNode = findNode(source);
    if(source.equals(target))
      return false;
    if(target.isRoot())
      throw new FileAlreadyExistsException(target.toString());
    GitPath targetParent = getParent(target);
    Node[] targetParentNodes = findNodes(targetParent);
    DirectoryNode targetDirectory = prepareDirectory(firstAsDirectory(targetParentNodes, targetParent), target.getFileSystem());
    Node targetNode = Node.ofSameType(sourceNode, targetDirectory);
    if(!targetDirectory.addChild(getFileName(target), targetNode, options.contains(StandardCopyOption.REPLACE_EXISTING)))
      throw new FileAlreadyExistsException(target.toString());
    copyNode(sourceNode, source.getFileSystem(), targetNode, target.getFileSystem());
    setParentsDirty(targetParentNodes);
    return true;
  }

  public static boolean move(@Nonnull GitPath source, @Nonnull GitPath target, @Nonnull Set<CopyOption> options) throws IOException {
    if(copy(source, target, options)) {
      delete(source);
      return true;
    }
    return false;
  }

  public static void delete(@Nonnull GitPath file) throws IOException {
    if(file.isRoot())
      throw new AccessDeniedException(file.toString());
    GitPath parent = getParent(file);
    Node[] parentNodes = findNodes(parent);
    DirectoryNode parentNode = prepareDirectory(firstAsDirectory(parentNodes, parent), file.getFileSystem());
    if(!parentNode.removeChild(getFileName(file)))
      throw new NoSuchFileException(file.toString());
  }

  public static void checkAccess(@Nonnull GitPath path, @Nonnull Set<AccessMode> modes) throws IOException {
    Node node = findNode(path);
    if(modes.contains(AccessMode.EXECUTE) && !node.isExecutableFile())
      throw new AccessDeniedException(path.toString());
  }

  @Nonnull
  public static <V extends FileAttributeView> V getFileAttributeView(@Nonnull GitPath path, @Nonnull Class<V> type) throws IOException, UnsupportedOperationException {
    return GitFileAttributeView.forNode(findNode(path), path.getFileSystem(), type);
  }

  @Nonnull
  private static AnyObjectId persistFile(@Nonnull FileNode file, @Nonnull GitFileSystem gfs) throws IOException {
    byte[] bytes = file.getBytes();
    if(bytes == null)
      throw new IllegalStateException();
    return gfs.saveBlob(bytes);
  }

  @Nonnull
  private static AnyObjectId persistDirectory(@Nonnull DirectoryNode dir, @Nonnull GitFileSystem gfs) throws IOException {
    Map<String, Node> children = dir.getChildren();
    if(children == null)
      throw new IllegalStateException();
    TreeFormatter formatter = new TreeFormatter();
    for(Map.Entry<String, Node> child : new TreeMap<>(children).entrySet()) {
      String name = child.getKey();
      Node node = child.getValue();
      formatter.append(name, node.getType().toFileMode(), persistNode(node, gfs));
    }
    return gfs.saveTree(formatter);
  }

  @Nonnull
  private static AnyObjectId persistNode(@Nonnull Node node, @Nonnull GitFileSystem gfs) throws IOException {
    if(!node.isDirty())
      return node.getObject();
    AnyObjectId objectId;
    if(node instanceof FileNode)
      objectId = persistFile((FileNode) node, gfs);
    else if(node instanceof DirectoryNode)
      objectId = persistDirectory((DirectoryNode) node, gfs);
    else
      throw new IllegalStateException();
    node.markClean(objectId);
    return objectId;
  }

  @Nonnull
  public static AnyObjectId persist(@Nonnull GitPath path) throws IOException {
    return persistNode(findNode(path), path.getFileSystem());
  }

}
