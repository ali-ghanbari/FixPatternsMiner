package org.mudebug.fpm.commons;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This class, in order to take two .java files as a buggy-fix pair, assumes
 * a directory of the following structure:
 * <p>
 * base/path/[common-path]/[key]/File.java
 * <p>
 * Thus, two files
 * <p>
 * <code>base/path/common/path/old/File.java</code>
 * <br>
 * and
 * <br>
 * <code>base/path/common/path/fix/File.java</code>
 * <p>
 * are considered as buggy-fixed pair, while the files
 * <p>
 * <code>base/path/common/path1/[...]/File.java</code>
 * <br>
 * and
 * <br>
 * <code>base/path/common/path2/[...]/File.java</code>
 * <p>
 * will not be taken as buggy-fix pair.
 * 
 * @author Ali
 *
 */
public final class Crawler {
    private final File basePath;
    
    public Crawler(final String basePath) {
        this(new File(basePath));
    }
    
    public Crawler(final File basePath) {
        assert (basePath.isDirectory() && basePath.exists());
        this.basePath = basePath;
    }

    public File getBasePath() {
        return basePath;
    }
    
    private static Function<File, File> classifier() {
        return new Function<File, File>() {
            /**
             * Returns the common part
             * @param javaFile
             * @return
             */
            @Override
            public File apply(final File javaFile) {
                return javaFile.getParentFile().getParentFile();
            }
        };
    }
    
    private static Predicate<File> fileFilter() {
        return f -> f.getName().endsWith(".java") && f.getParentFile() != null && f.getParentFile().getParentFile() != null;
    }
    
    public Collection<FilePair> ls() {
        try {
            return Files.walk(this.basePath.toPath()).filter(Files::isRegularFile).map(Path::toFile).filter(fileFilter())
                    .collect(Collectors.groupingBy(classifier())).entrySet().stream()
                    .map(Map.Entry::getValue)
                    .filter(jfl -> jfl.size() == 2)
                    .map(FilePair::new)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
