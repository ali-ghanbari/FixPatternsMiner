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

package uk.ac.ebi.atlas.web.controllers.page;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.ui.Model;
import uk.ac.ebi.atlas.geneindex.SolrClient;
import uk.ac.ebi.atlas.web.BioentityPageProperties;

import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GenePageControllerTest {

    private static final String PROPERTY_TYPES = "symbol,description,synonym,ortholog,goterm,interproterm,ensfamily_description,ensgene,entrezgene,uniprot,mgi_id,gene_biotype,designelement_accession";
    private static final String SYNONYMS = "Synonyms";
    private static final String GENE_ONTOLOGY = "Gene Ontology";
    private static final String IDENTIFIER = "IDENTIFIER";
    private static final String SPECIES = "SPECIES";
    private static final String SYMBOL = "symbol";
    private static final String DESCRIPTION = "description";
    private static final String SYNONYM = "synonym";
    private static final String GOTERM = "goterm";
    private static final String ORTHOLOG = "ortholog";

    private GenePageController subject;

    @Mock
    private SolrClient solrClientMock;

    @Mock
    private Model modelMock;

    @Mock
    private BioentityPropertyService bioentityPropertyServiceMock;

    @Mock
    private DifferentialGeneProfileService differentialGeneProfileServiceMock;

    private Properties properties = new Properties();

    private Multimap<String, String> genePageProperties = HashMultimap.create();

    @Before
    public void setUp() throws Exception {
        properties.setProperty("property.synonym", SYNONYMS);
        properties.setProperty("property.goterm", GENE_ONTOLOGY);

        BioentityPageProperties bioentityPageProperties = new BioentityPageProperties(properties);

        genePageProperties.put(SYMBOL, SYMBOL);
        genePageProperties.put(DESCRIPTION, DESCRIPTION);
        genePageProperties.put(SYNONYM, SYNONYM);
        genePageProperties.put(GOTERM, GOTERM);
        genePageProperties.put(ORTHOLOG, ORTHOLOG);

        when(solrClientMock.findSpeciesForGeneId(IDENTIFIER)).thenReturn(SPECIES);
        when(solrClientMock.fetchGenePageProperties(IDENTIFIER, PROPERTY_TYPES.split(","))).thenReturn(genePageProperties);
        when(solrClientMock.findPropertyValuesForGeneId(IDENTIFIER, SYMBOL)).thenReturn(Lists.newArrayList(SYMBOL));

        when(bioentityPropertyServiceMock.getFirstValueOfProperty(SYMBOL)).thenReturn(SYMBOL);
        when(bioentityPropertyServiceMock.getFirstValueOfProperty(DESCRIPTION)).thenReturn(DESCRIPTION);

        subject = new GenePageController();

        subject.setBioentityPageProperties(bioentityPageProperties);
        subject.setBioentityPropertyService(bioentityPropertyServiceMock);
        subject.setDifferentialGeneProfileService(differentialGeneProfileServiceMock);
        subject.setGenePagePropertyTypes(PROPERTY_TYPES);
    }

    @Test
    public void testShowGenePage() throws Exception {
        assertThat(subject.showGenePage(IDENTIFIER, modelMock), is("gene"));
        verify(modelMock).addAttribute(GenePageController.PROPERTY_TYPE_SYMBOL, SYMBOL);
        verify(differentialGeneProfileServiceMock).getDifferentialProfilesListMapForIdentifier(IDENTIFIER, GenePageController.CUTOFF);
    }

    @Test
    public void testFilterPropertyTypes() {
        assertThat(subject.getFilteredPropertyTypes(), hasItems(SYNONYM, ORTHOLOG, GOTERM, "interproterm", "ensfamily_description", "ensgene", "entrezgene", "uniprot", "mgi_id", "gene_biotype", "designelement_accession"));
        assertThat(subject.getFilteredPropertyTypes(), not(hasItems(SYMBOL, DESCRIPTION)));
    }


}