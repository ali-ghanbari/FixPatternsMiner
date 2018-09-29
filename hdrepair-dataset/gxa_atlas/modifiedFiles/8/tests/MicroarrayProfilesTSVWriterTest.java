/*
 * Copyright 2008-2013 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.atlas.commands.download;

import com.google.common.collect.Sets;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.Resource;
import uk.ac.ebi.atlas.commands.context.MicroarrayRequestContext;
import uk.ac.ebi.atlas.commons.streams.ObjectInputStream;
import uk.ac.ebi.atlas.model.differential.Contrast;
import uk.ac.ebi.atlas.model.differential.Regulation;
import uk.ac.ebi.atlas.model.differential.microarray.MicroarrayExperiment;
import uk.ac.ebi.atlas.model.differential.microarray.MicroarrayExpression;
import uk.ac.ebi.atlas.model.differential.microarray.MicroarrayProfile;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MicroarrayProfilesTSVWriterTest {

    private static final String HEADER_TEMPLATE = "# Expression Atlas version: 0.1.4-SNAPSHOT\n" +
            "# Query: Genes matching: ''{0}''{1},{2}{3} differentially expressed in {4} given the p-value cutoff {5} and log2-fold change cutoff {6} in experiment {7}\n" +
            "# Timestamp: {8}";

    @Mock
    private MicroarrayProfile geneProfileMock;

    @Mock
    private MicroarrayExpression expressionMock;

    @Mock
    private MicroarrayRequestContext microarrayRequestContextMock;

    @Mock
    private ObjectInputStream<MicroarrayProfile> inputStreamMock;

    @Mock
    private PrintWriter responseWriterMock;

    @Mock
    private MicroarrayExperiment experimentMock;

    @Mock
    private Resource resourceMock;

    private MicroarrayProfilesTSVWriter subject;

    private TreeSet<Contrast> conditions;


    @Before
    public void initMocks() throws Exception {
        when(geneProfileMock.getExpression(any(Contrast.class))).thenReturn(expressionMock);
        when(microarrayRequestContextMock.getGeneQuery()).thenReturn("");
        when(microarrayRequestContextMock.getSelectedQueryFactors()).thenReturn(Collections.EMPTY_SET);
        when(microarrayRequestContextMock.getRegulation()).thenReturn(Regulation.UP);
        when(microarrayRequestContextMock.getExperiment()).thenReturn(experimentMock);
        when(experimentMock.getAccession()).thenReturn("ACCESSION");

        when(resourceMock.getInputStream()).thenReturn(new ByteArrayInputStream(HEADER_TEMPLATE.getBytes()));

        subject = new MicroarrayProfilesTSVWriter(new CsvWriterFactory());
        subject.setRequestContext(microarrayRequestContextMock);
        subject.setTsvFileMastheadTemplateResource(resourceMock);

        Contrast contrast1 = mock(Contrast.class);
        when(contrast1.getDisplayName()).thenReturn("cond1");
        Contrast contrast2 = mock(Contrast.class);
        when(contrast2.getDisplayName()).thenReturn("cond2");

        conditions = Sets.newTreeSet();
        conditions.add(contrast1);
        conditions.add(contrast2);

        //given
        when(expressionMock.getFoldChange()).thenReturn(-0.978932452151424);
        when(expressionMock.getPValue()).thenReturn(0.134707651014487);
        when(expressionMock.getTstatistic()).thenReturn(0.0099999999);
    }

    @Test
    public void buildConditionExpressionsHeaders() throws Exception {

        String[] columnNames = subject.getConditionColumnHeaders(conditions);
        assertThat(columnNames.length, is(6));
        assertThat(columnNames, arrayContaining("cond1.p-value", "cond1.log2foldchange", "cond1.t-statistic", "cond2.p-value", "cond2.log2foldchange", "cond2.t-statistic"));
    }

    @Test
    public void testBuildExpressionsRow() throws Exception {

        SortedSet<Contrast> contrasts = new TreeSet<>();
        contrasts.add(new Contrast("id1", null, null, null, "name"));

        //when
        String[] expressions = subject.extractConditionLevels(geneProfileMock, contrasts);

        //then
        assertThat(expressions.length, Matchers.is(3));
        assertThat(expressions, is(new String[]{"0.134707651014487", "-0.978932452151424", "0.0099999999"}));

    }

    @Test
    public void testGetValueToString() throws Exception {
        assertThat(subject.getValueToString(0.134707651014487), is("0.134707651014487"));
        assertThat(subject.getValueToString(Double.POSITIVE_INFINITY), is("NA"));
        assertThat(subject.getValueToString(Double.NEGATIVE_INFINITY), is("NA"));
    }

    @Test
    public void testWrite() throws Exception {
        subject.setResponseWriter(responseWriterMock);
        subject.write(inputStreamMock, conditions);
        subject.getTsvFileMasthead();

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(responseWriterMock).write(stringArgumentCaptor.capture());

        assertThat(stringArgumentCaptor.getValue(), startsWith("# Expression Atlas version:"));

        assertThat(stringArgumentCaptor.getValue() , containsString(
                "\n# Query: Genes matching: '', up differentially expressed in any contrast given the p-value cutoff 0 and log2-fold change cutoff 0 in experiment ACCESSION" +
                        "\n# Timestamp: "));

        verify(responseWriterMock).write("Gene ID\tGene Name\tDesign Element\tcond1.p-value\tcond1.log2foldchange\tcond1.t-statistic\tcond2.p-value\tcond2.log2foldchange\tcond2.t-statistic\n", 0, 139);

        verify(responseWriterMock, times(1)).flush();
    }

    @Test
    public void testGetExpressionColumnsHeaders() {
        assertThat(subject.getExpressionColumnsHeaders(), contains("p-value", "log2foldchange", "t-statistic"));
    }

    @Test
    public void testGetExpressionLevelData() {
        assertThat(subject.getExpressionLevelData(expressionMock), contains(0.134707651014487, -0.978932452151424, 0.0099999999));
    }

    @Test
    public void testGetProfileIdColumnHeaders() {
        assertThat(subject.getProfileIdColumnHeaders(), arrayContaining("Gene ID", "Gene Name", "Design Element"));
    }

    @Test
    public void testGetSecondaryRowHeader() {
        when(geneProfileMock.getDesignElementName()).thenReturn("NAME");
        assertThat(subject.getSecondaryRowHeader(geneProfileMock), is("NAME"));
    }
}
