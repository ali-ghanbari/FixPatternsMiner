package io.lumify.core.model.ontology;

import io.lumify.core.exception.LumifyException;
import io.lumify.web.clientapi.model.ClientApiOntology;
import org.atteo.evo.inflector.English;
import org.securegraph.Authorizations;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class Concept {
    private final String parentConceptIRI;
    private final Collection<OntologyProperty> properties;

    protected Concept(String parentConceptIRI, Collection<OntologyProperty> properties) {
        this.parentConceptIRI = parentConceptIRI;
        this.properties = properties;
    }

    public abstract String getTitle();

    public abstract boolean hasGlyphIconResource();

    public abstract String getColor();

    public abstract String getDisplayName();

    public abstract String getDisplayType();

    public abstract String getTitleFormula();

    public abstract Boolean getSearchable();

    public abstract String getSubtitleFormula();

    public abstract String getTimeFormula();

    public abstract boolean getUserVisible();

    public abstract List<String> getAddRelatedConceptWhiteList();

    public Collection<OntologyProperty> getProperties() {
        return properties;
    }

    protected String getParentConceptIRI() {
        return this.parentConceptIRI;
    }

    public ClientApiOntology.Concept toClientApi() {
        try {
            ClientApiOntology.Concept concept = new ClientApiOntology.Concept();
            concept.setId(getTitle());
            concept.setTitle(getTitle());
            concept.setDisplayName(getDisplayName());
            if (getDisplayType() != null) {
                concept.setDisplayType(getDisplayType());
            }
            if (getTitleFormula() != null) {
                concept.setTitleFormula(getTitleFormula());
            }
            if (getSearchable() != null) {
                concept.setSearchable(getSearchable());
            }
            if (getSubtitleFormula() != null) {
                concept.setSubtitleFormula(getSubtitleFormula());
            }
            if (getTimeFormula() != null) {
                concept.setTimeFormula(getTimeFormula());
            }
            if (getParentConceptIRI() != null) {
                concept.setParentConcept(getParentConceptIRI());
            }
            if (getDisplayName() != null) {
                concept.setPluralDisplayName(English.plural(getDisplayName()));
            }
            if (!getUserVisible()) {
                concept.setUserVisible(getUserVisible());
            }
            if (hasGlyphIconResource()) {
                concept.setGlyphIconHref("resource?id=" + URLEncoder.encode(getTitle(), "utf8"));
            }
            if (getColor() != null) {
                concept.setColor(getColor());
            }
            if (getAddRelatedConceptWhiteList() != null) {
                concept.getAddRelatedConceptWhiteList().addAll(getAddRelatedConceptWhiteList());
            }
            if (this.properties != null) {
                for (OntologyProperty property : this.properties) {
                    concept.getProperties().add(property.getTitle());
                }
            }
            return concept;
        } catch (UnsupportedEncodingException e) {
            throw new LumifyException("bad encoding", e);
        }
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", getDisplayName(), getTitle());
    }

    public static Collection<ClientApiOntology.Concept> toClientApiConcepts(Iterable<Concept> concepts) {
        Collection<ClientApiOntology.Concept> results = new ArrayList<ClientApiOntology.Concept>();
        for (Concept concept : concepts) {
            results.add(concept.toClientApi());
        }
        return results;
    }

    public abstract void setProperty(String name, Object value, Authorizations authorizations);

    public abstract void removeProperty(String name, Authorizations authorizations);

    public abstract byte[] getGlyphIcon();

    public abstract byte[] getMapGlyphIcon();

    public abstract String getIRI();
}
