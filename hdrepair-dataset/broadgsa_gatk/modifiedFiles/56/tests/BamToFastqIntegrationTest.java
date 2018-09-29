package org.broadinstitute.sting.gatk.walkers.fasta;

import org.broadinstitute.sting.WalkerTest;
import org.junit.Test;

import java.util.Arrays;

public class BamToFastqIntegrationTest extends WalkerTest {
    @Test
    public void testIntervals() {

        WalkerTestSpec spec1 = new WalkerTestSpec(
                "-T BamToFastq -R /broad/1KG/reference/human_b36_both.fasta -I /humgen/gsa-scr1/GATK_Data/Validation_Data/NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam -L 1:10,000,100-10,000,500;1:10,100,000-10,101,000;1:10,900,000-10,900,001 -o %s",
                 1,
                 Arrays.asList("f742731e17fba105c7daae0e4f80ca1d"));
        executeTest("testBamToFasta", spec1);

        WalkerTestSpec spec2 = new WalkerTestSpec(
                "-T BamToFastq -reverse -R /broad/1KG/reference/human_b36_both.fasta -I /humgen/gsa-scr1/GATK_Data/Validation_Data/NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam -L 1:10,000,100-10,000,500;1:10,100,000-10,101,000;1:10,900,000-10,900,001 -o %s",
                 1,
                 Arrays.asList("19a5418fdf7b53dac8badb67bb1e1b88"));
        executeTest("testBamToFastaReverse", spec2);
    }
}
