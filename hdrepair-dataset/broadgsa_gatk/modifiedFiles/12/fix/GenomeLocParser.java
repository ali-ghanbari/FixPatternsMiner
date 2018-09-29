/*
 * Copyright (c) 2010 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.broadinstitute.sting.utils;

import com.google.java.contract.Ensures;
import com.google.java.contract.Requires;
import com.google.java.contract.ThrowEnsures;
import net.sf.picard.reference.ReferenceSequenceFile;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;
import org.apache.log4j.Logger;
import org.broad.tribble.Feature;
import org.broadinstitute.sting.utils.codecs.vcf.VCFConstants;
import org.broadinstitute.sting.utils.exceptions.ReviewedStingException;
import org.broadinstitute.sting.utils.exceptions.UserException;
import org.broadinstitute.sting.utils.variantcontext.VariantContext;

/**
 * Factory class for creating GenomeLocs
 */
public final class GenomeLocParser {
    private static Logger logger = Logger.getLogger(GenomeLocParser.class);

    // --------------------------------------------------------------------------------------------------------------
    //
    // Ugly global variable defining the optional ordering of contig elements
    //
    // --------------------------------------------------------------------------------------------------------------

    /**
     * This single variable holds the underlying SamSequenceDictionary used by the GATK.  We assume
     * it is thread safe.
     */
    final private SAMSequenceDictionary SINGLE_MASTER_SEQUENCE_DICTIONARY;

    /**
     * A thread-local caching contig info
     */
    private final ThreadLocal<CachingSequenceDictionary> contigInfoPerThread =
            new ThreadLocal<CachingSequenceDictionary>();

    /**
     * @return a caching sequence dictionary appropriate for this thread
     */
    private CachingSequenceDictionary getContigInfo() {
        if ( contigInfoPerThread.get() == null ) {
            // initialize for this thread
            contigInfoPerThread.set(new CachingSequenceDictionary(SINGLE_MASTER_SEQUENCE_DICTIONARY));
        }

        assert contigInfoPerThread.get() != null;

        return contigInfoPerThread.get();
    }

    /**
     * A wrapper class that provides efficient last used caching for the global
     * SAMSequenceDictionary underlying all of the GATK engine capabilities.
     */
    private final class CachingSequenceDictionary {
        final private SAMSequenceDictionary dict;

        // cache
        SAMSequenceRecord lastSSR = null;
        String lastContig = "";
        int lastIndex = -1;

        @Requires({"dict != null", "dict.size() > 0"})
        public CachingSequenceDictionary(SAMSequenceDictionary dict) {
            this.dict = dict;
        }

        @Ensures("result > 0")
        public final int getNSequences() {
            return dict.size();
        }

        @Requires("contig != null")
        public final synchronized boolean hasContig(final String contig) {
            return contig.equals(lastContig) || dict.getSequence(contig) != null;
        }

        @Requires("index >= 0")
        public final synchronized boolean hasContig(final int index) {
            return lastIndex == index || dict.getSequence(index) != null;
        }

        @Requires("contig != null")
        @Ensures("result != null")
        public synchronized final SAMSequenceRecord getSequence(final String contig) {
            if ( isCached(contig) )
                return lastSSR;
            else
                return updateCache(contig, -1);
        }

        @Requires("index >= 0")
        @Ensures("result != null")
        public synchronized final SAMSequenceRecord getSequence(final int index) {
            if ( isCached(index) )
                return lastSSR;
            else
                return updateCache(null, index);
        }

        @Requires("contig != null")
        @Ensures("result >= 0")
        public synchronized final int getSequenceIndex(final String contig) {
            if ( ! isCached(contig) ) {
                updateCache(contig, -1);
            }

            return lastIndex;
        }

        @Requires({"contig != null", "lastContig != null"})
        private synchronized boolean isCached(final String contig) {
            return lastContig.equals(contig);
        }

        @Requires({"lastIndex != -1", "index >= 0"})
        private synchronized boolean isCached(final int index) {
            return lastIndex == index;
        }

        /**
         * The key algorithm.  Given a new record, update the last used record, contig
         * name, and index.
         *
         * @param contig
         * @param index
         * @return
         */
        @Requires("contig != null || index >= 0")
        @Ensures("result != null")
        private synchronized SAMSequenceRecord updateCache(final String contig, int index ) {
            SAMSequenceRecord rec = contig == null ? dict.getSequence(index) : dict.getSequence(contig);
            if ( rec == null ) {
                throw new ReviewedStingException("BUG: requested unknown contig=" + contig + " index=" + index);
            } else {
                lastSSR = rec;
                lastContig = rec.getSequenceName();
                lastIndex = rec.getSequenceIndex();
                return rec;
            }
        }


    }

