/*
 *                  Aozan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU General Public License version 3 or later 
 * and CeCILL. This should be distributed with the code. If you 
 * do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/gpl-3.0-standalone.html
 *      http://www.cecill.info/licences/Licence_CeCILL_V2-en.html
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Aozan project and its aims,
 * or to join the Aozan Google group, visit the home page at:
 *
 *      http://www.transcriptome.ens.fr/aozan
 *
 */

package fr.ens.transcriptome.aozan.tests.sample;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import fr.ens.transcriptome.aozan.AozanException;
import fr.ens.transcriptome.aozan.RunData;
import fr.ens.transcriptome.aozan.collectors.FastqScreenCollector;
import fr.ens.transcriptome.aozan.fastqscreen.FastqScreenGenomeMapper;
import fr.ens.transcriptome.aozan.tests.AozanTest;

/**
 * The class add in the qc report html values from FastqScreen for each sample
 * and for each reference genomes. The list of references genomes contains
 * default references genomes defined in aozan configuration file. It add the
 * genomes sample for the run included in casava design file, only if it can be
 * used for mapping with bowtie. The alias genomes file make the correspondence
 * between the genome sample and the reference genome used with bowtie, if it
 * exists. The class retrieve the percent of reads mapped on each reference
 * genomes.
 * @since 1.0
 * @author Sandrine Perrin
 */
public class FastqScreenSimpleSampleTest extends AbstractSimpleSampleTest {

  private final String genomeReference;
  private final boolean isGenomeContamination;

  @Override
  public List<String> getCollectorsNamesRequiered() {
    return ImmutableList.of(FastqScreenCollector.COLLECTOR_NAME);
  }

  @Override
  public String getKey(int read, int readSample, int lane, String sampleName) {

    String value =
        isGenomeContamination
            ? ".mapped.percent" : ".one.hit.one.library.percent";

    // Check indetermined indexed sample
    if (sampleName == null) {
      return "fastqscreen.lane"
          + lane + ".undetermined.read" + readSample + "." + genomeReference + value;
    }
    return "fastqscreen.lane"
        + lane + ".sample." + sampleName + ".read" + readSample + "."
        + sampleName + "." + genomeReference + value;
  }

  @Override
  public Class<?> getValueType() {
    return Double.class;
  }

  /**
   * Transform the score : if genome of sample is the same as reference genome
   * then the score is reverse for change the color in QC report
   * @param data run data
   * @param read index of read
   * @param readSample index of read without indexed reads
   * @param lane lane index
   * @param sampleName sample name
   * @return the transformed score
   */
  @Override
  protected int transformScore(final int score, final RunData data,
      final int read, int readSample, final int lane, final String sampleName) {

    String keyGenomeSample =
        "design.lane" + lane + "." + sampleName + ".sample.ref";

    // Set genome sample
    String genomeSample = data.get(keyGenomeSample);

    // Set reference genome corresponding of genome sample if it exists
    String genomeSampleReference = null;
    try {
      genomeSampleReference =
          FastqScreenGenomeMapper.getInstance()
              .getGenomeReferenceCorresponding(genomeSample);
    } catch (AozanException e) {
    }

    // If genome sample are used like reference genome in FastqScreen, the score
    // are inverse. The value must be near 100% if it had no contamination.
    if (this.genomeReference.equals(genomeSampleReference))
      return (9 - score);

    return score;
  }

  @Override
  public List<AozanTest> configure(final Map<String, String> properties)
      throws AozanException {

    if (properties == null || properties.isEmpty())
      throw new NullPointerException("The properties object is null or empty");

    // Initialization fastqScreenGenomeMapper object
    final FastqScreenGenomeMapper fqsm =
        FastqScreenGenomeMapper.getInstance(properties);

    //
    final Set<String> genomes = fqsm.getGenomesToMapping();

    List<AozanTest> list = new ArrayList<AozanTest>();

    for (String genome : genomes) {

      // Create an new AozanTest for each reference genome
      final FastqScreenSimpleSampleTest testGenome =
          new FastqScreenSimpleSampleTest(genome,
              fqsm.isGenomeContamination(genome));

      testGenome.internalConfigure(properties);

      list.add(testGenome);
    }

    return list;
  }

  @Override
  public boolean isValuePercent() {
    return true;
  }

  /**
   * Get name of reference genome
   * @return name of reference genome
   */
  public String getNameGenome() {
    return this.genomeReference;
  }

  private void internalConfigure(final Map<String, String> properties)
      throws AozanException {
    super.configure(properties);
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   */
  public FastqScreenSimpleSampleTest() {
    this(null, false);
  }

  /**
   * Public constructor, specific for a reference genome
   * @param genome name of reference genome
   */
  public FastqScreenSimpleSampleTest(final String genome,
      final boolean isGenomeContamination) {
    super("fsqmapped", "", "fastqscreen "
        + (isGenomeContamination ? "" : "single ") + "mapped on " + genome, "%");
    this.genomeReference = genome;
    this.isGenomeContamination = isGenomeContamination;
  }

}
