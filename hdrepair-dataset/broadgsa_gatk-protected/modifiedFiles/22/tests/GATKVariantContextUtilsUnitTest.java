/*
* Copyright (c) 2012 The Broad Institute
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

package org.broadinstitute.sting.utils.variant;

import org.broadinstitute.sting.BaseTest;
import org.broadinstitute.sting.gatk.GenomeAnalysisEngine;
import org.broadinstitute.sting.utils.BaseUtils;
import org.broadinstitute.sting.utils.Utils;
import org.broadinstitute.sting.utils.collections.Pair;
import org.broadinstitute.variant.variantcontext.*;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

public class GATKVariantContextUtilsUnitTest extends BaseTest {

    Allele Aref, T, C, G, Cref, ATC, ATCATC;

    @BeforeSuite
    public void setup() {
        // alleles
        Aref = Allele.create("A", true);
        Cref = Allele.create("C", true);
        T = Allele.create("T");
        C = Allele.create("C");
        G = Allele.create("G");
        ATC = Allele.create("ATC");
        ATCATC = Allele.create("ATCATC");
    }

    private Genotype makeG(String sample, Allele a1, Allele a2) {
        return GenotypeBuilder.create(sample, Arrays.asList(a1, a2));
    }

    private Genotype makeG(String sample, Allele a1, Allele a2, double log10pError, double... pls) {
        return new GenotypeBuilder(sample, Arrays.asList(a1, a2)).log10PError(log10pError).PL(pls).make();
    }


    private Genotype makeG(String sample, Allele a1, Allele a2, double log10pError) {
        return new GenotypeBuilder(sample, Arrays.asList(a1, a2)).log10PError(log10pError).make();
    }

    private VariantContext makeVC(String source, List<Allele> alleles) {
        return makeVC(source, alleles, null, null);
    }

    private VariantContext makeVC(String source, List<Allele> alleles, Genotype... g1) {
        return makeVC(source, alleles, Arrays.asList(g1));
    }

    private VariantContext makeVC(String source, List<Allele> alleles, String filter) {
        return makeVC(source, alleles, filter.equals(".") ? null : new HashSet<String>(Arrays.asList(filter)));
    }

    private VariantContext makeVC(String source, List<Allele> alleles, Set<String> filters) {
        return makeVC(source, alleles, null, filters);
    }

    private VariantContext makeVC(String source, List<Allele> alleles, Collection<Genotype> genotypes) {
        return makeVC(source, alleles, genotypes, null);
    }

    private VariantContext makeVC(String source, List<Allele> alleles, Collection<Genotype> genotypes, Set<String> filters) {
        int start = 10;
        int stop = start; // alleles.contains(ATC) ? start + 3 : start;
        return new VariantContextBuilder(source, "1", start, stop, alleles).genotypes(genotypes).filters(filters).make();
    }

    // --------------------------------------------------------------------------------
    //
    // Test allele merging
    //
    // --------------------------------------------------------------------------------

    private class MergeAllelesTest extends TestDataProvider {
        List<List<Allele>> inputs;
        List<Allele> expected;

        private MergeAllelesTest(List<Allele>... arg) {
            super(MergeAllelesTest.class);
            LinkedList<List<Allele>> all = new LinkedList<List<Allele>>(Arrays.asList(arg));
            expected = all.pollLast();
            inputs = all;
        }

        public String toString() {
            return String.format("MergeAllelesTest input=%s expected=%s", inputs, expected);
        }
    }
    @DataProvider(name = "mergeAlleles")
    public Object[][] mergeAllelesData() {
        // first, do no harm
        new MergeAllelesTest(Arrays.asList(Aref),
                Arrays.asList(Aref));

        new MergeAllelesTest(Arrays.asList(Aref),
                Arrays.asList(Aref),
                Arrays.asList(Aref));

        new MergeAllelesTest(Arrays.asList(Aref),
                Arrays.asList(Aref, T),
                Arrays.asList(Aref, T));

        new MergeAllelesTest(Arrays.asList(Aref, C),
                Arrays.asList(Aref, T),
                Arrays.asList(Aref, C, T));

        new MergeAllelesTest(Arrays.asList(Aref, T),
                Arrays.asList(Aref, C),
                Arrays.asList(Aref, T, C)); // in order of appearence

        new MergeAllelesTest(Arrays.asList(Aref, C, T),
                Arrays.asList(Aref, C),
                Arrays.asList(Aref, C, T));

        new MergeAllelesTest(Arrays.asList(Aref, C, T), Arrays.asList(Aref, C, T));
        new MergeAllelesTest(Arrays.asList(Aref, T, C), Arrays.asList(Aref, T, C));

        new MergeAllelesTest(Arrays.asList(Aref, T, C),
                Arrays.asList(Aref, C),
                Arrays.asList(Aref, T, C)); // in order of appearence

        new MergeAllelesTest(Arrays.asList(Aref),
                Arrays.asList(Aref, ATC),
                Arrays.asList(Aref, ATC));

        new MergeAllelesTest(Arrays.asList(Aref),
                Arrays.asList(Aref, ATC, ATCATC),
                Arrays.asList(Aref, ATC, ATCATC));

        // alleles in the order we see them
        new MergeAllelesTest(Arrays.asList(Aref, ATCATC),
                Arrays.asList(Aref, ATC, ATCATC),
                Arrays.asList(Aref, ATCATC, ATC));

        // same
        new MergeAllelesTest(Arrays.asList(Aref, ATC),
                Arrays.asList(Aref, ATCATC),
                Arrays.asList(Aref, ATC, ATCATC));

        return MergeAllelesTest.getTests(MergeAllelesTest.class);
    }

    @Test(dataProvider = "mergeAlleles")
    public void testMergeAlleles(MergeAllelesTest cfg) {
        final List<VariantContext> inputs = new ArrayList<VariantContext>();

        int i = 0;
        for ( final List<Allele> alleles : cfg.inputs ) {
            final String name = "vcf" + ++i;
            inputs.add(makeVC(name, alleles));
        }

        final List<String> priority = vcs2priority(inputs);

        final VariantContext merged = GATKVariantContextUtils.simpleMerge(
                inputs, priority,
                GATKVariantContextUtils.FilteredRecordMergeType.KEEP_IF_ANY_UNFILTERED,
                GATKVariantContextUtils.GenotypeMergeType.PRIORITIZE, false, false, "set", false, false);

        Assert.assertEquals(merged.getAlleles(), cfg.expected);
    }

    // --------------------------------------------------------------------------------
    //
    // Test rsID merging
    //
    // --------------------------------------------------------------------------------

    private class SimpleMergeRSIDTest extends TestDataProvider {
        List<String> inputs;
        String expected;

        private SimpleMergeRSIDTest(String... arg) {
            super(SimpleMergeRSIDTest.class);
            LinkedList<String> allStrings = new LinkedList<String>(Arrays.asList(arg));
            expected = allStrings.pollLast();
            inputs = allStrings;
        }

        public String toString() {
            return String.format("SimpleMergeRSIDTest vc=%s expected=%s", inputs, expected);
        }
    }

    @DataProvider(name = "simplemergersiddata")
    public Object[][] createSimpleMergeRSIDData() {
        new SimpleMergeRSIDTest(".", ".");
        new SimpleMergeRSIDTest(".", ".", ".");
        new SimpleMergeRSIDTest("rs1", "rs1");
        new SimpleMergeRSIDTest("rs1", "rs1", "rs1");
        new SimpleMergeRSIDTest(".", "rs1", "rs1");
        new SimpleMergeRSIDTest("rs1", ".", "rs1");
        new SimpleMergeRSIDTest("rs1", "rs2", "rs1,rs2");
        new SimpleMergeRSIDTest("rs1", "rs2", "rs1", "rs1,rs2"); // duplicates
        new SimpleMergeRSIDTest("rs2", "rs1", "rs2,rs1");
        new SimpleMergeRSIDTest("rs2", "rs1", ".", "rs2,rs1");
        new SimpleMergeRSIDTest("rs2", ".", "rs1", "rs2,rs1");
        new SimpleMergeRSIDTest("rs1", ".", ".", "rs1");
        new SimpleMergeRSIDTest("rs1", "rs2", "rs3", "rs1,rs2,rs3");

        return SimpleMergeRSIDTest.getTests(SimpleMergeRSIDTest.class);
    }

    @Test(dataProvider = "simplemergersiddata")
    public void testRSIDMerge(SimpleMergeRSIDTest cfg) {
        VariantContext snpVC1 = makeVC("snpvc1", Arrays.asList(Aref, T));
        final List<VariantContext> inputs = new ArrayList<VariantContext>();

        for ( final String id : cfg.inputs ) {
            inputs.add(new VariantContextBuilder(snpVC1).id(id).make());
        }

        final VariantContext merged = GATKVariantContextUtils.simpleMerge(
                inputs, null,
                GATKVariantContextUtils.FilteredRecordMergeType.KEEP_IF_ANY_UNFILTERED,
                GATKVariantContextUtils.GenotypeMergeType.UNSORTED, false, false, "set", false, false);
        Assert.assertEquals(merged.getID(), cfg.expected);
    }

    // --------------------------------------------------------------------------------
    //
    // Test filtered merging
    //
    // --------------------------------------------------------------------------------

    private class MergeFilteredTest extends TestDataProvider {
        List<VariantContext> inputs;
        VariantContext expected;
        String setExpected;
        GATKVariantContextUtils.FilteredRecordMergeType type;


        private MergeFilteredTest(String name, VariantContext input1, VariantContext input2, VariantContext expected, String setExpected) {
            this(name, input1, input2, expected, GATKVariantContextUtils.FilteredRecordMergeType.KEEP_IF_ANY_UNFILTERED, setExpected);
        }

        private MergeFilteredTest(String name, VariantContext input1, VariantContext input2, VariantContext expected, GATKVariantContextUtils.FilteredRecordMergeType type, String setExpected) {
            super(MergeFilteredTest.class, name);
            LinkedList<VariantContext> all = new LinkedList<VariantContext>(Arrays.asList(input1, input2));
            this.expected = expected;
            this.type = type;
            inputs = all;
            this.setExpected = setExpected;
        }

        public String toString() {
            return String.format("%s input=%s expected=%s", super.toString(), inputs, expected);
        }
    }

    @DataProvider(name = "mergeFiltered")
    public Object[][] mergeFilteredData() {
        new MergeFilteredTest("AllPass",
                makeVC("1", Arrays.asList(Aref, T), VariantContext.PASSES_FILTERS),
                makeVC("2", Arrays.asList(Aref, T), VariantContext.PASSES_FILTERS),
                makeVC("3", Arrays.asList(Aref, T), VariantContext.PASSES_FILTERS),
                GATKVariantContextUtils.MERGE_INTERSECTION);

        new MergeFilteredTest("noFilters",
                makeVC("1", Arrays.asList(Aref, T), "."),
                makeVC("2", Arrays.asList(Aref, T), "."),
                makeVC("3", Arrays.asList(Aref, T), "."),
                GATKVariantContextUtils.MERGE_INTERSECTION);

        new MergeFilteredTest("oneFiltered",
                makeVC("1", Arrays.asList(Aref, T), "."),
                makeVC("2", Arrays.asList(Aref, T), "FAIL"),
                makeVC("3", Arrays.asList(Aref, T), "."),
                String.format("1-%s2", GATKVariantContextUtils.MERGE_FILTER_PREFIX));

        new MergeFilteredTest("onePassOneFail",
                makeVC("1", Arrays.asList(Aref, T), VariantContext.PASSES_FILTERS),
                makeVC("2", Arrays.asList(Aref, T), "FAIL"),
                makeVC("3", Arrays.asList(Aref, T), VariantContext.PASSES_FILTERS),
                String.format("1-%s2", GATKVariantContextUtils.MERGE_FILTER_PREFIX));

        new MergeFilteredTest("AllFiltered",
                makeVC("1", Arrays.asList(Aref, T), "FAIL"),
                makeVC("2", Arrays.asList(Aref, T), "FAIL"),
                makeVC("3", Arrays.asList(Aref, T), "FAIL"),
                GATKVariantContextUtils.MERGE_FILTER_IN_ALL);

        // test ALL vs. ANY
        new MergeFilteredTest("FailOneUnfiltered",
                makeVC("1", Arrays.asList(Aref, T), "FAIL"),
                makeVC("2", Arrays.asList(Aref, T), "."),
                makeVC("3", Arrays.asList(Aref, T), "."),
                GATKVariantContextUtils.FilteredRecordMergeType.KEEP_IF_ANY_UNFILTERED,
                String.format("%s1-2", GATKVariantContextUtils.MERGE_FILTER_PREFIX));

        new MergeFilteredTest("OneFailAllUnfilteredArg",
                makeVC("1", Arrays.asList(Aref, T), "FAIL"),
                makeVC("2", Arrays.asList(Aref, T), "."),
                makeVC("3", Arrays.asList(Aref, T), "FAIL"),
                GATKVariantContextUtils.FilteredRecordMergeType.KEEP_IF_ALL_UNFILTERED,
                String.format("%s1-2", GATKVariantContextUtils.MERGE_FILTER_PREFIX));

        // test excluding allele in filtered record
        new MergeFilteredTest("DontIncludeAlleleOfFilteredRecords",
                makeVC("1", Arrays.asList(Aref, T), "."),
                makeVC("2", Arrays.asList(Aref, T), "FAIL"),
                makeVC("3", Arrays.asList(Aref, T), "."),
                String.format("1-%s2", GATKVariantContextUtils.MERGE_FILTER_PREFIX));

        // promotion of site from unfiltered to PASSES
        new MergeFilteredTest("UnfilteredPlusPassIsPass",
                makeVC("1", Arrays.asList(Aref, T), "."),
                makeVC("2", Arrays.asList(Aref, T), VariantContext.PASSES_FILTERS),
                makeVC("3", Arrays.asList(Aref, T), VariantContext.PASSES_FILTERS),
                GATKVariantContextUtils.MERGE_INTERSECTION);

        new MergeFilteredTest("RefInAll",
                makeVC("1", Arrays.asList(Aref), VariantContext.PASSES_FILTERS),
                makeVC("2", Arrays.asList(Aref), VariantContext.PASSES_FILTERS),
                makeVC("3", Arrays.asList(Aref), VariantContext.PASSES_FILTERS),
                GATKVariantContextUtils.MERGE_REF_IN_ALL);

        new MergeFilteredTest("RefInOne",
                makeVC("1", Arrays.asList(Aref), VariantContext.PASSES_FILTERS),
                makeVC("2", Arrays.asList(Aref, T), VariantContext.PASSES_FILTERS),
                makeVC("3", Arrays.asList(Aref, T), VariantContext.PASSES_FILTERS),
                "2");

        return MergeFilteredTest.getTests(MergeFilteredTest.class);
    }

    @Test(dataProvider = "mergeFiltered")
    public void testMergeFiltered(MergeFilteredTest cfg) {
        final List<String> priority = vcs2priority(cfg.inputs);
        final VariantContext merged = GATKVariantContextUtils.simpleMerge(
                cfg.inputs, priority, cfg.type, GATKVariantContextUtils.GenotypeMergeType.PRIORITIZE, true, false, "set", false, false);

        // test alleles are equal
        Assert.assertEquals(merged.getAlleles(), cfg.expected.getAlleles());

        // test set field
        Assert.assertEquals(merged.getAttribute("set"), cfg.setExpected);

        // test filter field
        Assert.assertEquals(merged.getFilters(), cfg.expected.getFilters());
    }

    // --------------------------------------------------------------------------------
    //
    // Test genotype merging
    //
    // --------------------------------------------------------------------------------

    private class MergeGenotypesTest extends TestDataProvider {
        List<VariantContext> inputs;
        VariantContext expected;
        List<String> priority;

        private MergeGenotypesTest(String name, String priority, VariantContext... arg) {
            super(MergeGenotypesTest.class, name);
            LinkedList<VariantContext> all = new LinkedList<VariantContext>(Arrays.asList(arg));
            this.expected = all.pollLast();
            inputs = all;
            this.priority = Arrays.asList(priority.split(","));
        }

        public String toString() {
            return String.format("%s input=%s expected=%s", super.toString(), inputs, expected);
        }
    }

    @DataProvider(name = "mergeGenotypes")
    public Object[][] mergeGenotypesData() {
        new MergeGenotypesTest("TakeGenotypeByPriority-1,2", "1,2",
                makeVC("1", Arrays.asList(Aref, T), makeG("s1", Aref, T, -1)),
                makeVC("2", Arrays.asList(Aref, T), makeG("s1", Aref, T, -2)),
                makeVC("3", Arrays.asList(Aref, T), makeG("s1", Aref, T, -1)));

        new MergeGenotypesTest("TakeGenotypeByPriority-1,2-nocall", "1,2",
                makeVC("1", Arrays.asList(Aref, T), makeG("s1", Allele.NO_CALL, Allele.NO_CALL, -1)),
                makeVC("2", Arrays.asList(Aref, T), makeG("s1", Aref, T, -2)),
                makeVC("3", Arrays.asList(Aref, T), makeG("s1", Allele.NO_CALL, Allele.NO_CALL, -1)));

        new MergeGenotypesTest("TakeGenotypeByPriority-2,1", "2,1",
                makeVC("1", Arrays.asList(Aref, T), makeG("s1", Aref, T, -1)),
                makeVC("2", Arrays.asList(Aref, T), makeG("s1", Aref, T, -2)),
                makeVC("3", Arrays.asList(Aref, T), makeG("s1", Aref, T, -2)));

        new MergeGenotypesTest("NonOverlappingGenotypes", "1,2",
                makeVC("1", Arrays.asList(Aref, T), makeG("s1", Aref, T, -1)),
                makeVC("2", Arrays.asList(Aref, T), makeG("s2", Aref, T, -2)),
                makeVC("3", Arrays.asList(Aref, T), makeG("s1", Aref, T, -1), makeG("s2", Aref, T, -2)));

        new MergeGenotypesTest("PreserveNoCall", "1,2",
                makeVC("1", Arrays.asList(Aref, T), makeG("s1", Allele.NO_CALL, Allele.NO_CALL, -1)),
                makeVC("2", Arrays.asList(Aref, T), makeG("s2", Aref, T, -2)),
                makeVC("3", Arrays.asList(Aref, T), makeG("s1", Allele.NO_CALL, Allele.NO_CALL, -1), makeG("s2", Aref, T, -2)));

        new MergeGenotypesTest("PerserveAlleles", "1,2",
                makeVC("1", Arrays.asList(Aref, T), makeG("s1", Aref, T, -1)),
                makeVC("2", Arrays.asList(Aref, C), makeG("s2", Aref, C, -2)),
                makeVC("3", Arrays.asList(Aref, T, C), makeG("s1", Aref, T, -1), makeG("s2", Aref, C, -2)));

        new MergeGenotypesTest("TakeGenotypePartialOverlap-1,2", "1,2",
                makeVC("1", Arrays.asList(Aref, T), makeG("s1", Aref, T, -1)),
                makeVC("2", Arrays.asList(Aref, T), makeG("s1", Aref, T, -2), makeG("s3", Aref, T, -3)),
                makeVC("3", Arrays.asList(Aref, T), makeG("s1", Aref, T, -1), makeG("s3", Aref, T, -3)));

        new MergeGenotypesTest("TakeGenotypePartialOverlap-2,1", "2,1",
                makeVC("1", Arrays.asList(Aref, T), makeG("s1", Aref, T, -1)),
                makeVC("2", Arrays.asList(Aref, T), makeG("s1", Aref, T, -2), makeG("s3", Aref, T, -3)),
                makeVC("3", Arrays.asList(Aref, T), makeG("s1", Aref, T, -2), makeG("s3", Aref, T, -3)));

        //
        // merging genothpes with PLs
        //

        // first, do no harm
        new MergeGenotypesTest("OrderedPLs", "1",
                makeVC("1", Arrays.asList(Aref, T), makeG("s1", Aref, T, -1, 1, 2, 3)),
                makeVC("1", Arrays.asList(Aref, T), makeG("s1", Aref, T, -1, 1, 2, 3)));

        // first, do no harm
        new MergeGenotypesTest("OrderedPLs-3Alleles", "1",
                makeVC("1", Arrays.asList(Aref, C, T), makeG("s1", Aref, T, -1, 1, 2, 3, 4, 5, 6)),
                makeVC("1", Arrays.asList(Aref, C, T), makeG("s1", Aref, T, -1, 1, 2, 3, 4, 5, 6)));

        // first, do no harm
        new MergeGenotypesTest("OrderedPLs-3Alleles-2", "1",
                makeVC("1", Arrays.asList(Aref, T, C), makeG("s1", Aref, T, -1, 1, 2, 3, 4, 5, 6)),
                makeVC("1", Arrays.asList(Aref, T, C), makeG("s1", Aref, T, -1, 1, 2, 3, 4, 5, 6)));

        // first, do no harm
        new MergeGenotypesTest("OrderedPLs-3Alleles-2", "1",
                makeVC("1", Arrays.asList(Aref, T, C), makeG("s1", Aref, T, -1, 1, 2, 3, 4, 5, 6)),
                makeVC("1", Arrays.asList(Aref, T, C), makeG("s2", Aref, C, -1, 1, 2, 3, 4, 5, 6)),
                makeVC("1", Arrays.asList(Aref, T, C), makeG("s1", Aref, T, -1, 1, 2, 3, 4, 5, 6), makeG("s2", Aref, C, -1, 1, 2, 3, 4, 5, 6)));

        new MergeGenotypesTest("TakeGenotypePartialOverlapWithPLs-2,1", "2,1",
                makeVC("1", Arrays.asList(Aref, T), makeG("s1", Aref, T, -1,5,0,3)),
                makeVC("2", Arrays.asList(Aref, T), makeG("s1", Aref, T, -2,4,0,2), makeG("s3", Aref, T, -3,3,0,2)),
                makeVC("3", Arrays.asList(Aref, T), makeG("s1", Aref, T, -2,4,0,2), makeG("s3", Aref, T, -3,3,0,2)));

        new MergeGenotypesTest("TakeGenotypePartialOverlapWithPLs-1,2", "1,2",
                makeVC("1", Arrays.asList(Aref,ATC), makeG("s1", Aref, ATC, -1,5,0,3)),
                makeVC("2", Arrays.asList(Aref, T), makeG("s1", Aref, T, -2,4,0,2), makeG("s3", Aref, T, -3,3,0,2)),
                // no likelihoods on result since type changes to mixed multiallelic
                makeVC("3", Arrays.asList(Aref, ATC, T), makeG("s1", Aref, ATC, -1), makeG("s3", Aref, T, -3)));

        new MergeGenotypesTest("MultipleSamplePLsDifferentOrder", "1,2",
                makeVC("1", Arrays.asList(Aref, C, T), makeG("s1", Aref, C, -1, 1, 2, 3, 4, 5, 6)),
                makeVC("2", Arrays.asList(Aref, T, C), makeG("s2", Aref, T, -2, 6, 5, 4, 3, 2, 1)),
                // no likelihoods on result since type changes to mixed multiallelic
                makeVC("3", Arrays.asList(Aref, C, T), makeG("s1", Aref, C, -1), makeG("s2", Aref, T, -2)));

        return MergeGenotypesTest.getTests(MergeGenotypesTest.class);
    }

    @Test(dataProvider = "mergeGenotypes")
    public void testMergeGenotypes(MergeGenotypesTest cfg) {
        final VariantContext merged = GATKVariantContextUtils.simpleMerge(
                cfg.inputs, cfg.priority, GATKVariantContextUtils.FilteredRecordMergeType.KEEP_IF_ANY_UNFILTERED,
                GATKVariantContextUtils.GenotypeMergeType.PRIORITIZE, true, false, "set", false, false);

        // test alleles are equal
        Assert.assertEquals(merged.getAlleles(), cfg.expected.getAlleles());

        // test genotypes
        assertGenotypesAreMostlyEqual(merged.getGenotypes(), cfg.expected.getGenotypes());
    }

    // necessary to not overload equals for genotypes
    private void assertGenotypesAreMostlyEqual(GenotypesContext actual, GenotypesContext expected) {
        if (actual == expected) {
            return;
        }

        if (actual == null || expected == null) {
            Assert.fail("Maps not equal: expected: " + expected + " and actual: " + actual);
        }

        if (actual.size() != expected.size()) {
            Assert.fail("Maps do not have the same size:" + actual.size() + " != " + expected.size());
        }

        for (Genotype value : actual) {
            Genotype expectedValue = expected.get(value.getSampleName());

            Assert.assertEquals(value.getAlleles(), expectedValue.getAlleles(), "Alleles in Genotype aren't equal");
            Assert.assertEquals(value.getGQ(), expectedValue.getGQ(), "GQ values aren't equal");
            Assert.assertEquals(value.hasLikelihoods(), expectedValue.hasLikelihoods(), "Either both have likelihoods or both not");
            if ( value.hasLikelihoods() )
                Assert.assertEquals(value.getLikelihoods().getAsVector(), expectedValue.getLikelihoods().getAsVector(), "Genotype likelihoods aren't equal");
        }
    }

    @Test
    public void testMergeGenotypesUniquify() {
        final VariantContext vc1 = makeVC("1", Arrays.asList(Aref, T), makeG("s1", Aref, T, -1));
        final VariantContext vc2 = makeVC("2", Arrays.asList(Aref, T), makeG("s1", Aref, T, -2));

        final VariantContext merged = GATKVariantContextUtils.simpleMerge(
                Arrays.asList(vc1, vc2), null, GATKVariantContextUtils.FilteredRecordMergeType.KEEP_IF_ANY_UNFILTERED,
                GATKVariantContextUtils.GenotypeMergeType.UNIQUIFY, false, false, "set", false, false);

        // test genotypes
        Assert.assertEquals(merged.getSampleNames(), new HashSet<String>(Arrays.asList("s1.1", "s1.2")));
    }

// TODO: remove after testing
//    @Test(expectedExceptions = IllegalStateException.class)
//    public void testMergeGenotypesRequireUnique() {
//        final VariantContext vc1 = makeVC("1", Arrays.asList(Aref, T), makeG("s1", Aref, T, -1));
//        final VariantContext vc2 = makeVC("2", Arrays.asList(Aref, T), makeG("s1", Aref, T, -2));
//
//        final VariantContext merged = VariantContextUtils.simpleMerge(
//                Arrays.asList(vc1, vc2), null, VariantContextUtils.FilteredRecordMergeType.KEEP_IF_ANY_UNFILTERED,
//                VariantContextUtils.GenotypeMergeType.REQUIRE_UNIQUE, false, false, "set", false, false);
//    }

    // --------------------------------------------------------------------------------
    //
    // Misc. tests
    //
    // --------------------------------------------------------------------------------

    @Test
    public void testAnnotationSet() {
        for ( final boolean annotate : Arrays.asList(true, false)) {
            for ( final String set : Arrays.asList("set", "combine", "x")) {
                final List<String> priority = Arrays.asList("1", "2");
                VariantContext vc1 = makeVC("1", Arrays.asList(Aref, T), VariantContext.PASSES_FILTERS);
                VariantContext vc2 = makeVC("2", Arrays.asList(Aref, T), VariantContext.PASSES_FILTERS);

                final VariantContext merged = GATKVariantContextUtils.simpleMerge(
                        Arrays.asList(vc1, vc2), priority, GATKVariantContextUtils.FilteredRecordMergeType.KEEP_IF_ANY_UNFILTERED,
                        GATKVariantContextUtils.GenotypeMergeType.PRIORITIZE, annotate, false, set, false, false);

                if ( annotate )
                    Assert.assertEquals(merged.getAttribute(set), GATKVariantContextUtils.MERGE_INTERSECTION);
                else
                    Assert.assertFalse(merged.hasAttribute(set));
            }
        }
    }

    private static final List<String> vcs2priority(final Collection<VariantContext> vcs) {
        final List<String> priority = new ArrayList<String>();

        for ( final VariantContext vc : vcs ) {
            priority.add(vc.getSource());
        }

        return priority;
    }

    // --------------------------------------------------------------------------------
    //
    // basic allele clipping test
    //
    // --------------------------------------------------------------------------------

    private class ReverseClippingPositionTestProvider extends TestDataProvider {
        final String ref;
        final List<Allele> alleles = new ArrayList<Allele>();
        final int expectedClip;

        private ReverseClippingPositionTestProvider(final int expectedClip, final String ref, final String... alleles) {
            super(ReverseClippingPositionTestProvider.class);
            this.ref = ref;
            for ( final String allele : alleles )
                this.alleles.add(Allele.create(allele));
            this.expectedClip = expectedClip;
        }

        @Override
        public String toString() {
            return String.format("ref=%s allele=%s reverse clip %d", ref, alleles, expectedClip);
        }
    }

    @DataProvider(name = "ReverseClippingPositionTestProvider")
    public Object[][] makeReverseClippingPositionTestProvider() {
        // pair clipping
        new ReverseClippingPositionTestProvider(0, "ATT", "CCG");
        new ReverseClippingPositionTestProvider(1, "ATT", "CCT");
        new ReverseClippingPositionTestProvider(2, "ATT", "CTT");
        new ReverseClippingPositionTestProvider(2, "ATT", "ATT");  // cannot completely clip allele

        // triplets
        new ReverseClippingPositionTestProvider(0, "ATT", "CTT", "CGG");
        new ReverseClippingPositionTestProvider(1, "ATT", "CTT", "CGT"); // the T can go
        new ReverseClippingPositionTestProvider(2, "ATT", "CTT", "CTT"); // both Ts can go

        return ReverseClippingPositionTestProvider.getTests(ReverseClippingPositionTestProvider.class);
    }

    @Test(dataProvider = "ReverseClippingPositionTestProvider")
    public void testReverseClippingPositionTestProvider(ReverseClippingPositionTestProvider cfg) {
        int result = GATKVariantContextUtils.computeReverseClipping(cfg.alleles, cfg.ref.getBytes());
        Assert.assertEquals(result, cfg.expectedClip);
    }


    // --------------------------------------------------------------------------------
    //
    // test splitting into bi-allelics
    //
    // --------------------------------------------------------------------------------

    @DataProvider(name = "SplitBiallelics")
    public Object[][] makeSplitBiallelics() throws CloneNotSupportedException {
        List<Object[]> tests = new ArrayList<Object[]>();

        final VariantContextBuilder root = new VariantContextBuilder("x", "20", 10, 10, Arrays.asList(Aref, C));

        // biallelic -> biallelic
        tests.add(new Object[]{root.make(), Arrays.asList(root.make())});

        // monos -> monos
        root.alleles(Arrays.asList(Aref));
        tests.add(new Object[]{root.make(), Arrays.asList(root.make())});

        root.alleles(Arrays.asList(Aref, C, T));
        tests.add(new Object[]{root.make(),
                Arrays.asList(
                        root.alleles(Arrays.asList(Aref, C)).make(),
                        root.alleles(Arrays.asList(Aref, T)).make())});

        root.alleles(Arrays.asList(Aref, C, T, G));
        tests.add(new Object[]{root.make(),
                Arrays.asList(
                        root.alleles(Arrays.asList(Aref, C)).make(),
                        root.alleles(Arrays.asList(Aref, T)).make(),
                        root.alleles(Arrays.asList(Aref, G)).make())});

        final Allele C      = Allele.create("C");
        final Allele CA      = Allele.create("CA");
        final Allele CAA     = Allele.create("CAA");
        final Allele CAAAA   = Allele.create("CAAAA");
        final Allele CAAAAA  = Allele.create("CAAAAA");
        final Allele Cref      = Allele.create("C", true);
        final Allele CAref     = Allele.create("CA", true);
        final Allele CAAref    = Allele.create("CAA", true);
        final Allele CAAAref   = Allele.create("CAAA", true);

        root.alleles(Arrays.asList(Cref, CA, CAA));
        tests.add(new Object[]{root.make(),
                Arrays.asList(
                        root.alleles(Arrays.asList(Cref, CA)).make(),
                        root.alleles(Arrays.asList(Cref, CAA)).make())});

        root.alleles(Arrays.asList(CAAref, C, CA)).stop(12);
        tests.add(new Object[]{root.make(),
                Arrays.asList(
                        root.alleles(Arrays.asList(CAAref, C)).make(),
                        root.alleles(Arrays.asList(CAref, C)).stop(11).make())});

        root.alleles(Arrays.asList(CAAAref, C, CA, CAA)).stop(13);
        tests.add(new Object[]{root.make(),
                Arrays.asList(
                        root.alleles(Arrays.asList(CAAAref, C)).make(),
                        root.alleles(Arrays.asList(CAAref, C)).stop(12).make(),
                        root.alleles(Arrays.asList(CAref, C)).stop(11).make())});

        root.alleles(Arrays.asList(CAAAref, CAAAAA, CAAAA, CAA, C)).stop(13);
        tests.add(new Object[]{root.make(),
                Arrays.asList(
                        root.alleles(Arrays.asList(Cref, CAA)).stop(10).make(),
                        root.alleles(Arrays.asList(Cref, CA)).stop(10).make(),
                        root.alleles(Arrays.asList(CAref, C)).stop(11).make(),
                        root.alleles(Arrays.asList(CAAAref, C)).stop(13).make())});

        final Allele threeCopies = Allele.create("GTTTTATTTTATTTTA", true);
        final Allele twoCopies = Allele.create("GTTTTATTTTA", true);
        final Allele zeroCopies = Allele.create("G", false);
        final Allele oneCopies = Allele.create("GTTTTA", false);
        tests.add(new Object[]{root.alleles(Arrays.asList(threeCopies, zeroCopies, oneCopies)).stop(25).make(),
                Arrays.asList(
                        root.alleles(Arrays.asList(threeCopies, zeroCopies)).stop(25).make(),
                        root.alleles(Arrays.asList(twoCopies, zeroCopies)).stop(20).make())});

        return tests.toArray(new Object[][]{});
    }

    @Test(dataProvider = "SplitBiallelics")
    public void testSplitBiallelicsNoGenotypes(final VariantContext vc, final List<VariantContext> expectedBiallelics) {
        final List<VariantContext> biallelics = GATKVariantContextUtils.splitVariantContextToBiallelics(vc);
        Assert.assertEquals(biallelics.size(), expectedBiallelics.size());
        for ( int i = 0; i < biallelics.size(); i++ ) {
            final VariantContext actual = biallelics.get(i);
            final VariantContext expected = expectedBiallelics.get(i);
            assertVariantContextsAreEqual(actual, expected);
        }
    }

    @Test(dataProvider = "SplitBiallelics", dependsOnMethods = "testSplitBiallelicsNoGenotypes")
    public void testSplitBiallelicsGenotypes(final VariantContext vc, final List<VariantContext> expectedBiallelics) {
        final List<Genotype> genotypes = new ArrayList<Genotype>();

        int sampleI = 0;
        for ( final List<Allele> alleles : Utils.makePermutations(vc.getAlleles(), 2, true) ) {
            genotypes.add(GenotypeBuilder.create("sample" + sampleI++, alleles));
        }
        genotypes.add(GenotypeBuilder.createMissing("missing", 2));

        final VariantContext vcWithGenotypes = new VariantContextBuilder(vc).genotypes(genotypes).make();

        final List<VariantContext> biallelics = GATKVariantContextUtils.splitVariantContextToBiallelics(vcWithGenotypes);
        for ( int i = 0; i < biallelics.size(); i++ ) {
            final VariantContext actual = biallelics.get(i);
            Assert.assertEquals(actual.getNSamples(), vcWithGenotypes.getNSamples()); // not dropping any samples

            for ( final Genotype inputGenotype : genotypes ) {
                final Genotype actualGenotype = actual.getGenotype(inputGenotype.getSampleName());
                Assert.assertNotNull(actualGenotype);
                if ( ! vc.isVariant() || vc.isBiallelic() )
                    Assert.assertEquals(actualGenotype, vcWithGenotypes.getGenotype(inputGenotype.getSampleName()));
                else
                    Assert.assertTrue(actualGenotype.isNoCall());
            }
        }
    }


    // --------------------------------------------------------------------------------
    //
    // Test repeats
    //
    // --------------------------------------------------------------------------------

    private class RepeatDetectorTest extends TestDataProvider {
        String ref;
        boolean isTrueRepeat;
        VariantContext vc;

        private RepeatDetectorTest(boolean isTrueRepeat, String ref, String refAlleleString, String ... altAlleleStrings) {
            super(RepeatDetectorTest.class);
            this.isTrueRepeat = isTrueRepeat;
            this.ref = ref;

            List<Allele> alleles = new LinkedList<Allele>();
            final Allele refAllele = Allele.create(refAlleleString, true);
            alleles.add(refAllele);
            for ( final String altString: altAlleleStrings) {
                final Allele alt = Allele.create(altString, false);
                alleles.add(alt);
            }

            VariantContextBuilder builder = new VariantContextBuilder("test", "chr1", 1, refAllele.length(), alleles);
            this.vc = builder.make();
        }

        public String toString() {
            return String.format("%s refBases=%s trueRepeat=%b vc=%s", super.toString(), ref, isTrueRepeat, vc);
        }
    }

    @DataProvider(name = "RepeatDetectorTest")
    public Object[][] makeRepeatDetectorTest() {
        new RepeatDetectorTest(true,  "NAAC", "N", "NA");
        new RepeatDetectorTest(true,  "NAAC", "NA", "N");
        new RepeatDetectorTest(false, "NAAC", "NAA", "N");
        new RepeatDetectorTest(false, "NAAC", "N", "NC");
        new RepeatDetectorTest(false, "AAC", "A", "C");

        // running out of ref bases => false
        new RepeatDetectorTest(false, "NAAC", "N", "NCAGTA");

        // complex repeats
        new RepeatDetectorTest(true,  "NATATATC", "N", "NAT");
        new RepeatDetectorTest(true,  "NATATATC", "N", "NATA");
        new RepeatDetectorTest(true,  "NATATATC", "N", "NATAT");
        new RepeatDetectorTest(true,  "NATATATC", "NAT", "N");
        new RepeatDetectorTest(false, "NATATATC", "NATA", "N");
        new RepeatDetectorTest(false, "NATATATC", "NATAT", "N");

        // multi-allelic
        new RepeatDetectorTest(true,  "NATATATC", "N", "NAT", "NATAT");
        new RepeatDetectorTest(true,  "NATATATC", "N", "NAT", "NATA");
        new RepeatDetectorTest(true,  "NATATATC", "NAT", "N", "NATAT");
        new RepeatDetectorTest(true,  "NATATATC", "NAT", "N", "NATA"); // two As
        new RepeatDetectorTest(false, "NATATATC", "NAT", "N", "NATC"); // false
        new RepeatDetectorTest(false, "NATATATC", "NAT", "N", "NCC"); // false
        new RepeatDetectorTest(false, "NATATATC", "NAT", "NATAT", "NCC"); // false

        return RepeatDetectorTest.getTests(RepeatDetectorTest.class);
    }

    @Test(dataProvider = "RepeatDetectorTest")
    public void testRepeatDetectorTest(RepeatDetectorTest cfg) {

        // test alleles are equal
        Assert.assertEquals(GATKVariantContextUtils.isTandemRepeat(cfg.vc, cfg.ref.getBytes()), cfg.isTrueRepeat);
    }

    @Test
    public void testRepeatAllele() {
        Allele nullR = Allele.create("A", true);
        Allele nullA = Allele.create("A", false);
        Allele atc   = Allele.create("AATC", false);
        Allele atcatc   = Allele.create("AATCATC", false);
        Allele ccccR = Allele.create("ACCCC", true);
        Allele cc   = Allele.create("ACC", false);
        Allele cccccc   = Allele.create("ACCCCCC", false);
        Allele gagaR   = Allele.create("AGAGA", true);
        Allele gagagaga   = Allele.create("AGAGAGAGA", false);

        // - / ATC [ref] from 20-22
        String delLoc = "chr1";
        int delLocStart = 20;
        int delLocStop = 22;

        // - [ref] / ATC from 20-20
        String insLoc = "chr1";
        int insLocStart = 20;
        int insLocStop = 20;

        Pair<List<Integer>,byte[]> result;
        byte[] refBytes = "TATCATCATCGGA".getBytes();

        Assert.assertEquals(GATKVariantContextUtils.findNumberofRepetitions("ATG".getBytes(), "ATGATGATGATG".getBytes(), true),4);
        Assert.assertEquals(GATKVariantContextUtils.findNumberofRepetitions("G".getBytes(), "ATGATGATGATG".getBytes(), true),0);
        Assert.assertEquals(GATKVariantContextUtils.findNumberofRepetitions("T".getBytes(), "T".getBytes(), true),1);
        Assert.assertEquals(GATKVariantContextUtils.findNumberofRepetitions("AT".getBytes(), "ATGATGATCATG".getBytes(), true),1);
        Assert.assertEquals(GATKVariantContextUtils.findNumberofRepetitions("CCC".getBytes(), "CCCCCCCC".getBytes(), true),2);

        Assert.assertEquals(GATKVariantContextUtils.findRepeatedSubstring("ATG".getBytes()),3);
        Assert.assertEquals(GATKVariantContextUtils.findRepeatedSubstring("AAA".getBytes()),1);
        Assert.assertEquals(GATKVariantContextUtils.findRepeatedSubstring("CACACAC".getBytes()),7);
        Assert.assertEquals(GATKVariantContextUtils.findRepeatedSubstring("CACACA".getBytes()),2);
        Assert.assertEquals(GATKVariantContextUtils.findRepeatedSubstring("CATGCATG".getBytes()),4);
        Assert.assertEquals(GATKVariantContextUtils.findRepeatedSubstring("AATAATA".getBytes()),7);


        // A*,ATC, context = ATC ATC ATC : (ATC)3 -> (ATC)4
        VariantContext vc = new VariantContextBuilder("foo", insLoc, insLocStart, insLocStop, Arrays.asList(nullR,atc)).make();
        result = GATKVariantContextUtils.getNumTandemRepeatUnits(vc, refBytes);
        Assert.assertEquals(result.getFirst().toArray()[0],3);
        Assert.assertEquals(result.getFirst().toArray()[1],4);
        Assert.assertEquals(result.getSecond().length,3);

        // ATC*,A,ATCATC
        vc = new VariantContextBuilder("foo", insLoc, insLocStart, insLocStart+3, Arrays.asList(Allele.create("AATC", true),nullA,atcatc)).make();
        result = GATKVariantContextUtils.getNumTandemRepeatUnits(vc, refBytes);
        Assert.assertEquals(result.getFirst().toArray()[0],3);
        Assert.assertEquals(result.getFirst().toArray()[1],2);
        Assert.assertEquals(result.getFirst().toArray()[2],4);
        Assert.assertEquals(result.getSecond().length,3);

        // simple non-tandem deletion: CCCC*, -
        refBytes = "TCCCCCCCCATG".getBytes();
        vc = new VariantContextBuilder("foo", delLoc, 10, 14, Arrays.asList(ccccR,nullA)).make();
        result = GATKVariantContextUtils.getNumTandemRepeatUnits(vc, refBytes);
        Assert.assertEquals(result.getFirst().toArray()[0],8);
        Assert.assertEquals(result.getFirst().toArray()[1],4);
        Assert.assertEquals(result.getSecond().length,1);

        // CCCC*,CC,-,CCCCCC, context = CCC: (C)7 -> (C)5,(C)3,(C)9
        refBytes = "TCCCCCCCAGAGAGAG".getBytes();
        vc = new VariantContextBuilder("foo", insLoc, insLocStart, insLocStart+4, Arrays.asList(ccccR,cc, nullA,cccccc)).make();
        result = GATKVariantContextUtils.getNumTandemRepeatUnits(vc, refBytes);
        Assert.assertEquals(result.getFirst().toArray()[0],7);
        Assert.assertEquals(result.getFirst().toArray()[1],5);
        Assert.assertEquals(result.getFirst().toArray()[2],3);
        Assert.assertEquals(result.getFirst().toArray()[3],9);
        Assert.assertEquals(result.getSecond().length,1);

        // GAGA*,-,GAGAGAGA
        refBytes = "TGAGAGAGAGATTT".getBytes();
        vc = new VariantContextBuilder("foo", insLoc, insLocStart, insLocStart+4, Arrays.asList(gagaR, nullA,gagagaga)).make();
        result = GATKVariantContextUtils.getNumTandemRepeatUnits(vc, refBytes);
        Assert.assertEquals(result.getFirst().toArray()[0],5);
        Assert.assertEquals(result.getFirst().toArray()[1],3);
        Assert.assertEquals(result.getFirst().toArray()[2],7);
        Assert.assertEquals(result.getSecond().length,2);

    }

    // --------------------------------------------------------------------------------
    //
    // test forward clipping
    //
    // --------------------------------------------------------------------------------

    @DataProvider(name = "ForwardClippingData")
    public Object[][] makeForwardClippingData() {
        List<Object[]> tests = new ArrayList<Object[]>();

        // this functionality can be adapted to provide input data for whatever you might want in your data
        tests.add(new Object[]{Arrays.asList("A"), -1});
        tests.add(new Object[]{Arrays.asList("<DEL>"), -1});
        tests.add(new Object[]{Arrays.asList("A", "C"), -1});
        tests.add(new Object[]{Arrays.asList("AC", "C"), -1});
        tests.add(new Object[]{Arrays.asList("A", "G"), -1});
        tests.add(new Object[]{Arrays.asList("A", "T"), -1});
        tests.add(new Object[]{Arrays.asList("GT", "CA"), -1});
        tests.add(new Object[]{Arrays.asList("GT", "CT"), -1});
        tests.add(new Object[]{Arrays.asList("ACC", "AC"), 0});
        tests.add(new Object[]{Arrays.asList("ACGC", "ACG"), 1});
        tests.add(new Object[]{Arrays.asList("ACGC", "ACG"), 1});
        tests.add(new Object[]{Arrays.asList("ACGC", "ACGA"), 2});
        tests.add(new Object[]{Arrays.asList("ACGC", "AGC"), 0});
        tests.add(new Object[]{Arrays.asList("A", "<DEL>"), -1});
        for ( int len = 0; len < 50; len++ )
            tests.add(new Object[]{Arrays.asList("A" + new String(Utils.dupBytes((byte)'C', len)), "C"), -1});

        tests.add(new Object[]{Arrays.asList("A", "T", "C"), -1});
        tests.add(new Object[]{Arrays.asList("AT", "AC", "AG"), 0});
        tests.add(new Object[]{Arrays.asList("AT", "AC", "A"), -1});
        tests.add(new Object[]{Arrays.asList("AT", "AC", "ACG"), 0});
        tests.add(new Object[]{Arrays.asList("AC", "AC", "ACG"), 0});
        tests.add(new Object[]{Arrays.asList("AC", "ACT", "ACG"), 0});
        tests.add(new Object[]{Arrays.asList("ACG", "ACGT", "ACGTA"), 1});
        tests.add(new Object[]{Arrays.asList("ACG", "ACGT", "ACGCA"), 1});

        return tests.toArray(new Object[][]{});
    }

    @Test(dataProvider = "ForwardClippingData")
    public void testForwardClipping(final List<String> alleleStrings, final int expectedClip) {
        final List<Allele> alleles = new LinkedList<Allele>();
        for ( final String alleleString : alleleStrings )
            alleles.add(Allele.create(alleleString));

        for ( final List<Allele> myAlleles : Utils.makePermutations(alleles, alleles.size(), false)) {
            final int actual = GATKVariantContextUtils.computeForwardClipping(myAlleles);
            Assert.assertEquals(actual, expectedClip);
        }
    }

    @DataProvider(name = "ClipAlleleTest")
    public Object[][] makeClipAlleleTest() {
        List<Object[]> tests = new ArrayList<Object[]>();

        // this functionality can be adapted to provide input data for whatever you might want in your data
        tests.add(new Object[]{Arrays.asList("ACC", "AC"), Arrays.asList("AC", "A"), 0});
        tests.add(new Object[]{Arrays.asList("ACGC", "ACG"), Arrays.asList("GC", "G"), 2});
        tests.add(new Object[]{Arrays.asList("ACGC", "ACGA"), Arrays.asList("C", "A"), 3});
        tests.add(new Object[]{Arrays.asList("ACGC", "AGC"), Arrays.asList("AC", "A"), 0});
        tests.add(new Object[]{Arrays.asList("AT", "AC", "AG"), Arrays.asList("T", "C", "G"), 1});
        tests.add(new Object[]{Arrays.asList("AT", "AC", "ACG"), Arrays.asList("T", "C", "CG"), 1});
        tests.add(new Object[]{Arrays.asList("AC", "ACT", "ACG"), Arrays.asList("C", "CT", "CG"), 1});
        tests.add(new Object[]{Arrays.asList("ACG", "ACGT", "ACGTA"), Arrays.asList("G", "GT", "GTA"), 2});
        tests.add(new Object[]{Arrays.asList("ACG", "ACGT", "ACGCA"), Arrays.asList("G", "GT", "GCA"), 2});

        // trims from left and right
        tests.add(new Object[]{Arrays.asList("ACGTT", "ACCTT"), Arrays.asList("G", "C"), 2});
        tests.add(new Object[]{Arrays.asList("ACGTT", "ACCCTT"), Arrays.asList("G", "CC"), 2});
        tests.add(new Object[]{Arrays.asList("ACGTT", "ACGCTT"), Arrays.asList("G", "GC"), 2});

        return tests.toArray(new Object[][]{});
    }

    @Test(dataProvider = "ClipAlleleTest")
    public void testClipAlleles(final List<String> alleleStrings, final List<String> expected, final int numLeftClipped) {
        final int start = 10;
        final VariantContext unclipped = GATKVariantContextUtils.makeFromAlleles("test", "20", start, alleleStrings);
        final VariantContext clipped = GATKVariantContextUtils.trimAlleles(unclipped, true, true);

        Assert.assertEquals(clipped.getStart(), unclipped.getStart() + numLeftClipped);
        for ( int i = 0; i < unclipped.getAlleles().size(); i++ ) {
            final Allele trimmed = clipped.getAlleles().get(i);
            Assert.assertEquals(trimmed.getBaseString(), expected.get(i));
        }
    }

    // --------------------------------------------------------------------------------
    //
    // test primitive allele splitting
    //
    // --------------------------------------------------------------------------------

    @DataProvider(name = "PrimitiveAlleleSplittingData")
    public Object[][] makePrimitiveAlleleSplittingData() {
        List<Object[]> tests = new ArrayList<Object[]>();

        // no split
        tests.add(new Object[]{"A", "C", 0, null});
        tests.add(new Object[]{"A", "AC", 0, null});
        tests.add(new Object[]{"AC", "A", 0, null});

        // one split
        tests.add(new Object[]{"ACA", "GCA", 1, Arrays.asList(0)});
        tests.add(new Object[]{"ACA", "AGA", 1, Arrays.asList(1)});
        tests.add(new Object[]{"ACA", "ACG", 1, Arrays.asList(2)});

        // two splits
        tests.add(new Object[]{"ACA", "GGA", 2, Arrays.asList(0, 1)});
        tests.add(new Object[]{"ACA", "GCG", 2, Arrays.asList(0, 2)});
        tests.add(new Object[]{"ACA", "AGG", 2, Arrays.asList(1, 2)});

        // three splits
        tests.add(new Object[]{"ACA", "GGG", 3, Arrays.asList(0, 1, 2)});

        return tests.toArray(new Object[][]{});
    }

    @Test(dataProvider = "PrimitiveAlleleSplittingData")
    public void testPrimitiveAlleleSplitting(final String ref, final String alt, final int expectedSplit, final List<Integer> variantPositions) {

        final int start = 10;
        final VariantContext vc = GATKVariantContextUtils.makeFromAlleles("test", "20", start, Arrays.asList(ref, alt));

        final List<VariantContext> result = GATKVariantContextUtils.splitIntoPrimitiveAlleles(vc);

        if ( expectedSplit > 0 ) {
            Assert.assertEquals(result.size(), expectedSplit);
            for ( int i = 0; i < variantPositions.size(); i++ ) {
                Assert.assertEquals(result.get(i).getStart(), start + variantPositions.get(i));
            }
        } else {
            Assert.assertEquals(result.size(), 1);
            Assert.assertEquals(vc, result.get(0));
        }
    }

    // --------------------------------------------------------------------------------
    //
    // test allele remapping
    //
    // --------------------------------------------------------------------------------

    @DataProvider(name = "AlleleRemappingData")
    public Object[][] makeAlleleRemappingData() {
        List<Object[]> tests = new ArrayList<Object[]>();

        final Allele originalBase1 = Allele.create((byte)'A');
        final Allele originalBase2 = Allele.create((byte)'T');

        for ( final byte base1 : BaseUtils.BASES ) {
            for ( final byte base2 : BaseUtils.BASES ) {
                for ( final int numGenotypes : Arrays.asList(0, 1, 2, 5) ) {
                    Map<Allele, Allele> map = new HashMap<Allele, Allele>(2);
                    map.put(originalBase1, Allele.create(base1));
                    map.put(originalBase2, Allele.create(base2));

                    tests.add(new Object[]{map, numGenotypes});
                }
            }
        }

        return tests.toArray(new Object[][]{});
    }

    @Test(dataProvider = "AlleleRemappingData")
    public void testAlleleRemapping(final Map<Allele, Allele> alleleMap, final int numGenotypes) {

        final GATKVariantContextUtils.AlleleMapper alleleMapper = new GATKVariantContextUtils.AlleleMapper(alleleMap);

        final GenotypesContext originalGC = createGenotypesContext(numGenotypes, new ArrayList(alleleMap.keySet()));

        final GenotypesContext remappedGC = GATKVariantContextUtils.updateGenotypesWithMappedAlleles(originalGC, alleleMapper);

        for ( int i = 0; i < numGenotypes; i++ ) {
            final Genotype originalG = originalGC.get(String.format("%d", i));
            final Genotype remappedG = remappedGC.get(String.format("%d", i));

            Assert.assertEquals(originalG.getAlleles().size(), remappedG.getAlleles().size());
            for ( int j = 0; j < originalG.getAlleles().size(); j++ )
                Assert.assertEquals(remappedG.getAllele(j), alleleMap.get(originalG.getAllele(j)));
        }
    }

    private static GenotypesContext createGenotypesContext(final int numGenotypes, final List<Allele> alleles) {
        GenomeAnalysisEngine.resetRandomGenerator();
        final Random random = GenomeAnalysisEngine.getRandomGenerator();

        final GenotypesContext gc = GenotypesContext.create();
        for ( int i = 0; i < numGenotypes; i++ ) {
            // choose alleles at random
            final List<Allele> myAlleles = new ArrayList<Allele>();
            myAlleles.add(alleles.get(random.nextInt(2)));
            myAlleles.add(alleles.get(random.nextInt(2)));

            final Genotype g = new GenotypeBuilder(String.format("%d", i)).alleles(myAlleles).make();
            gc.add(g);
        }

        return gc;
    }
}