    /**
     * set our internal reference contig order
     * @param refFile the reference file
     */
    @Requires("refFile != null")
    public GenomeLocParser(final ReferenceSequenceFile refFile) {
        this(refFile.getSequenceDictionary());
    }

    public GenomeLocParser(SAMSequenceDictionary seqDict) {
        if (seqDict == null) { // we couldn't load the reference dictionary
            //logger.info("Failed to load reference dictionary, falling back to lexicographic order for contigs");
            throw new UserException.CommandLineException("Failed to load reference dictionary");
        }

        SINGLE_MASTER_SEQUENCE_DICTIONARY = seqDict;
        logger.debug(String.format("Prepared reference sequence contig dictionary"));
        for (SAMSequenceRecord contig : seqDict.getSequences()) {
            logger.debug(String.format(" %s (%d bp)", contig.getSequenceName(), contig.getSequenceLength()));
        }
    }

    /**
     * Determines whether the given contig is valid with respect to the sequence dictionary
     * already installed in the GenomeLoc.
     *
     * @return True if the contig is valid.  False otherwise.
     */
    public final boolean contigIsInDictionary(String contig) {
        return contig != null && getContigInfo().hasContig(contig);
    }

    public final boolean indexIsInDictionary(final int index) {
        return index >= 0 && getContigInfo().hasContig(index);
    }


    /**
     * get the contig's SAMSequenceRecord
     *
     * @param contig the string name of the contig
     *
     * @return the sam sequence record
     */
    @Ensures("result != null")
    @ThrowEnsures({"UserException.MalformedGenomeLoc", "!contigIsInDictionary(contig) || contig == null"})
    public final SAMSequenceRecord getContigInfo(final String contig) {
        if ( contig == null || ! contigIsInDictionary(contig) )
            throw new UserException.MalformedGenomeLoc(String.format("Contig %s given as location, but this contig isn't present in the Fasta sequence dictionary", contig));
        return getContigInfo().getSequence(contig);
    }

    /**
     * Returns the contig index of a specified string version of the contig
     *
     * @param contig the contig string
     *
     * @return the contig index, -1 if not found
     */
    @Ensures("result >= 0")
    @ThrowEnsures({"UserException.MalformedGenomeLoc", "!contigIsInDictionary(contig) || contig == null"})
    public final int getContigIndex(final String contig) {
        return getContigInfo(contig).getSequenceIndex();
    }

    @Requires("contig != null")
    protected int getContigIndexWithoutException(final String contig) {
        if ( contig == null || ! getContigInfo().hasContig(contig) )
            return -1;
        return getContigInfo().getSequenceIndex(contig);
    }

    /**
     * Return the master sequence dictionary used within this GenomeLocParser
     * @return
     */
    public final SAMSequenceDictionary getContigs() {
        return getContigInfo().dict;
    }

    // --------------------------------------------------------------------------------------------------------------
    //
    // Low-level creation functions
    //
    // --------------------------------------------------------------------------------------------------------------
    /**
     * create a genome loc, given the contig name, start, and stop
     *
     * @param contig the contig name
     * @param start  the starting position
     * @param stop   the stop position
     *
     * @return a new genome loc
     */
    @Ensures("result != null")
    @ThrowEnsures({"UserException.MalformedGenomeLoc", "!isValidGenomeLoc(contig, start, stop)"})
    public GenomeLoc createGenomeLoc(String contig, final int start, final int stop) {
        return createGenomeLoc(contig, getContigIndex(contig), start, stop);
    }

    public GenomeLoc createGenomeLoc(String contig, final int start, final int stop, boolean mustBeOnReference) {
        return createGenomeLoc(contig, getContigIndex(contig), start, stop, mustBeOnReference);
    }

    @ThrowEnsures({"UserException.MalformedGenomeLoc", "!isValidGenomeLoc(contig, start, stop, false)"})
    public GenomeLoc createGenomeLoc(String contig, int index, final int start, final int stop) {
        return createGenomeLoc(contig, index, start, stop, false);
    }

    @ThrowEnsures({"UserException.MalformedGenomeLoc", "!isValidGenomeLoc(contig, start, stop,mustBeOnReference)"})
    public GenomeLoc createGenomeLoc(String contig, int index, final int start, final int stop, boolean mustBeOnReference) {
        validateGenomeLoc(contig, index, start, stop, mustBeOnReference, true);
        return new GenomeLoc(contig, index, start, stop);
    }

