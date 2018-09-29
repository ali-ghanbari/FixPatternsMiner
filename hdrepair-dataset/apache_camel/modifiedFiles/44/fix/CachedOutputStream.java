/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.converter.stream;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.StreamCache;
import org.apache.camel.util.FileUtil;
import org.apache.camel.util.IOHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This output stream will store the content into a File if the stream context size is exceed the
 * THRESHOLD which's default value is 64K. The temp file will store in the temp directory, you 
 * can configure it by setting the TEMP_DIR property. If you don't set the TEMP_DIR property,
 * it will choose the directory which is set by the system property of "java.io.tmpdir".
 * You can get a cached input stream of this stream. The temp file which is created with this 
 * output stream will be deleted when you close this output stream or the cached inputStream.
 */
public class CachedOutputStream extends OutputStream {
    public static final String THRESHOLD = "CamelCachedOutputStreamThreshold";
    public static final String TEMP_DIR = "CamelCachedOutputStreamOutputDirectory";
    private static final transient Log LOG = LogFactory.getLog(CachedOutputStream.class);
    
    private OutputStream currentStream = new ByteArrayOutputStream(2048);
    private boolean inMemory = true;
    private int totalLength;
    private File tempFile;
    
    private List<FileInputStreamCache> fileInputStreamCaches = new ArrayList<FileInputStreamCache>(4);

    private long threshold = 64 * 1024;
    private File outputDir;

    public CachedOutputStream(Exchange exchange) {
        String hold = exchange.getContext().getProperties().get(THRESHOLD);
        String dir = exchange.getContext().getProperties().get(TEMP_DIR);
        if (hold != null) {
            this.threshold = exchange.getContext().getTypeConverter().convertTo(Long.class, hold);
        }
        if (dir != null) {
            this.outputDir = exchange.getContext().getTypeConverter().convertTo(File.class, dir);
        }
        
    }

    public void flush() throws IOException {
        currentStream.flush();       
    }

    public void close() throws IOException {
        currentStream.close();
        try {
            // cleanup temporary file
            if (tempFile != null) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    LOG.warn("Cannot delete temporary cache file: " + tempFile);
                } else if (LOG.isTraceEnabled()) {
                    LOG.trace("Deleted temporary cache file: " + tempFile);
                }
                tempFile = null;
            }
        } catch (Exception e) {
            LOG.warn("Error deleting temporary cache file: " + tempFile, e);
        }
    }

    public boolean equals(Object obj) {
        return currentStream.equals(obj);
    }

    public int hashCode() {
        return currentStream.hashCode();
    }

    public String toString() {
        return "CachedOutputStream[size: " + totalLength + "]";
    }

    public void write(byte[] b, int off, int len) throws IOException {
        this.totalLength += len;
        if (threshold > 0 && inMemory && totalLength > threshold && currentStream instanceof ByteArrayOutputStream) {
            pageToFileStream();
        }
        currentStream.write(b, off, len);
    }

    public void write(byte[] b) throws IOException {
        this.totalLength += b.length;
        if (threshold > 0 && inMemory && totalLength > threshold && currentStream instanceof ByteArrayOutputStream) {
            pageToFileStream();
        }
        currentStream.write(b);
    }

    public void write(int b) throws IOException {
        this.totalLength++;
        if (threshold > 0 && inMemory && totalLength > threshold && currentStream instanceof ByteArrayOutputStream) {
            pageToFileStream();
        }
        currentStream.write(b);
    }

    public InputStream getInputStream() throws IOException {
        flush();

        if (inMemory) {
            if (currentStream instanceof ByteArrayOutputStream) {
                return new ByteArrayInputStream(((ByteArrayOutputStream) currentStream).toByteArray());
            } else {
                throw new IllegalStateException("CurrentStream should be an instance of ByteArrayOutputStream but is: " + currentStream.getClass().getName());
            }
        } else {
            try {
                FileInputStreamCache answer = new FileInputStreamCache(tempFile, this);
                fileInputStreamCaches.add(answer);
                return answer;
            } catch (FileNotFoundException e) {
                throw IOHelper.createIOException("Cached file " + tempFile + " not found", e);
            }
        }
    }


    public StreamCache getStreamCache() throws IOException {
        flush();

        if (inMemory) {
            if (currentStream instanceof ByteArrayOutputStream) {
                return new InputStreamCache(((ByteArrayOutputStream) currentStream).toByteArray());
            } else {
                throw new IllegalStateException("CurrentStream should be an instance of ByteArrayOutputStream but is: " + currentStream.getClass().getName());
            }
        } else {
            try {
                FileInputStreamCache answer = new FileInputStreamCache(tempFile, this);
                fileInputStreamCaches.add(answer);
                return answer;
            } catch (FileNotFoundException e) {
                throw IOHelper.createIOException("Cached file " + tempFile + " not found", e);
            }
        }
    }

    private void pageToFileStream() throws IOException {
        flush();

        ByteArrayOutputStream bout = (ByteArrayOutputStream)currentStream;
        if (outputDir == null) {
            tempFile = FileUtil.createTempFile("cos", ".tmp");
        } else {
            tempFile = FileUtil.createTempFile("cos", ".tmp", outputDir);
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("Creating temporary stream cache file: " + tempFile);
        }

        try {
            currentStream = new BufferedOutputStream(new FileOutputStream(tempFile));
            bout.writeTo(currentStream);
        } finally {
            // ensure flag is flipped to file based
            inMemory = false;
        }
    }

}
