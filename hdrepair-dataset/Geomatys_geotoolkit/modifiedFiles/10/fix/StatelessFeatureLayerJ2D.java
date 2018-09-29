/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2004 - 2008, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2008 - 2009, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotoolkit.display2d.container.stateless;

import java.awt.RenderingHints;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.geotoolkit.storage.DataStoreException;
import org.geotoolkit.data.DataStoreRuntimeException;
import org.geotoolkit.display.canvas.ReferencedCanvas2D;
import org.geotoolkit.display.canvas.VisitFilter;
import org.geotoolkit.display.canvas.control.CanvasMonitor;
import org.geotoolkit.display2d.container.ContextContainer2D;
import org.geotoolkit.display.exception.PortrayalException;
import org.geotoolkit.display2d.primitive.DefaultGraphicFeatureJ2D;
import org.geotoolkit.display2d.primitive.GraphicJ2D;
import org.geotoolkit.display.canvas.RenderingContext;
import org.geotoolkit.display.primitive.SearchArea;
import org.geotoolkit.display2d.canvas.RenderingContext2D;
import org.geotoolkit.display2d.container.statefull.StatefullContextParams;
import org.geotoolkit.display2d.container.statefull.StatefullProjectedFeature;
import org.geotoolkit.display2d.style.CachedRule;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.display2d.primitive.DefaultSearchAreaJ2D;
import org.geotoolkit.display2d.primitive.SearchAreaJ2D;
import org.geotoolkit.geometry.Envelope2D;
import org.geotoolkit.geometry.GeneralEnvelope;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.GraphicBuilder;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.geometry.DefaultBoundingBox;
import org.geotoolkit.style.MutableRule;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureIterator;
import org.geotoolkit.data.query.Query;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.display2d.GO2Hints;
import org.geotoolkit.display2d.container.statefull.StatefullCachedRule;
import org.geotoolkit.display2d.primitive.ProjectedFeature;
import org.geotoolkit.display2d.style.renderer.SymbolizerRenderer;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.factory.HintsPending;
import org.geotoolkit.filter.FilterUtilities;
import org.geotoolkit.referencing.operation.transform.AffineTransform2D;
import org.geotoolkit.style.StyleUtilities;
import org.geotoolkit.geometry.jts.transform.CoordinateSequenceMathTransformer;

import org.opengis.display.primitive.Graphic;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.style.Rule;
import org.opengis.style.Style;
import org.opengis.style.Symbolizer;

import static org.geotoolkit.display2d.GO2Utilities.*;

/**
 * Single object to represent a complete mapcontext.
 * This is a Stateless graphic object.
 *
 * @author johann sorel (Geomatys)
 * @module pending
 */
public class StatelessFeatureLayerJ2D extends AbstractLayerJ2D<FeatureMapLayer>{
    
