/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.parser.pkg;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.extractor.ParsingEmbeddedDocumentExtractor;
import org.apache.tika.io.CloseShieldInputStream;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Extractor for packaging and compression formats.
 */
class PackageExtractor {

    private final ContentHandler handler;

    private final Metadata metadata;

    private final EmbeddedDocumentExtractor extractor;

    public PackageExtractor(
            ContentHandler handler, Metadata metadata, ParseContext context) {
        this.handler = handler;
        this.metadata = metadata;

        EmbeddedDocumentExtractor ex = context.get(EmbeddedDocumentExtractor.class);

        if (ex==null) {
            this.extractor = new ParsingEmbeddedDocumentExtractor(context);
        } else {
            this.extractor = ex;
        }

    }

    public void parse(InputStream stream)
            throws IOException, SAXException, TikaException {
        XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, metadata);
        xhtml.startDocument();

        // At the end we want to close the package/compression stream to
        // release any associated resources, but the underlying document
        // stream should not be closed
        stream = new CloseShieldInputStream(stream);

        // Capture two bytes to determine the packaging/compression format
        if (!stream.markSupported()) {
            stream = new BufferedInputStream(stream);
        }
        stream.mark(2);
        int a = stream.read();
        int b = stream.read();
        stream.reset();

        // Select decompression or unpacking mechanism based on the two bytes
        if (a == 'B' && b == 'Z') {
            metadata.set(Metadata.CONTENT_TYPE, "application/x-bzip");
            decompress(new BZip2CompressorInputStream(stream), xhtml);
        } else if (a == 0x1f && b == 0x8b) {
            metadata.set(Metadata.CONTENT_TYPE, "application/x-gzip");
            decompress(new GZIPInputStream(stream), xhtml);
        } else if (a == 'P' && b == 'K') {
            metadata.set(Metadata.CONTENT_TYPE, "application/zip");
            unpack(new ZipArchiveInputStream(stream), xhtml);
        } else if ((a == '0' && b == '7')
                || (a == 0x71 && b == 0xc7)
                || (a == 0xc7 && b == 0x71)) {
            metadata.set(Metadata.CONTENT_TYPE, "application/x-cpio");
            unpack(new CpioArchiveInputStream(stream), xhtml);
        } else if (a == '=' && (b == '<' || b == '!')) {
            metadata.set(Metadata.CONTENT_TYPE, "application/x-archive");
            unpack(new ArArchiveInputStream(stream), xhtml);
        } else {
            metadata.set(Metadata.CONTENT_TYPE, "application/x-tar");
            unpack(new TarArchiveInputStream(stream), xhtml);
        }

        xhtml.endDocument();
    }

    private void decompress(InputStream stream, XHTMLContentHandler xhtml)
            throws IOException, SAXException, TikaException {
        try {
            Metadata entrydata = new Metadata();
            String name = metadata.get(Metadata.RESOURCE_NAME_KEY);
            if (name != null) {
                if (name.endsWith(".tbz")) {
                    name = name.substring(0, name.length() - 4) + ".tar";
                } else if (name.endsWith(".tbz2")) {
                    name = name.substring(0, name.length() - 5) + ".tar";
                } else if (name.endsWith(".bz")) {
                    name = name.substring(0, name.length() - 3);
                } else if (name.endsWith(".bz2")) {
                    name = name.substring(0, name.length() - 4);
                } else if (name.length() > 0) {
                    name = GzipUtils.getUncompressedFilename(name);
                }
                entrydata.set(Metadata.RESOURCE_NAME_KEY, name);
            }

            // Use the delegate parser to parse the compressed document
            if (extractor.shouldParseEmbedded(entrydata)) {
                extractor.parseEmbedded(stream, xhtml, entrydata, true);
            }
        } finally {
            stream.close();
        }
    }

    /**
     * Parses the given stream as a package of multiple underlying files.
     * The package entries are parsed using the delegate parser instance.
     * It is not an error if the entry can not be parsed, in that case
     * just the entry name (if given) is emitted.
     *
     * @param archive package stream
     * @param xhtml content handler
     * @throws IOException if an IO error occurs
     * @throws SAXException if a SAX error occurs
     * @throws TikaException if another error occurs
     */
    public void unpack(ArchiveInputStream archive, XHTMLContentHandler xhtml)
            throws IOException, SAXException, TikaException {
        try {
            ArchiveEntry entry = archive.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    String name = entry.getName();

                    if (archive.canReadEntryData(entry)) {
                        Metadata entrydata = new Metadata();
                        if (name != null && name.length() > 0) {
                            entrydata.set(Metadata.RESOURCE_NAME_KEY, name);
                        }
                        if (extractor.shouldParseEmbedded(entrydata)) {
                            // For detectors to work, we need a mark/reset supporting
                            //  InputStream, which ArchiveInputStream isn't, so wrap
                            TemporaryResources tmp = new TemporaryResources();
                            try {
                                TikaInputStream stream = TikaInputStream.get(archive, tmp);
                                extractor.parseEmbedded(stream, xhtml, entrydata, true);
                            } finally {
                                tmp.dispose();
                            }
                        }
                    } else if (name != null && name.length() > 0) {
                        xhtml.element("p", name);
                    }
                }
                entry = archive.getNextEntry();
            }
        } finally {
            archive.close();
        }
    }

}
