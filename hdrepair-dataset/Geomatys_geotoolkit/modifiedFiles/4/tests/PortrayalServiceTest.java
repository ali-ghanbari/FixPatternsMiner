/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010-2014, Geomatys
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
package org.geotoolkit.display2d.service;

import org.geotoolkit.coverage.io.CoverageStoreException;
import org.geotoolkit.style.StyleConstants;
import org.geotoolkit.map.CoverageMapLayer;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;
import org.apache.sis.geometry.GeneralEnvelope;

import org.geotoolkit.coverage.CoverageStack;
import org.geotoolkit.coverage.grid.GridCoverage2D;
import org.geotoolkit.coverage.grid.GridCoverageBuilder;
import org.geotoolkit.coverage.io.GridCoverageReader;
import org.geotoolkit.data.FeatureStoreUtilities;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureWriter;
import org.geotoolkit.display.canvas.control.StopOnErrorMonitor;
import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display2d.GO2Hints;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.geotoolkit.feature.FeatureUtilities;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.referencing.ReferencingUtilities;
import org.apache.sis.referencing.crs.DefaultCompoundCRS;
import org.geotoolkit.style.DefaultStyleFactory;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.style.MutableStyleFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import org.opengis.coverage.Coverage;
import org.opengis.coverage.grid.GridGeometry;
import org.geotoolkit.feature.simple.SimpleFeature;
import org.geotoolkit.feature.simple.SimpleFeatureType;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import static org.junit.Assert.*;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.FeatureType;
import org.opengis.filter.expression.Expression;
import org.opengis.style.ChannelSelection;
import org.opengis.style.ColorMap;
import org.opengis.style.ContrastEnhancement;
import org.opengis.style.Description;
import org.opengis.style.OverlapBehavior;
import org.opengis.style.RasterSymbolizer;
import org.opengis.style.ShadedRelief;
import org.opengis.style.Symbolizer;

import static org.geotoolkit.style.StyleConstants.*;
import org.apache.sis.referencing.CommonCRS;
import org.geotoolkit.factory.FactoryFinder;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.map.FeatureMapLayer;
import org.opengis.filter.FilterFactory;
import org.opengis.style.Fill;
import org.opengis.style.Graphic;
import org.opengis.style.GraphicalSymbol;
import org.opengis.style.Mark;
import org.opengis.style.PointSymbolizer;
import org.opengis.style.Stroke;

/**
 * Testing portrayal service.
 *
 * @author Johann Sorel (Geomatys)
 */
public class PortrayalServiceTest {

    private static final double EPS = 0.000000001d;

    private static final FilterFactory FF = FactoryFinder.getFilterFactory(null);
    private static final GeometryFactory GF = new GeometryFactory();
    private static final GridCoverageBuilder GCF = new GridCoverageBuilder();
    private static final MutableStyleFactory SF = new DefaultStyleFactory();

    private final List<FeatureCollection> featureColls = new ArrayList<FeatureCollection>();
    private final List<GridCoverage2D> coverages = new ArrayList<GridCoverage2D>();
    private final List<Envelope> envelopes = new ArrayList<Envelope>();
    private final List<Date[]> dates = new ArrayList<Date[]>();
    private final List<Double[]> elevations = new ArrayList<Double[]>();

    private final Coverage coverage4D;