    public StatelessFeatureLayerJ2D(ReferencedCanvas2D canvas, FeatureMapLayer layer){
        super(canvas, layer);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void paint(final RenderingContext2D renderingContext) {
        
        //we abort painting if the layer is not visible.
        if (!layer.isVisible()) return;  

        //search for a special graphic renderer
        final GraphicBuilder<GraphicJ2D> builder = (GraphicBuilder<GraphicJ2D>) layer.getGraphicBuilder(GraphicJ2D.class);
        if(builder != null){
            //there is a special graphic builder, no need to check anything, the custom
            // graphic will take care of it
            paintVectorLayer(builder, renderingContext);
        }

        final FeatureType sft = layer.getCollection().getFeatureType();
        final CachedRule[] rules;

        final Style style;
        final double opacity = layer.getOpacity();
        if(opacity == 1){
            style = layer.getStyle();
        }else if(opacity == 0){
            //no need to paint
            return;
        }else{
            style = StyleUtilities.copy(layer.getStyle(),opacity);
        }

        final Filter selectionFilter = layer.getSelectionFilter();
        if(selectionFilter != null && !Filter.EXCLUDE.equals(selectionFilter)){
            //merge the style and filter with the selection
            final List<Rule> selectionRules;
            final List<Rule> normalRules = GO2Utilities.getValidRules(
                   style, renderingContext.getGeographicScale(), sft);

            final List<CachedRule> mixedRules = new ArrayList<CachedRule>();
            final MutableStyle selectionStyle = layer.getSelectionStyle();
            if(selectionStyle == null){
                selectionRules = GO2Utilities.getValidRules(
                        ContextContainer2D.DEFAULT_SELECTION_STYLE, renderingContext.getGeographicScale(), sft);
            }else{
                selectionRules = GO2Utilities.getValidRules(
                        selectionStyle, renderingContext.getScale(), sft);
            }

            //update the rules filters
            for(final Rule rule : selectionRules){
                final List<? extends Symbolizer> symbols = rule.symbolizers();
                final MutableRule mixedRule = STYLE_FACTORY.rule(symbols.toArray(new Symbolizer[symbols.size()]));
                Filter f = rule.getFilter();
                if(f == null){
                    f = selectionFilter;
                }else{
                    f = FILTER_FACTORY.and(f,selectionFilter);
                }
                mixedRule.setFilter(f);
                mixedRules.add(GO2Utilities.getCached(mixedRule));
            }

            final Filter notSelectionFilter = FILTER_FACTORY.not(selectionFilter);

            for(final Rule rule : normalRules){
                final MutableRule mixedRule = STYLE_FACTORY.rule(rule.symbolizers().toArray(new Symbolizer[0]));
                Filter f = rule.getFilter();
                if(f == null){
                    f = notSelectionFilter;
                }else{
                    f = FILTER_FACTORY.and(f,notSelectionFilter);
                }
                mixedRule.setFilter(f);
                mixedRules.add(GO2Utilities.getCached(mixedRule));
            }

            rules = mixedRules.toArray(new CachedRule[mixedRules.size()]);

        }else{
            rules = GO2Utilities.getValidCachedRules(
                style, renderingContext.getGeographicScale(), sft);
        }

        //we perform a first check on the style to see if there is at least
        //one valid rule at this scale, if not we just continue.
        if (rules.length == 0) {
            return;
        }

        paintVectorLayer(rules, renderingContext);
    }

    protected void paintVectorLayer(GraphicBuilder builder, final RenderingContext2D renderingContext){
        final Collection<GraphicJ2D> graphics = builder.createGraphics(layer, canvas);
        for(GraphicJ2D gra : graphics){
            gra.paint(renderingContext);
        }
    }

    protected void paintVectorLayer(final CachedRule[] rules, final RenderingContext2D context) {
        
        final CanvasMonitor monitor = context.getMonitor();
        final Query query = prepareQuery(context, layer, rules);

        if(monitor.stopRequested()) return;

        //we do not check if the collection is empty or not since
        //it can be a very expensive operation
        
        //prepare the rendering parameters
        final StatefullContextParams params = prepareContextParams(context, null);

        final FeatureCollection<Feature> features;
        try{
            features = ((FeatureCollection<Feature>)layer.getCollection()).subCollection(query);
        }catch(DataStoreException ex){
            context.getMonitor().exceptionOccured(ex, Level.WARNING);
            //can not continue this layer with this error
            return;
        }

        //prepare the renderers
        final StatefullCachedRule preparedRenderers = new StatefullCachedRule(rules, context);

        final Boolean SymbolOrder = (Boolean) canvas.getRenderingHint(GO2Hints.KEY_SYMBOL_RENDERING_ORDER);
        if(SymbolOrder == null || SymbolOrder == false){
            try{
                renderByFeatureOrder(features, context, preparedRenderers, params);
            }catch(PortrayalException ex){
                monitor.exceptionOccured(ex, Level.WARNING);
            }
        }else{
            try{
                renderBySymbolOrder(features, context, preparedRenderers, params);
            }catch(PortrayalException ex){
                monitor.exceptionOccured(ex, Level.WARNING);
            }
        }
  
    }

    protected RenderingIterator getIterator(FeatureCollection<? extends Feature> features,
            RenderingContext2D renderingContext, StatefullContextParams params){
        final Hints iteHints = new Hints(HintsPending.FEATURE_DETACHED, Boolean.FALSE);
        final FeatureIterator<? extends Feature> iterator = features.iterator(iteHints);
        final StatefullProjectedFeature projectedFeature = new StatefullProjectedFeature(params);
        return new GraphicIterator(iterator, projectedFeature);
    }

    /**
     * Render by feature order.
     * @param features 
     * @param renderers
     * @param context 
     * @param params 
     * @throws PortrayalException
     */
    protected final void renderByFeatureOrder(FeatureCollection<? extends Feature> features,
            RenderingContext2D context, StatefullCachedRule renderers, StatefullContextParams params)
            throws PortrayalException{
        final CanvasMonitor monitor = context.getMonitor();
        final RenderingIterator statefullIterator = getIterator(features, context, params);

        try{
            //performance routine, only one symbol to render
            if(renderers.rules.length == 1
               && (renderers.rules[0].getFilter() == null || renderers.rules[0].getFilter() == Filter.INCLUDE)
               && renderers.rules[0].symbolizers().length == 1){
                renderers.renderers[0][0].portray(statefullIterator);
                return;
            }

            while(statefullIterator.hasNext()){
                if(monitor.stopRequested()) return;
                final ProjectedFeature projectedFeature = statefullIterator.next();

                boolean painted = false;
                for(int i=0; i<renderers.elseRuleIndex; i++){
                    final CachedRule rule = renderers.rules[i];
                    final Filter ruleFilter = rule.getFilter();
                    //test if the rule is valid for this feature
                    if (ruleFilter == null || ruleFilter.evaluate(projectedFeature.getFeature())) {
                        painted = true;
                        for (final SymbolizerRenderer renderer : renderers.renderers[i]) {
                            renderer.portray(projectedFeature);
                        }
                    }
                }

                //the feature hasn't been painted, paint it with the 'else' rules
                if(!painted){
                    for(int i=renderers.elseRuleIndex; i<renderers.rules.length; i++){
                        final CachedRule rule = renderers.rules[i];
                        final Filter ruleFilter = rule.getFilter();
                        //test if the rule is valid for this feature
                        if (ruleFilter == null || ruleFilter.evaluate(projectedFeature.getFeature())) {
                            for (final SymbolizerRenderer renderer : renderers.renderers[i]) {
                                renderer.portray(projectedFeature);
                            }
                        }
                    }
                }
            }
        }finally{
            try {
                statefullIterator.close();
            } catch (IOException ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
        }
    }

    /**
     * render by symbol order.
     */
    protected final void renderBySymbolOrder(FeatureCollection<? extends Feature> features,
            RenderingContext2D context, StatefullCachedRule renderers, StatefullContextParams params)
            throws PortrayalException {
        final CanvasMonitor monitor = context.getMonitor();

        //performance routine, only one symbol to render
        if(renderers.rules.length == 1
           && (renderers.rules[0].getFilter() == null || renderers.rules[0].getFilter() == Filter.INCLUDE)
           && renderers.rules[0].symbolizers().length == 1){
            final RenderingIterator statefullIterator = getIterator(features, context, params);
            renderers.renderers[0][0].portray(statefullIterator);
            try {
                statefullIterator.close();
            } catch (IOException ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
            return;
        }


        //store the ids of the features painted
        final Set<String> painted = new HashSet<String>();
        //render the main rules ------------------------------------------------
        for (int i = 0; i < renderers.elseRuleIndex; i++) {
            if(monitor.stopRequested()) return;
            final CachedRule rule = renderers.rules[i];
            final Filter rulefilter = rule.getFilter();

            for (final SymbolizerRenderer renderer : renderers.renderers[i]) {
                if(monitor.stopRequested()) return;
                final RenderingIterator ite = getIterator(features, context, params);
                try {
                    while (ite.hasNext()) {
                        if(monitor.stopRequested()) return;
                        final ProjectedFeature pf = ite.next();
                        final Feature f = pf.getFeature();
                        if (rulefilter == null || rulefilter.evaluate(pf.getFeature())) {
                            painted.add(f.getIdentifier().getID());
                            renderer.portray(pf);
                        }
                    }
                } finally {
                    try {
                        ite.close();
                    } catch (IOException ex) {
                        getLogger().log(Level.WARNING, null, ex);
                    }
                }
            }
        }

        //render the else rules ------------------------------------------------
        for (int i = renderers.elseRuleIndex; i < renderers.rules.length; i++) {
            if(monitor.stopRequested()) return;
            final CachedRule rule = renderers.rules[i];
            final Filter rulefilter = rule.getFilter();

            for (final SymbolizerRenderer renderer : renderers.renderers[i]) {
                if(monitor.stopRequested()) return;
                final RenderingIterator ite = getIterator(features, context, params);
                try {
                    while (ite.hasNext()) {
                        if(monitor.stopRequested()) return;
                        final ProjectedFeature pf = ite.next();
                        final Feature f = pf.getFeature();
                        if (!painted.contains(f.getIdentifier().getID())
                            && (rulefilter == null || rulefilter.evaluate(pf.getFeature()))) {
                            renderer.portray(pf);
                        }
                    }
                } finally {
                    try {
                        ite.close();
                    } catch (IOException ex) {
                        getLogger().log(Level.WARNING, null, ex);
                    }
                }
            }
        }

    }


    protected List<Graphic> searchGraphicAt(final FeatureMapLayer layer, final CachedRule[] rules,
            final RenderingContext2D renderingContext, final SearchAreaJ2D mask, VisitFilter visitFilter, List<Graphic> graphics) {

        final Query query = prepareQuery(renderingContext, layer, rules);

        final FeatureCollection<Feature> features;
        try{
            features = ((FeatureCollection<Feature>)layer.getCollection()).subCollection(query);
        }catch(DataStoreException ex){
            renderingContext.getMonitor().exceptionOccured(ex, Level.WARNING);
            //can not continue this layer with this error
            return graphics;
        }

        //we do not check if the collection is empty or not since
        //it can be a very expensive operation

        final StatefullContextParams params = prepareContextParams(renderingContext, null);


        // iterate and find the first graphic that hit the given point
        final FeatureIterator<Feature> iterator;
        try{
            iterator = features.iterator();
        }catch(DataStoreRuntimeException ex){
            renderingContext.getMonitor().exceptionOccured(ex, Level.WARNING);
            return graphics;
        }

        //prepare the renderers
        final StatefullCachedRule preparedRenderers = new StatefullCachedRule(rules, renderingContext);

        final StatefullProjectedFeature projectedFeature = new StatefullProjectedFeature(params);
        try{
            while(iterator.hasNext()){
                Feature feature = iterator.next();
                projectedFeature.setFeature(feature);

                boolean painted = false;
                for(int i=0;i<preparedRenderers.elseRuleIndex;i++){
                    final CachedRule rule = preparedRenderers.rules[i];
                    final Filter ruleFilter = rule.getFilter();
                    //test if the rule is valid for this feature
                    if (ruleFilter == null || ruleFilter.evaluate(feature)) {
                        painted = true;
                        for(SymbolizerRenderer renderer : preparedRenderers.renderers[i]){
                            if(renderer.hit(projectedFeature, mask, visitFilter)){
                                if(feature != null) graphics.add( new DefaultGraphicFeatureJ2D(getCanvas(), layer, feature) );
                                break;
                            }
                        }
                    }
                }

                //the feature hasn't been painted, paint it with the 'else' rules
                if(!painted){
                    for(int i=preparedRenderers.elseRuleIndex; i<preparedRenderers.rules.length; i++){
                        final CachedRule rule = preparedRenderers.rules[i];
                        final Filter ruleFilter = rule.getFilter();
                        //test if the rule is valid for this feature
                        if (ruleFilter == null || ruleFilter.evaluate(feature)) {
                            for(SymbolizerRenderer renderer : preparedRenderers.renderers[i]){
                                if(renderer.hit(projectedFeature, mask, visitFilter)){
                                    if(feature != null) graphics.add( new DefaultGraphicFeatureJ2D(getCanvas(), layer, feature) );
                                    break;
                                }
                            }
                        }
                    }
                }
                
            }
        }finally{
            iterator.close();
        }

        return graphics;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<Graphic> getGraphicAt(RenderingContext rdcontext, SearchArea mask, VisitFilter filter, List<Graphic> graphics) {
        
        if(!layer.isSelectable()) return graphics;

        if(!(rdcontext instanceof RenderingContext2D)) return graphics;
        final RenderingContext2D c2d = (RenderingContext2D) rdcontext;

        //nothing visible so no possible selection
        if (!layer.isVisible()) return graphics;

        final Name featureTypeName = layer.getCollection().getFeatureType().getName();
        final CachedRule[] rules = GO2Utilities.getValidCachedRules(layer.getStyle(), c2d.getGeographicScale(), featureTypeName);

        //we perform a first check on the style to see if there is at least
        //one valid rule at this scale, if not we just return null.
        if (rules.length == 0) {
            return graphics;
        }

        if(graphics == null) graphics = new ArrayList<Graphic>();

        if(mask instanceof SearchAreaJ2D){
            return searchGraphicAt(layer, rules, c2d, (SearchAreaJ2D)mask, filter, graphics);
        }else{
            return searchGraphicAt(layer, rules, c2d, new DefaultSearchAreaJ2D(mask), filter, graphics);
        }
    }

    /**
     * Creates an optimal query to send to the datastore, knowing which properties are knowned and
     * the appropriate bounding box to filter.
     */
    protected static Query prepareQuery(RenderingContext2D renderingContext, FeatureMapLayer layer, CachedRule[] rules){

        final FeatureCollection<? extends Feature> fs            = layer.getCollection();
        final FeatureType schema                                 = fs.getFeatureType();
        final GeometryDescriptor geomDesc                        = schema.getGeometryDescriptor();
        BoundingBox bbox                                         = renderingContext.getPaintingObjectiveBounds2D();
        final CoordinateReferenceSystem bboxCRS                  = bbox.getCoordinateReferenceSystem();
        final CanvasMonitor monitor                              = renderingContext.getMonitor();
        final CoordinateReferenceSystem layerCRS                 = schema.getCoordinateReferenceSystem();
        final String geomAttName                                 = (geomDesc!=null)? geomDesc.getLocalName() : null;
        final RenderingHints hints                               = renderingContext.getRenderingHints();

        if( !CRS.equalsIgnoreMetadata(layerCRS,bboxCRS)){
            //BBox and layer bounds have different CRS. reproject bbox bounds
            Envelope env;

            try{
                env = CRS.transform(bbox, layerCRS);
            }catch(TransformException ex){
                //TODO is fixed in geotidy, the result envelope will have infinte values where needed
                //TODO should do something about this, since canvas bounds may be over the crs bounds
                monitor.exceptionOccured(ex, Level.WARNING);
                env = new Envelope2D();
            }catch(IllegalArgumentException ex){
                //looks like the coordinate of the bbox are outside of the crs valide area.
                //some crs raise this error, other not.
                //if so we should reduce our bbox to the valide extent of the crs.
                monitor.exceptionOccured(ex, Level.WARNING);

                final GeographicBoundingBox gbox = CRS.getGeographicBoundingBox(layerCRS);

                if(gbox == null){
                    env = new GeneralEnvelope(layerCRS);
                }else{
                    env = new GeneralEnvelope(gbox);
                }

            }catch(Exception ex){
                //we should not catch this but we must not block the canvas
                monitor.exceptionOccured(ex, Level.WARNING);
                return null;
            }

            //TODO looks like the envelope after transform operation doesnt have always exactly the same CRS.
            //fix CRS classes method and remove the two next lines.
            env = new GeneralEnvelope(env);
            ((GeneralEnvelope)env).setCoordinateReferenceSystem(layerCRS);

            bbox = new DefaultBoundingBox(env);
        }

        Filter filter;

        //final Envelope layerBounds = layer.getBounds();
        //we better not do any call to the layer bounding box before since it can be
        //really expensive, the datastore is the best placed to check if he might
        //optimize the filter.
        //if( ((BoundingBox)bbox).contains(new DefaultBoundingBox(layerBounds))){
            //the layer bounds overlaps the bbox, no need for a spatial filter
        //   filter = Filter.INCLUDE;
        //}else{
        //make a bbox filter
        if(geomAttName != null){
            filter = FILTER_FACTORY.bbox(FILTER_FACTORY.property(geomAttName),bbox);
        }else{
            filter = Filter.EXCLUDE;
        }
        //}

        //concatenate geographique filter with data filter if there is one
        if(layer.getQuery() != null && layer.getQuery().getFilter() != null){
            filter = FILTER_FACTORY.and(filter,layer.getQuery().getFilter());
        }


        //concatenate with temporal range if needed ----------------------------
        final Filter temporalFilter;
        final Date[] temporal = renderingContext.getTemporalRange().clone();
        final Expression[] layerTemporalRange = layer.getTemporalRange().clone();

        if(temporal[0] == null){
            temporal[0] = new Date(Long.MIN_VALUE);
        }
        if(temporal[1] == null){
            temporal[1] = new Date(Long.MAX_VALUE);
        }

        if(layerTemporalRange[0] != null && layerTemporalRange[1] != null){
            temporalFilter = FILTER_FACTORY.and(
                    FILTER_FACTORY.lessOrEqual(layerTemporalRange[0], FILTER_FACTORY.literal(temporal[1])),
                    FILTER_FACTORY.greaterOrEqual(layerTemporalRange[1], FILTER_FACTORY.literal(temporal[0])));
        }else if(layerTemporalRange[0] != null){
            temporalFilter = FILTER_FACTORY.and(
                    FILTER_FACTORY.lessOrEqual(layerTemporalRange[0], FILTER_FACTORY.literal(temporal[1])),
                    FILTER_FACTORY.greaterOrEqual(layerTemporalRange[0], FILTER_FACTORY.literal(temporal[0])));
        }else if(layerTemporalRange[1] != null){
            temporalFilter = FILTER_FACTORY.and(
                    FILTER_FACTORY.lessOrEqual(layerTemporalRange[1], FILTER_FACTORY.literal(temporal[1])),
                    FILTER_FACTORY.greaterOrEqual(layerTemporalRange[1], FILTER_FACTORY.literal(temporal[0])));
        }else{
            temporalFilter = Filter.INCLUDE;
        }

        if(temporalFilter != Filter.INCLUDE){
            filter = FILTER_FACTORY.and(filter,temporalFilter);
        }

        //concatenate with elevation range if needed ---------------------------
        final Filter verticalFilter;
        final Double[] vertical = renderingContext.getElevationRange().clone();
        final Expression[] layerVerticalRange = layer.getElevationRange().clone();

        if(vertical[0] == null){
            vertical[0] = Double.NEGATIVE_INFINITY;
        }
        if(vertical[1] == null){
            vertical[1] = Double.POSITIVE_INFINITY;
        }

        if(layerVerticalRange[0] != null && layerVerticalRange[1] != null){
            verticalFilter = FILTER_FACTORY.and(
                    FILTER_FACTORY.lessOrEqual(layerVerticalRange[0], FILTER_FACTORY.literal(vertical[1])),
                    FILTER_FACTORY.greaterOrEqual(layerVerticalRange[1], FILTER_FACTORY.literal(vertical[0])));
        }else if(layerVerticalRange[0] != null){
            verticalFilter = FILTER_FACTORY.and(
                    FILTER_FACTORY.lessOrEqual(layerVerticalRange[0], FILTER_FACTORY.literal(vertical[1])),
                    FILTER_FACTORY.greaterOrEqual(layerVerticalRange[0], FILTER_FACTORY.literal(vertical[0])));
        }else if(layerVerticalRange[1] != null){
            verticalFilter = FILTER_FACTORY.and(
                    FILTER_FACTORY.lessOrEqual(layerVerticalRange[1], FILTER_FACTORY.literal(vertical[1])),
                    FILTER_FACTORY.greaterOrEqual(layerVerticalRange[1], FILTER_FACTORY.literal(vertical[0])));
        }else{
            verticalFilter = Filter.INCLUDE;
        }

        if(verticalFilter != Filter.INCLUDE){
            filter = FILTER_FACTORY.and(filter,verticalFilter);
        }


        final Set<String> attributs = GO2Utilities.propertiesCachedNames(rules);
        final Set<String> copy = new HashSet<String>(attributs);
        if(geomAttName != null){
            copy.add(geomAttName);
        }
        final String[] atts = copy.toArray(new String[copy.size()]);

        //check that properties names does not hold sub properties values, if one is found
        //then we reduce it to the first parent property.
        for(int i=0; i<atts.length; i++){
            String attName = atts[i];
            int index = attName.indexOf('/');
            if(index >=0){
                //remove all xpath filtering and indexing
                int n = attName.indexOf('[', index+1);
                while(n > 0){
                    int d = attName.indexOf(']', n);
                    if(d>0){
                        attName = attName.substring(index, n)+attName.substring(d+1);
                    }else{
                        break;
                    }
                    n = attName.indexOf('[', index+1);
                }

                //looks like we have a path
                int nextOcc = attName.indexOf('/', index+1);
                int startBracket = attName.indexOf('{', index+1);
                int endBracket = attName.indexOf('}', index+1);
                if(nextOcc> startBracket && nextOcc<endBracket){
                    //in a qname, ignore this slash
                    nextOcc = attName.indexOf('/', endBracket+1);
                }

                if(endBracket > index){
                    //this is a path, reduce it
                    atts[i] = attName.substring(index+1, nextOcc);
                }
            }
        }

        //optimize the filter---------------------------------------------------
        filter = FilterUtilities.prepare(filter,Feature.class);

        final QueryBuilder qb = new QueryBuilder();
        qb.setTypeName(schema.getName());
        qb.setFilter(filter);
        qb.setProperties(atts);

        //add resampling -------------------------------------------------------
        Boolean resample = (hints == null) ? null : (Boolean) hints.get(GO2Hints.KEY_GENERALIZE);
        if(!Boolean.FALSE.equals(resample)){
            //we only disable resampling if it is explictly specified
            final double[] res = renderingContext.getResolution(layerCRS);

            //adjust with the generalization factor
            final Number n =  (hints==null) ? null : (Number)hints.get(GO2Hints.KEY_GENERALIZE_FACTOR);
            final double factor;
            if(n != null){
                factor = n.doubleValue();
            }else{
                factor = GO2Hints.GENERALIZE_FACTOR_DEFAULT.doubleValue();
            }
            res[0] *= factor;
            res[1] *= factor;
            qb.setResolution(renderingContext.getResolution(layerCRS));
        }

        //add reprojection -----------------------------------------------------
        qb.setCRS(renderingContext.getObjectiveCRS2D());
        
        return qb.buildQuery();
    }

    private StatefullContextParams prepareContextParams(RenderingContext2D renderingContext,
            StatefullContextParams params){
        final CoordinateReferenceSystem displayCRS   = renderingContext.getDisplayCRS();
        final AffineTransform2D objtoDisp              = renderingContext.getObjectiveToDisplay();

        if(params == null){
            params = new StatefullContextParams(getCanvas(),layer);
        }

        params.displayCRS = displayCRS;
        params.objectiveToDisplay.setTransform(objtoDisp);
        ((CoordinateSequenceMathTransformer)params.objToDisplayTransformer.getCSTransformer())
                .setTransform(objtoDisp);
        
        return params;
    }

    protected static interface RenderingIterator extends Iterator<ProjectedFeature>,Closeable{}

    protected static class GraphicIterator implements RenderingIterator{

        private final FeatureIterator<? extends Feature> ite;
        private final StatefullProjectedFeature projected;

        public GraphicIterator(FeatureIterator<? extends Feature> ite, StatefullProjectedFeature projected) {
            this.ite = ite;
            this.projected = projected;
        }

        @Override
        public boolean hasNext() {
            return ite.hasNext();
        }

        @Override
        public ProjectedFeature next() {
            projected.setFeature(ite.next());
            return projected;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public void close() throws IOException {
            ite.close();
        }

    }

}
