package edu.utdallas.fpm.commons;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.file.Files;
import java.util.stream.StreamSupport;

import static edu.utdallas.fpm.commons.Util.panic;

public final class FileListParser {
    private final Reader reader;

    public FileListParser() {
        this.reader = new InputStreamReader(System.in);
    }

    public FileListParser(final File filesListCsv) {
        this.reader = newBufferedReader(filesListCsv);
    }

    private static Reader newBufferedReader(final File file) {
        try {
            return Files.newBufferedReader(file.toPath());
        } catch (Exception e) {
            panic(e);
        }
        return null;
    }

    private String getBuggyFileName(final CSVRecord record) {
        return record.get(0).trim();
    }

    private String getFixedFileName(final CSVRecord record) {
        return record.get(1).trim();
    }

    public void parse(final FilePairVisitor visitor, final boolean parallelInvocation) {
        try (CSVParser parser = new CSVParser(this.reader, CSVFormat.DEFAULT)) {
            StreamSupport.stream(parser.spliterator(), parallelInvocation)
                    .forEach(record -> {
                        final File buggy = new File(getBuggyFileName(record));
                        final File fixed = new File(getFixedFileName(record));
                        visitor.visit(buggy, fixed);
                    });
        } catch (Exception e) {
            panic(e);
        }
    }
}