    public PortrayalServiceTest() throws Exception {

        // create the feature collection for tests -----------------------------
        final FeatureTypeBuilder sftb = new FeatureTypeBuilder();
        sftb.setName("test");
        sftb.add("geom", Point.class, CommonCRS.WGS84.normalizedGeographic());
        sftb.add("att1", String.class);
        sftb.add("att2", Double.class);
        final SimpleFeatureType sft = sftb.buildSimpleFeatureType();
        FeatureCollection col = FeatureStoreUtilities.collection("id", sft);

        final FeatureWriter writer = col.getSession().getFeatureStore().getFeatureWriterAppend(sft.getName());

        SimpleFeature sf = (SimpleFeature) writer.next();
        sf.setAttribute("geom", GF.createPoint(new Coordinate(0, 0)));
        sf.setAttribute("att1", "value1");
        writer.write();
        sf = (SimpleFeature) writer.next();
        sf.setAttribute("geom", GF.createPoint(new Coordinate(-180, -90)));
        sf.setAttribute("att1", "value1");
        writer.write();
        sf = (SimpleFeature) writer.next();
        sf.setAttribute("geom", GF.createPoint(new Coordinate(-180, 90)));
        sf.setAttribute("att1", "value1");
        writer.write();
        sf = (SimpleFeature) writer.next();
        sf.setAttribute("geom", GF.createPoint(new Coordinate(180, -90)));
        sf.setAttribute("att1", "value1");
        writer.write();
        sf = (SimpleFeature) writer.next();
        sf.setAttribute("geom", GF.createPoint(new Coordinate(180, -90)));
        sf.setAttribute("att1", "value1");
        writer.write();

        writer.close();

        featureColls.add(col);


        //create a serie of envelopes for tests --------------------------------
        GeneralEnvelope env = new GeneralEnvelope(CRS.decode("EPSG:4326"));
        env.setRange(0, -90, 90);
        env.setRange(1, -180, 180);
        envelopes.add(env);
        env = new GeneralEnvelope(CRS.decode("EPSG:4326"));
        env.setRange(0, -12, 31);
        env.setRange(1, -5, 46);
        envelopes.add(env);
        env = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
        env.setRange(0, -180, 180);
        env.setRange(1, -90, 90);
        envelopes.add(env);
        env = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
        env.setRange(0, -5, 46);
        env.setRange(1, -12, 31);
        envelopes.add(env);
        env = new GeneralEnvelope(CRS.decode("EPSG:3395"));
        env.setRange(0, -1200000, 3100000);
        env.setRange(1, -500000, 4600000);
        envelopes.add(env);

        //create a serie of date ranges ----------------------------------------
        dates.add(new Date[]{new Date(1000),new Date(15000)});
        dates.add(new Date[]{null,          new Date(15000)});
        dates.add(new Date[]{new Date(1000),null});
        dates.add(new Date[]{null,          null});

        //create a serie of elevation ranges -----------------------------------
        elevations.add(new Double[]{-15d,   50d});
        elevations.add(new Double[]{null,   50d});
        elevations.add(new Double[]{-15d,   null});
        elevations.add(new Double[]{null,   null});


        //create some coverages ------------------------------------------------

        env = new GeneralEnvelope(CRS.decode("EPSG:32738"));
        env.setRange(0,  695035,  795035);
        env.setRange(1, 7545535, 7645535);
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);
        g.fill(new Rectangle(0, 0, 100, 100));
        GCF.reset();
        GCF.setEnvelope(env);
        GCF.setRenderedImage(img);
        GridCoverage2D coverage = GCF.getGridCoverage2D();
        coverages.add(coverage);

        env = new GeneralEnvelope(CRS.decode("EPSG:4326"));
        env.setRange(0,  -10,  25);
        env.setRange(1, -56, -21);
        img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        g = img.createGraphics();
        g.setColor(Color.RED);
        g.fill(new Rectangle(0, 0, 100, 100));
        GCF.reset();
        GCF.setEnvelope(env);
        GCF.setRenderedImage(img);
        coverage = GCF.getGridCoverage2D();
        coverages.add(coverage);

        //create some ND coverages ---------------------------------------------
        CoordinateReferenceSystem crs = new DefaultCompoundCRS(
                    Collections.singletonMap(DefaultCompoundCRS.NAME_KEY, "4D crs"),
                    CRS.decode("EPSG:4326"),
                    CommonCRS.Vertical.ELLIPSOIDAL.crs(),
                    CommonCRS.Temporal.JAVA.crs());

