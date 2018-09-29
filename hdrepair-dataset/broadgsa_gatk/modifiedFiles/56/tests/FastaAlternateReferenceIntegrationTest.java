package org.broadinstitute.sting.gatk.walkers.fasta;

import org.broadinstitute.sting.WalkerTest;
import org.junit.Test;

import java.util.Arrays;

public class FastaAlternateReferenceIntegrationTest extends WalkerTest {
    @Test
    public void testIntervals() {

        WalkerTestSpec spec1 = new WalkerTestSpec(
                "-T FastaAlternateReferenceMaker -R /broad/1KG/reference/human_b36_both.fasta -L 1:10,000,100-10,000,500;1:10,100,000-10,101,000;1:10,900,000-10,900,001 -o %s",
                 1,
                 Arrays.asList("328d2d52cedfdc52da7d1abff487633d"));
        executeTest("testFastaReference", spec1);

        WalkerTestSpec spec2 = new WalkerTestSpec(
                "-T FastaAlternateReferenceMaker -R /broad/1KG/reference/human_b36_both.fasta -B indels,PointIndel,/humgen/gsa-scr1/GATK_Data/Validation_Data/NA12878.chr1_10mb_11mb.slx.indels -B snpmask,dbsnp,/humgen/gsa-scr1/GATK_Data/dbsnp_129_b36.rod -L 1:10,075,000-10,075,380;1:10,093,447-10,093,847;1:10,271,252-10,271,452 -o %s",
                 1,
                 Arrays.asList("b7c0d95c1bf1ea69f5a7a58dd4d57685"));
        executeTest("testFastaAlternateReferenceIndels", spec2);

        WalkerTestSpec spec3 = new WalkerTestSpec(
                "-T FastaAlternateReferenceMaker -sequenom -R /broad/1KG/reference/human_b36_both.fasta -B indels,PointIndel,/humgen/gsa-scr1/GATK_Data/Validation_Data/NA12878.chr1_10mb_11mb.slx.indels -B snpmask,dbsnp,/humgen/gsa-scr1/GATK_Data/dbsnp_129_b36.rod -L 1:10,075,000-10,075,380;1:10,093,447-10,093,847;1:10,271,252-10,271,452 -o %s",
                 1,
                 Arrays.asList("3137b0c90ba13fb648be51c14fdb296f"));
        executeTest("testFastaAlternateReferenceIndelSequenom", spec3);
	
        WalkerTestSpec spec4 = new WalkerTestSpec(
                "-T FastaAlternateReferenceMaker -R /broad/1KG/reference/human_b36_both.fasta -B snps,Variants,/humgen/gsa-scr1/GATK_Data/Validation_Data/NA12878.chr1_10mb_11mb.slx.geli.calls -B snpmask,dbsnp,/humgen/gsa-scr1/GATK_Data/dbsnp_129_b36.rod -L 1:10,023,400-10,023,500;1:10,029,200-10,029,500 -o %s",
                 1,
                 Arrays.asList("82705a88f6fc25880dd2331183531d9a"));
        executeTest("testFastaAlternateReferenceSnps", spec4);

        WalkerTestSpec spec5 = new WalkerTestSpec(
                "-T FastaAlternateReferenceMaker -sequenom -R /broad/1KG/reference/human_b36_both.fasta -B snps,Variants,/humgen/gsa-scr1/GATK_Data/Validation_Data/NA12878.chr1_10mb_11mb.slx.geli.calls -B snpmask,dbsnp,/humgen/gsa-scr1/GATK_Data/dbsnp_129_b36.rod -L 1:10,023,400-10,023,500;1:10,029,200-10,029,500 -o %s",
                 1,
                 Arrays.asList("c6744c61009a7c15899ce4ef1faa0000"));
        executeTest("testFastaAlternateReferenceSnpsSequenom", spec5);
    }
}
