/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eqtlmappingpipeline;

import java.io.File;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import umcg.genetica.io.trityper.EQTL;
import umcg.genetica.io.trityper.eQTLTextFile;
import eqtlmappingpipeline.util.eQTLFileSorter;

/**
 *
 * @author MarcJan
 */
public class FullQtlMappingTransMetaTest {

    private File tmpOutputFolder;
    private final String fileSep = System.getProperty("file.separator");
    private final File testFilesFolder;

    public FullQtlMappingTransMetaTest() throws URISyntaxException {
        testFilesFolder = new File(this.getClass().getResource("/GeuvadisTestData/").toURI());
        System.out.println("Using test data from: " + testFilesFolder.getAbsolutePath());
    }

    @BeforeTest
    public void setUpMethod() throws Exception {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date date = new Date();

        tmpOutputFolder = new File(tmpDir, "QTLMappingTransMetaTest_" + dateFormat.format(date));

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Removing tmp dir and files");
                for (File file : tmpOutputFolder.listFiles()) {
                    System.out.println(" - Deleting: " + file.getAbsolutePath());
                    file.deleteOnExit();
                }
                System.out.println(" - Deleting: " + tmpOutputFolder.getAbsolutePath());
                tmpOutputFolder.deleteOnExit();
            }
        });

        tmpOutputFolder.mkdir();

        System.out.println("Temp folder with output of this test: " + tmpOutputFolder.getAbsolutePath());
    }

    @Test
    public void testMain() throws Exception {
        String settingsFile = testFilesFolder + fileSep + "settings.xml";
        System.out.println(settingsFile);

        Main.main("--mode", "metaqtl", "--settings", settingsFile, "--replacetext", "${InputFolder}", "--replacetextwith", testFilesFolder.getAbsolutePath(), "--replacetext2", "${OutputFolder}", "--replacetext2with", tmpOutputFolder.getAbsolutePath());

        eQTLFileSorter r = new eQTLFileSorter();
        r.run(tmpOutputFolder.getAbsolutePath()+fileSep+"eQTLsFDR0.05-ProbeLevel.txt", tmpOutputFolder.getAbsolutePath()+fileSep+"eQTLsFDR0.05-ProbeLevel_S.txt");
        
        eQTLTextFile eExp = new eQTLTextFile(testFilesFolder+fileSep+"TestOutput"+fileSep+"Trans-Meta-eQTLsFDR0.05-ProbeLevel.txt", eQTLTextFile.R);
        
        eQTLTextFile eActual = new eQTLTextFile(tmpOutputFolder.getAbsolutePath()+fileSep+"eQTLsFDR0.05-ProbeLevel_S.txt", eQTLTextFile.R);

        Iterator<EQTL> eExpIterator = eExp.getEQtlIterator();
        Iterator<EQTL> eActualIterator = eActual.getEQtlIterator();
        
        while(eExpIterator.hasNext() && eActualIterator.hasNext()){
            assertTrue(eActualIterator.next().sameQTL(eExpIterator.next()), "eQTL not identical");
            
        }
        
        assertFalse(eExpIterator.hasNext(), "not all expected eQTL are found");
        assertFalse(eActualIterator.hasNext(), "found more eQTL than expected");
    }
}