        List<Coverage> temps = new ArrayList<Coverage>();
        for(int i=0; i<10; i++){
            final List<Coverage> eles = new ArrayList<Coverage>();
            for(int k=0;k<10;k++){
                env = new GeneralEnvelope(crs);
                env.setRange(0,  0,  10);
                env.setRange(1, 0, 10);
                env.setRange(2, k, k+1);
                env.setRange(3, i, i+1);
                img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
                GCF.reset();
                GCF.setEnvelope(env);
                GCF.setRenderedImage(img);
                coverage = GCF.getGridCoverage2D();
                eles.add(coverage);
            }
            temps.add(new CoverageStack("3D", eles));
        }
        coverage4D = new CoverageStack("4D", coverages);
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Test
    public void testEnvelopeNotNull() throws NoSuchAuthorityCodeException, FactoryException, PortrayalException {
        MapContext context = MapBuilder.createContext(CRS.decode("EPSG:4326"));
        GeneralEnvelope env = new GeneralEnvelope(CRS.decode("EPSG:4326"));
        env.setRange(0, -180, 180);
        env.setRange(1, -90, 90);

        DefaultPortrayalService.portray(
                new CanvasDef(new Dimension(800, 600), null),
                new SceneDef(context),
                new ViewDef(env));



        //CRS can not obtain envelope for this projection. we check that we don't reaise any error.
        context = MapBuilder.createContext(CRS.decode("CRS:84"));
        env = new GeneralEnvelope(CRS.decode("CRS:84"));
        env.setRange(0, -180, 180);
        env.setRange(1, -90, 90);

        DefaultPortrayalService.portray(
                new CanvasDef(new Dimension(800, 600), null),
                new SceneDef(context),
                new ViewDef(env));


    }

    @Test
    public void testFeatureRendering() throws Exception{
        for(FeatureCollection col : featureColls){
            final MapLayer layer = MapBuilder.createFeatureLayer(col, SF.style(SF.pointSymbolizer()));
            testRendering(layer);
        }
    }

