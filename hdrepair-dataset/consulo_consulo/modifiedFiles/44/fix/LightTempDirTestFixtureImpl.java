package com.intellij.testFramework.fixtures.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author yole
 */
public class LightTempDirTestFixtureImpl extends BaseFixture implements TempDirTestFixture {
  public VirtualFile copyFile(VirtualFile file, String targetPath) {
    int pos = targetPath.lastIndexOf('/');
    String path = pos < 0 ? "" : targetPath.substring(0, pos);
    try {
      VirtualFile targetDir = findOrCreateTargetDir(path);
      return VfsUtil.copyFile(this, file, targetDir);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private VirtualFile findOrCreateTargetDir(String path) throws IOException {
    VirtualFile root = LightPlatformTestCase.getSourceRoot();
    if (path.length() == 0) return root;
    final String[] dirs = path.split("/");
    for (String dirName : dirs) {
      VirtualFile dir = root.findChild(dirName);
      if (dir != null) {
        root = dir;
      }
      else {
        root = root.createChildDirectory(this, dirName);
      }
    }
    return root;
  }

  public VirtualFile copyAll(final String dataDir, final String targetDir) {
    return ApplicationManager.getApplication().runWriteAction(new Computable<VirtualFile>() {
      public VirtualFile compute() {
        final VirtualFile from = LocalFileSystem.getInstance().refreshAndFindFileByPath(dataDir);
        assert from != null: "Cannot find testdata directory " + dataDir;
        try {
          VirtualFile tempDir = LightPlatformTestCase.getSourceRoot();
          if (targetDir.length() > 0) {
            assert !targetDir.contains("/") : "nested directories not implemented";
            VirtualFile child = tempDir.findChild(targetDir);
            if (child == null) {
              child = tempDir.createChildDirectory(this, targetDir);
            }
            tempDir = child;
          }

          VfsUtil.copyDirectory(this, from, tempDir, null);
          return tempDir;
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  public String getTempDirPath() {
    return "temp:///";
  }

  public VirtualFile getFile(@NonNls String path) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  public VirtualFile createFile(String name) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  public VirtualFile createFile(String name, String text) throws IOException {
    throw new UnsupportedOperationException();
  }
}
