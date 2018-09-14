package org.mudebug.fpm.commons;

import java.io.File;
import java.util.Collection;

public final class FilePair {
    private final File buggy;
    private final File fixed;
    
    public FilePair(final Collection<File> files) {
        assert (files != null && files.size() == 2);
        this.buggy = files.stream().filter(f -> f.getParentFile().getName().matches("(bug(.)*)|(old(.)*)")).findAny().get();
        this.fixed = files.stream().filter(f -> f.getParentFile().getName().matches("(fix(.)*)|(new(.)*)")).findAny().get();
    }
    
    public File getBuggy() {
        return buggy;
    }

    public File getFixed() {
        return fixed;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((buggy == null) ? 0 : buggy.hashCode());
        result = prime * result + ((fixed == null) ? 0 : fixed.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FilePair other = (FilePair) obj;
        if (buggy == null) {
            if (other.buggy != null) {
                return false;
            }
        } else if (!buggy.equals(other.buggy)) {
            return false;
        }
        if (fixed == null) {
            if (other.fixed != null) {
                return false;
            }
        } else if (!fixed.equals(other.fixed)) {
            return false;
        }            
        return true;
    }
    
    @Override
    public String toString() {
        return String.format("Buggy File: %s%nFixed File: %s",
                this.buggy.getAbsolutePath(),
                this.fixed.getAbsolutePath());
    }
}