    /**
     * validate a position or interval on the genome as valid
     *
     * Requires that contig exist in the master sequence dictionary, and that contig index be valid as well.  Requires
     * that start <= stop.
     *
     * if mustBeOnReference is true,
     * performs boundary validation for genome loc INTERVALS:
     * start and stop are on contig and start <= stop
     *
     * @param contig the contig name
     * @param start  the start position
     * @param stop   the stop position
     *
     * @return true if it's valid, false otherwise.  If exceptOnError, then throws a UserException if invalid
     */
    private boolean validateGenomeLoc(String contig, int contigIndex, int start, int stop, boolean mustBeOnReference, boolean exceptOnError) {
        if ( ! getContigInfo().hasContig(contig) )
            return vglHelper(exceptOnError, String.format("Unknown contig %s", contig));

        if (stop < start)
            return vglHelper(exceptOnError, String.format("The stop position %d is less than start %d in contig %s", stop, start, contig));

        if (contigIndex < 0)
            return vglHelper(exceptOnError, String.format("The contig index %d is less than 0", contigIndex));

        if (contigIndex >= getContigInfo().getNSequences())
            return vglHelper(exceptOnError, String.format("The contig index %d is greater than the stored sequence count (%d)", contigIndex, getContigInfo().getNSequences()));

        if ( mustBeOnReference ) {
            if (start < 1)
                return vglHelper(exceptOnError, String.format("The start position %d is less than 1", start));

            if (stop < 1)
                return vglHelper(exceptOnError, String.format("The stop position %d is less than 1", stop));

            int contigSize = getContigInfo().getSequence(contigIndex).getSequenceLength();
            if (start > contigSize || stop > contigSize)
                return vglHelper(exceptOnError, String.format("The genome loc coordinates %d-%d exceed the contig size (%d)", start, stop, contigSize));
        }

        // we passed
        return true;
    }

    public boolean isValidGenomeLoc(String contig, int start, int stop, boolean mustBeOnReference ) {
        return validateGenomeLoc(contig, getContigIndexWithoutException(contig), start, stop, mustBeOnReference, false);
    }

    public boolean isValidGenomeLoc(String contig, int start, int stop ) {
        return validateGenomeLoc(contig, getContigIndexWithoutException(contig), start, stop, true, false);
    }

    private boolean vglHelper(boolean exceptOnError, String msg) {
        if ( exceptOnError )
            throw new UserException.MalformedGenomeLoc("Parameters to GenomeLocParser are incorrect:" + msg);
        else
            return false;
    }

    // --------------------------------------------------------------------------------------------------------------
    //
    // Parsing genome locs
    //
    // --------------------------------------------------------------------------------------------------------------

    /**
     * parse a genome interval, from a location string
     *
     * Performs interval-style validation:
     *
     * contig is valid; start and stop less than the end; start <= stop, and start/stop are on the contig
     * @param str the string to parse
     *
     * @return a GenomeLoc representing the String
     *
     */
    @Requires("str != null")
    @Ensures("result != null")
    public GenomeLoc parseGenomeLoc(final String str) {
        // 'chr2', 'chr2:1000000' or 'chr2:1,000,000-2,000,000'
        //System.out.printf("Parsing location '%s'%n", str);

        String contig = null;
        int start = 1;
        int stop = -1;

        final int colonIndex = str.lastIndexOf(":");
        if(colonIndex == -1) {
            contig = str.substring(0, str.length());  // chr1
            stop = Integer.MAX_VALUE;
        } else {
            contig = str.substring(0, colonIndex);
            final int dashIndex = str.indexOf('-', colonIndex);
            try {
                if(dashIndex == -1) {
                    if(str.charAt(str.length() - 1) == '+') {
                        start = parsePosition(str.substring(colonIndex + 1, str.length() - 1));  // chr:1+
                        stop = Integer.MAX_VALUE;
                    } else {
                        start = parsePosition(str.substring(colonIndex + 1));   // chr1:1
                        stop = start;
                    }
                } else {
                    start = parsePosition(str.substring(colonIndex + 1, dashIndex));  // chr1:1-1
                    stop = parsePosition(str.substring(dashIndex + 1));
                }
            } catch(Exception e) {
                throw new UserException("Failed to parse Genome Location string: " + str, e);
            }
        }

        // is the contig valid?
        if (!contigIsInDictionary(contig))
            throw new UserException.MalformedGenomeLoc("Contig '" + contig + "' does not match any contig in the GATK sequence dictionary derived from the reference; are you sure you are using the correct reference fasta file?");

        if (stop == Integer.MAX_VALUE)
            // lookup the actually stop position!
            stop = getContigInfo(contig).getSequenceLength();

        return createGenomeLoc(contig, getContigIndex(contig), start, stop, true);
    }

