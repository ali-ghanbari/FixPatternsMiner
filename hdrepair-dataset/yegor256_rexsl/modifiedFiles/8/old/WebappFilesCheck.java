/**
 * Copyright (c) 2011-2012, ReXSL.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the ReXSL.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.rexsl.maven.checks;

import com.jcabi.log.Logger;
import com.rexsl.maven.Check;
import com.rexsl.maven.Environment;
import java.io.File;
import java.util.Collection;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.lang.ArrayUtils;

/**
 * Checks webapp directory contents.
 *
 * <p>Since this class is NOT public its documentation is not available online.
 * All details of this check should be explained in the JavaDoc of
 * {@link com.rexsl.maven.checks.DefaultChecksProvider}.
 *
 * <p>The class is immutable and thread-safe.
 *
 * @author Evgeniy Nyavro (e.nyavro@gmail.com)
 * @version $Id: WebappFilesCheck.java 1732 2012-05-24 20:36:50Z guard $
 */
final class WebappFilesCheck implements Check {

    /**
     * Allowed extensions for files in sub folders.
     * @checkstyle MultipleStringLiterals (15 lines)
     */
    @SuppressWarnings({
        "PMD.UseConcurrentHashMap",
        "PMD.AvoidDuplicateLiterals",
        "unchecked"
    })
    private static final Map<String, String> EXTS = ArrayUtils.toMap(
        new String[][]{
            {"webapp", ".*\\.html|robots.txt"},
            {"WEB-INF", ".*\\.xml"},
            {"css", ".*\\.css"},
            {"js", ".*\\.js"},
            {"xsl", ".*\\.xsl"},
        }
    );

    /**
     * {@inheritDoc}
     */
    @Override
    public void setScope(final String scope) {
        // nothing to scope here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validate(final Environment env) {
        final File dir = new File(env.basedir(), "src/main/webapp");
        boolean valid;
        if (dir.exists()) {
            valid = this.validate(dir);
        } else {
            valid = false;
            Logger.warn(this, "Directory '%s' is absent", dir);
        }
        return valid;
    }

    /**
     * Validate one dir.
     * @param dir The dir
     * @return TRUE if valid
     */
    private boolean validate(final File dir) {
        boolean valid = true;
        for (File file : this.getFiles(dir)) {
            final String folder = file.getParentFile().getName();
            if (!WebappFilesCheck.EXTS.containsKey(folder)) {
                Logger.warn(
                    this,
                    "Invalid folder in webapp hierarchy: %s",
                    folder
                );
                valid = false;
                break;
            }
            final String regex = WebappFilesCheck.EXTS.get(folder);
            final String name = file.getName();
            if (!name.matches(regex)) {
                Logger.warn(
                    this,
                    "File '%s' has incorrect extension (should match '%s')",
                    file,
                    regex
                );
                valid = false;
                break;
            }
        }
        return valid;
    }

    /**
     * Get files, recursively.
     * @param dir The directory to read from
     * @return Collection of files
     */
    private Collection<File> getFiles(final File dir) {
        return FileUtils.listFiles(
            dir,
            HiddenFileFilter.VISIBLE,
            new AndFileFilter(
                HiddenFileFilter.VISIBLE,
                new NotFileFilter(new NameFileFilter(".svn"))
            )
        );
    }

}