    /**
     * Test rendering of a coverage inside a feature property.
     */
    @Test
    public void testCoveragePropertyRendering() throws Exception{
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("test");
        ftb.add("coverage", GridCoverage2D.class, CommonCRS.WGS84.normalizedGeographic());
        final FeatureType ft = ftb.buildFeatureType();

        final BufferedImage img = new BufferedImage(90, 90, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = img.createGraphics();
        g.setColor(Color.GREEN);
        g.fillRect(0, 0, 90, 90);
        g.dispose();

        final GridCoverageBuilder gcb = new GridCoverageBuilder();
        gcb.setName("propcov");
        gcb.setCoordinateReferenceSystem(CommonCRS.WGS84.normalizedGeographic());
        gcb.setGridToCRS(1, 0, 0, 1, 0.5, 0.5);
        gcb.setRenderedImage(img);

        final Feature f = FeatureUtilities.defaultFeature(ft, "id0");
        f.getProperty("coverage").setValue(gcb.getGridCoverage2D());
        final FeatureCollection collection = FeatureStoreUtilities.collection(f);


        final String name = "mySymbol";
        final Description desc = DEFAULT_DESCRIPTION;
        final String geometry = "coverage";
        final Unit unit = NonSI.PIXEL;
        final Expression opacity = LITERAL_ONE_FLOAT;
        final ChannelSelection channels = null;
        final OverlapBehavior overlap = null;
        final ColorMap colormap = null;
        final ContrastEnhancement enhance = null;
        final ShadedRelief relief = null;
        final Symbolizer outline = null;

        final RasterSymbolizer symbol = SF.rasterSymbolizer(
                name,geometry,desc,unit,opacity,
                channels,overlap,colormap,enhance,relief,outline);
        final MutableStyle style = SF.style(symbol);

        final MapLayer layer = MapBuilder.createFeatureLayer(collection, style);
        final MapContext context = MapBuilder.createContext();
        context.layers().add(layer);

        final CanvasDef cdef = new CanvasDef(new Dimension(360, 180), null);
        final SceneDef sdef = new SceneDef(context);
        final GeneralEnvelope env = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
        env.setRange(0, -180, +180);
        env.setRange(1, -90, +90);
        final ViewDef vdef = new ViewDef(env);

        final BufferedImage result = DefaultPortrayalService.portray(cdef, sdef, vdef);
        final Raster raster = result.getData();
        final int[] pixel = new int[4];
        final int[] trans = new int[]{0,0,0,0};
        final int[] green = new int[]{0,255,0,255};
        assertNotNull(result);
        raster.getPixel(0, 0, pixel);
        assertArrayEquals(trans, pixel);
        raster.getPixel(179, 45, pixel);
        assertArrayEquals(trans, pixel);
        raster.getPixel(181, 45, pixel);
        assertArrayEquals(green, pixel);
    }

    @Test
    @Ignore
    public void testCoverageRendering() throws Exception{
        for(GridCoverage2D col : coverages){
            final MapLayer layer = MapBuilder.createCoverageLayer(col, SF.style(SF.rasterSymbolizer()), "cov");
            testRendering(layer);
        }
    }

    @Test
    public void testCoverageNDRendering() throws Exception{
        //todo
    }

    @Test
    public void testLongitudeFirst() throws Exception{

        final int[] pixel = new int[4];
        final int[] red = new int[]{255,0,0,255};
        final int[] white = new int[]{255,255,255,255};

        final Hints hints = new Hints();
        hints.put(GO2Hints.KEY_COLOR_MODEL, ColorModel.getRGBdefault());




        //create a map context with a layer that will cover the entire area we will ask for
        final GeneralEnvelope covenv = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
        covenv.setRange(0, -180, 180);
        covenv.setRange(1, -90, 90);
        final BufferedImage img = new BufferedImage(360, 180, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);
        g.fill(new Rectangle(0, 0, 360, 180));
        final GridCoverageBuilder gcb = new GridCoverageBuilder();
        gcb.setEnvelope(covenv);
        gcb.setRenderedImage(img);
        final GridCoverage2D coverage = gcb.getGridCoverage2D();
        final MapLayer layer = MapBuilder.createCoverageLayer(coverage, SF.style(SF.rasterSymbolizer()), "");
        final MapContext context = MapBuilder.createContext();
        context.layers().add(layer);


        //sanity test, image should be a red vertical band in the middle
        final CoordinateReferenceSystem epsg4326 = CRS.decode("EPSG:4326");
        GeneralEnvelope env = new GeneralEnvelope(epsg4326);
        env.setRange(0, -180, 180);
        env.setRange(1, -180, 180);

        BufferedImage buffer = DefaultPortrayalService.portray(
                new CanvasDef(new Dimension(360, 360), Color.WHITE),
                new SceneDef(context, hints),
                new ViewDef(env));
        //ImageIO.write(buffer, "png", new File("sanity.png"));
        assertEquals(360,buffer.getWidth());
        assertEquals(360,buffer.getHeight());

        WritableRaster raster = buffer.getRaster();
        raster.getPixel(0, 0, pixel);       assertArrayEquals(white, pixel);
        raster.getPixel(359, 0, pixel);     assertArrayEquals(white, pixel);
        raster.getPixel(359, 359, pixel);   assertArrayEquals(white, pixel);
        raster.getPixel(0, 359, pixel);     assertArrayEquals(white, pixel);
        raster.getPixel(180, 0, pixel);     assertArrayEquals(red, pixel);
        raster.getPixel(180, 359, pixel);   assertArrayEquals(red, pixel);
        raster.getPixel(0, 180, pixel);     assertArrayEquals(white, pixel);
        raster.getPixel(359, 180, pixel);   assertArrayEquals(white, pixel);



        //east=horizontal test, image should be a red horizontal band in the middle
        buffer = DefaultPortrayalService.portray(
                new CanvasDef(new Dimension(360, 360), Color.WHITE),
                new SceneDef(context, hints),
                new ViewDef(env).setLongitudeFirst());
        //ImageIO.write(buffer, "png", new File("flip.png"));
        assertEquals(360,buffer.getWidth());
        assertEquals(360,buffer.getHeight());

        raster = buffer.getRaster();
        raster.getPixel(0, 0, pixel);       assertArrayEquals(white, pixel);
        raster.getPixel(359, 0, pixel);     assertArrayEquals(white, pixel);
        raster.getPixel(359, 359, pixel);   assertArrayEquals(white, pixel);
        raster.getPixel(0, 359, pixel);     assertArrayEquals(white, pixel);
        raster.getPixel(180, 0, pixel);     assertArrayEquals(white, pixel);
        raster.getPixel(180, 359, pixel);   assertArrayEquals(white, pixel);
        raster.getPixel(0, 180, pixel);     assertArrayEquals(red, pixel);
        raster.getPixel(359, 180, pixel);   assertArrayEquals(red, pixel);



    }

    /**
     * Test the CoverageReader view of a scene.
     */
    @Test
    public void testPortrayalCoverageReader() throws CoverageStoreException{

        //create a test coverage
        final BufferedImage img = new BufferedImage(360, 180, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.GREEN);
        g2d.fillRect(0, 0, 360, 180);
        final GeneralEnvelope env = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
        env.setRange(0, -180, 180);
        env.setRange(1, -90, 90);
        final GridCoverageBuilder gcb = new GridCoverageBuilder();
        gcb.setEnvelope(env);
        gcb.setRenderedImage(img);
        final GridCoverage2D coverage = gcb.getGridCoverage2D();

        //display it
        final MapContext context = MapBuilder.createContext();
        final CoverageMapLayer cl = MapBuilder.createCoverageLayer(
                coverage, SF.style(StyleConstants.DEFAULT_RASTER_SYMBOLIZER), "coverage");
        context.layers().add(cl);

        final SceneDef sceneDef = new SceneDef(context);
        final GridCoverageReader reader = DefaultPortrayalService.asCoverageReader(sceneDef);

        assertEquals(1, reader.getCoverageNames().size());

        final GridGeometry gridGeom = reader.getGridGeometry(0);
        assertNotNull(gridGeom);

        final GridCoverage2D result = (GridCoverage2D) reader.read(0, null);
        final RenderedImage image = result.getRenderedImage();
        assertEquals(1000, image.getWidth());

    }

    /**
     * Test that a large graphic outside the map area is still rendered.
     * 
     */
    @Test
    public void testMarginRendering() throws Exception{
        final List<GraphicalSymbol> symbols = new ArrayList<>();
        final Stroke stroke = SF.stroke(Color.BLACK, 0);
        final Fill fill = SF.fill(Color.BLACK);
        final Mark mark = SF.mark(MARK_CIRCLE, fill, stroke);
        symbols.add(mark);
        final Graphic graphic = SF.graphic(symbols, LITERAL_ONE_FLOAT, FF.literal(8), LITERAL_ONE_FLOAT, DEFAULT_ANCHOR_POINT, DEFAULT_DISPLACEMENT);
        final PointSymbolizer symbolizer = SF.pointSymbolizer("mySymbol",(String)null,DEFAULT_DESCRIPTION, NonSI.PIXEL, graphic);
        
        final CoordinateReferenceSystem crs = CommonCRS.WGS84.normalizedGeographic();
        
        
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("test");
        ftb.add("geom", Point.class,crs);
        final FeatureType ft = ftb.buildFeatureType();
        final Feature feature = FeatureUtilities.defaultFeature(ft, "0");
        final Point pt = GF.createPoint(new Coordinate(12, 5));
        JTS.setCRS(pt, crs);
        feature.setPropertyValue("geom", pt);
        
        final FeatureCollection col = FeatureStoreUtilities.collection(feature);        
        final FeatureMapLayer layer = MapBuilder.createFeatureLayer(col,SF.style(symbolizer));
        final MapContext context = MapBuilder.createContext();
        context.layers().add(layer);
        
        final GeneralEnvelope env = new GeneralEnvelope(crs);
        env.setRange(0, 0, 10);
        env.setRange(1, 0, 10);
        
        final CanvasDef cdef = new CanvasDef(new Dimension(10, 10), Color.WHITE);
        final SceneDef sdef = new SceneDef(context);
        final ViewDef vdef = new ViewDef(env);
        
        final BufferedImage img = DefaultPortrayalService.portray(cdef, sdef, vdef);
        
        assertEquals(Color.BLACK.getRGB(), img.getRGB(9, 5));
        assertEquals(Color.BLACK.getRGB(), img.getRGB(8, 5));
        assertEquals(Color.WHITE.getRGB(), img.getRGB(7, 5));        
    }

    
    private void testRendering(final MapLayer layer) throws TransformException, PortrayalException{
        final StopOnErrorMonitor monitor = new StopOnErrorMonitor();

        final MapContext context = MapBuilder.createContext(CommonCRS.WGS84.normalizedGeographic());
        context.layers().add(layer);
        assertEquals(1, context.layers().size());

        for(final Envelope env : envelopes){
            for(Date[] drange : dates){
                for(Double[] erange : elevations){
                    final Envelope cenv = ReferencingUtilities.combine(env, drange, erange);
                    final BufferedImage img = DefaultPortrayalService.portray(
                        new CanvasDef(new Dimension(800, 600), null),
                        new SceneDef(context),
                        new ViewDef(cenv,0,monitor));
                    assertNull(monitor.getLastException());
                    assertNotNull(img);
                }
            }
        }
    }

}
