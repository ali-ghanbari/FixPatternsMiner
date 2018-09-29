package org.mudebug.fpm.commons;

import java.io.File;

public interface FilePairVisitor {
    void visit(File buggy, File fixed);
}
