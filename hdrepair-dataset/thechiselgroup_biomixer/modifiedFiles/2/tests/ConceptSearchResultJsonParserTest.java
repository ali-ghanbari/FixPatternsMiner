/*******************************************************************************
 * Copyright 2012 David Rusk
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *
 *    http://www.apache.org/licenses/LICENSE-2.0 
 *     
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 *******************************************************************************/
package org.thechiselgroup.biomixer.client.services.search;

import static org.junit.Assert.assertThat;
import static org.thechiselgroup.biomixer.shared.core.test.matchers.collections.CollectionMatchers.containsExactly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.thechiselgroup.biomixer.client.Concept;
import org.thechiselgroup.biomixer.client.core.resources.Resource;
import org.thechiselgroup.biomixer.client.services.AbstractJsonParserTest;
import org.thechiselgroup.biomixer.server.workbench.util.json.JavaJsonParser;

public class ConceptSearchResultJsonParserTest extends AbstractJsonParserTest {

    private ConceptSearchResultJsonParser underTest;

    public ConceptSearchResultJsonParserTest() {
        super(ConceptSearchResultJsonParserTest.class);
    }

    public List<String> getValues(Set<Resource> resources, String key) {
        List<String> values = new ArrayList<String>();
        for (Resource resource : resources) {
            values.add((String) resource.getValue(key));
        }
        return values;
    }

    @Test
    public void parseSearchResults() throws IOException {
        Set<Resource> searchResults = parseSearchResults("searchResults.json");

        List<String> virtualOntologyIds = getValues(searchResults,
                Concept.VIRTUAL_ONTOLOGY_ID);
        assertThat(virtualOntologyIds,
                containsExactly(Arrays.asList("1501", "1613", "1615")));

        List<String> shortIds = getValues(searchResults, Concept.SHORT_ID);
        assertThat(shortIds, containsExactly(Arrays.asList("neomark:Gene",
                "Gene", "bp:Gene")));
    }

    public Set<Resource> parseSearchResults(String jsonFilename)
            throws IOException {
        return underTest.parse(getFileContentsAsString(jsonFilename));
    }

    @Before
    public void setUp() {
        this.underTest = new ConceptSearchResultJsonParser(new JavaJsonParser());
    }

}