    /**
     * Parses a number like 1,000,000 into a long.
     * @param pos
     */
    @Requires("pos != null")
    @Ensures("result >= 0")
    private int parsePosition(final String pos) {
        if(pos.indexOf('-') != -1) {
            throw new NumberFormatException("Position: '" + pos + "' can't contain '-'." );
        }

        if(pos.indexOf(',') != -1) {
            final StringBuilder buffer = new StringBuilder();
            for(int i = 0; i < pos.length(); i++) {
                final char c = pos.charAt(i);

                if(c == ',') {
                    continue;
                } else if(c < '0' || c > '9') {
                    throw new NumberFormatException("Position: '" + pos + "' contains invalid chars." );
                } else {
                    buffer.append(c);
                }
            }
            return Integer.parseInt(buffer.toString());
        } else {
            return Integer.parseInt(pos);
        }
    }

    // --------------------------------------------------------------------------------------------------------------
    //
    // Parsing string representations
    //
    // --------------------------------------------------------------------------------------------------------------

    /**
     * create a genome loc, given a read. If the read is unmapped, *and* yet the read has a contig and start position,
     * then a GenomeLoc is returned for contig:start-start, otherwise and UNMAPPED GenomeLoc is returned.
     *
     * @param read
     *
     * @return
     */
    @Requires("read != null")
    @Ensures("result != null")
    public GenomeLoc createGenomeLoc(final SAMRecord read) {
        if ( read.getReadUnmappedFlag() && read.getReferenceIndex() == -1 )
            // read is unmapped and not placed anywhere on the genome
            return GenomeLoc.UNMAPPED;
        else {
            // Use Math.max to ensure that end >= start (Picard assigns the end to reads that are entirely within an insertion as start-1)
            int end = read.getReadUnmappedFlag() ? read.getAlignmentStart() : Math.max(read.getAlignmentEnd(), read.getAlignmentStart());
            return createGenomeLoc(read.getReferenceName(), read.getReferenceIndex(), read.getAlignmentStart(), end, false);
        }
    }

    /**
     * Creates a GenomeLoc from a Tribble feature
     * @param feature
     * @return
     */
    public GenomeLoc createGenomeLoc(final Feature feature) {
        return createGenomeLoc(feature.getChr(), feature.getStart(), feature.getEnd());
    }

    /**
     * Creates a GenomeLoc corresponding to the variant context vc.  If includeSymbolicEndIfPossible
     * is true, and VC is a symbolic allele the end of the created genome loc will be the value
     * of the END info field key, if it exists, or vc.getEnd() if not.
     *
     * @param vc
     * @param includeSymbolicEndIfPossible
     * @return
     */
    public GenomeLoc createGenomeLoc(final VariantContext vc, boolean includeSymbolicEndIfPossible) {
        if ( includeSymbolicEndIfPossible && vc.isSymbolic() ) {
            int end = vc.getAttributeAsInt(VCFConstants.END_KEY, vc.getEnd());
            return createGenomeLoc(vc.getChr(), vc.getStart(), end);
        }
        else
            return createGenomeLoc(vc.getChr(), vc.getStart(), vc.getEnd());
    }

    public GenomeLoc createGenomeLoc(final VariantContext vc) {
        return createGenomeLoc(vc, false);
    }

    /**
     * create a new genome loc, given the contig name, and a single position. Must be on the reference
     *
     * @param contig the contig name
     * @param pos    the postion
     *
     * @return a genome loc representing a single base at the specified postion on the contig
     */
    @Ensures("result != null")
    @ThrowEnsures({"UserException.MalformedGenomeLoc", "!isValidGenomeLoc(contig, pos, pos, true)"})
    public GenomeLoc createGenomeLoc(final String contig, final int pos) {
        return createGenomeLoc(contig, getContigIndex(contig), pos, pos);
    }

    /**
     * create a new genome loc from an existing loc, with a new start position
     * Note that this function will NOT explicitly check the ending offset, in case someone wants to
     * set the start of a new GenomeLoc pertaining to a read that goes off the end of the contig.
     *
     * @param loc   the old location
     * @param start a new start position
     *
     * @return the newly created genome loc
     */
    public GenomeLoc setStart(GenomeLoc loc, int start) {
        return createGenomeLoc(loc.getContig(), loc.getContigIndex(), start, loc.getStop());
    }

