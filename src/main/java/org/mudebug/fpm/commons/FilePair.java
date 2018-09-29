package org.mudebug.fpm.commons;

import java.io.File;
import java.util.Objects;

public final class FilePair {
    private final File buggy;
    private final File fixed;
    
   public FilePair(File buggy, File fixed) {
       this.buggy = buggy;
       this.fixed = fixed;
   }
    
    public File getBuggy() {
        return buggy;
    }

    public File getFixed() {
        return fixed;
    }
    
    @Override
    public String toString() {
        return String.format("Buggy File: %s%nFixed File: %s",
                this.buggy.getAbsolutePath(),
                this.fixed.getAbsolutePath());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FilePair filePair = (FilePair) o;
        return Objects.equals(buggy, filePair.buggy) &&
                Objects.equals(fixed, filePair.fixed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(buggy, fixed);
    }
}