    /**
     * create a new genome loc from an existing loc, with a new stop position
     * Note that this function will NOT explicitly check the ending offset, in case someone wants to
     * set the stop of a new GenomeLoc pertaining to a read that goes off the end of the contig.
     *
     * @param loc  the old location
     * @param stop a new stop position
     *
     * @return
     */
    public GenomeLoc setStop(GenomeLoc loc, int stop) {
        return createGenomeLoc(loc.getContig(), loc.getContigIndex(), loc.start, stop);
    }

    /**
     * return a new genome loc, with an incremented position
     *
     * @param loc the old location
     *
     * @return a new genome loc
     */
    public GenomeLoc incPos(GenomeLoc loc) {
        return incPos(loc, 1);
    }

    /**
     * return a new genome loc, with an incremented position
     *
     * @param loc the old location
     * @param by  how much to move the start and stop by
     *
     * @return a new genome loc
     */
    public GenomeLoc incPos(GenomeLoc loc, int by) {
        return createGenomeLoc(loc.getContig(), loc.getContigIndex(), loc.start + by, loc.stop + by);
    }

    /**
     * Creates a GenomeLoc than spans the entire contig.
     * @param contigName Name of the contig.
     * @return A locus spanning the entire contig.
     */
    @Requires("contigName != null")
    @Ensures("result != null")
    public GenomeLoc createOverEntireContig(String contigName) {
        SAMSequenceRecord contig = getContigInfo().getSequence(contigName);
        return createGenomeLoc(contigName,contig.getSequenceIndex(),1,contig.getSequenceLength(), true);
    }

    /**
     * Creates a loc to the left (starting at the loc start + 1) of maxBasePairs size.
     * @param loc The original loc
     * @param maxBasePairs The maximum number of basePairs
     * @return The contiguous loc of up to maxBasePairs length or null if the loc is already at the start of the contig.
     */
    @Requires({"loc != null", "maxBasePairs > 0"})
    public GenomeLoc createGenomeLocAtStart(GenomeLoc loc, int maxBasePairs) {
        if (GenomeLoc.isUnmapped(loc))
            return null;
        String contigName = loc.getContig();
        SAMSequenceRecord contig = getContigInfo().getSequence(contigName);
        int contigIndex = contig.getSequenceIndex();

        int start = loc.getStart() - maxBasePairs;
        int stop = loc.getStart() - 1;

        if (start < 1)
            start = 1;
        if (stop < 1)
            return null;

        return createGenomeLoc(contigName, contigIndex, start, stop, true);
    }

    /**
     * Creates a loc padded in both directions by maxBasePairs size (if possible).
     * @param loc      The original loc
     * @param padding  The number of base pairs to pad on either end
     * @return The contiguous loc of length up to the original length + 2*padding (depending on the start/end of the contig).
     */
    @Requires({"loc != null", "padding > 0"})
    public GenomeLoc createPaddedGenomeLoc(final GenomeLoc loc, final int padding) {
        if (GenomeLoc.isUnmapped(loc))
            return loc;
        final String contigName = loc.getContig();
        final SAMSequenceRecord contig = getContigInfo().getSequence(contigName);
        final int contigIndex = contig.getSequenceIndex();
        final int contigLength = contig.getSequenceLength();

        final int start = Math.max(1, loc.getStart() - padding);
        final int stop = Math.min(contigLength, loc.getStop() + padding);

        return createGenomeLoc(contigName, contigIndex, start, stop, true);
    }

    /**
     * Creates a loc to the right (starting at the loc stop + 1) of maxBasePairs size.
     * @param loc The original loc
     * @param maxBasePairs The maximum number of basePairs
     * @return The contiguous loc of up to maxBasePairs length or null if the loc is already at the end of the contig.
     */
    @Requires({"loc != null", "maxBasePairs > 0"})
    public GenomeLoc createGenomeLocAtStop(GenomeLoc loc, int maxBasePairs) {
        if (GenomeLoc.isUnmapped(loc))
            return null;
        String contigName = loc.getContig();
        SAMSequenceRecord contig = getContigInfo().getSequence(contigName);
        int contigIndex = contig.getSequenceIndex();
        int contigLength = contig.getSequenceLength();

        int start = loc.getStop() + 1;
        int stop = loc.getStop() + maxBasePairs;

        if (start > contigLength)
            return null;
        if (stop > contigLength)
            stop = contigLength;

        return createGenomeLoc(contigName, contigIndex, start, stop, true);
    }
}
