package org.esa.beam.framework.datamodel;

import org.esa.beam.framework.dataop.maptransf.*;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.cs.DefaultEllipsoidalCS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeocentricCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;

import java.awt.geom.AffineTransform;

class CoordinateReferenceSystems {

    private static final GeocentricCRS ITRF97;
    private static final GeographicCRS WGS72;
    private static final GeographicCRS WGS84;

    static {
        final CRSAuthorityFactory crsFactory = ReferencingFactoryFinder.getCRSAuthorityFactory("EPSG", null);

        try {
            ITRF97 = crsFactory.createGeocentricCRS("EPSG:4918");
            WGS72 = crsFactory.createGeographicCRS("EPSG:4322");
            WGS84 = crsFactory.createGeographicCRS("EPSG:4326");
        } catch (FactoryException e) {
            throw new IllegalStateException(e);
        }
    }

    public static CoordinateReferenceSystem getCRS(MapProjection projection, Datum datum) {
        CoordinateReferenceSystem result = WGS84;

        try {
            final MapTransform mapTransform = projection.getMapTransform();
            if (mapTransform.getDescriptor() instanceof IdentityTransformDescriptor) {
                // 1. Identity map projection
                if (Datum.ITRF_97.equals(datum)) {
                    result = ITRF97;
                } else if (Datum.WGS_72.equals(datum)) {
                    result = WGS72;
                }
            } else if (projection instanceof UTMProjection && !Datum.ITRF_97.equals(datum)) {
                // 2. UTM map projections
                final UTMProjection utmProjection = (UTMProjection) projection;
                final int zone = utmProjection.getZone();

                if (zone >= 1 && zone <= 60) {
                    final CRSAuthorityFactory factory = ReferencingFactoryFinder.getCRSAuthorityFactory("EPSG", null);
                    if (utmProjection.isNorth()) {
                        if (Datum.WGS_72.equals(datum)) {
                            result = factory.createProjectedCRS("EPSG:" + (32200 + zone));
                        } else if (Datum.WGS_84.equals(datum)) {
                            result = factory.createProjectedCRS("EPSG:" + (32200 + zone));
                        }
                    } else {
                        if (Datum.WGS_72.equals(datum)) {
                            result = factory.createProjectedCRS("EPSG:" + (32300 + zone));
                        } else if (Datum.WGS_84.equals(datum)) {
                            result = factory.createProjectedCRS("EPSG:" + (32700 + zone));
                        }
                    }
                }
            } else if (Datum.ITRF_97.equals(datum)) {
                // 3. Other map projections
                final String crsName = "ITRF 97 / " + mapTransform.getDescriptor().getName();
                final DefaultGeographicCRS baseCrs = new DefaultGeographicCRS(GeodeticDatums.ITRF97,
                                                                              DefaultEllipsoidalCS.GEODETIC_2D);
                final MathTransform mathTransform = getMathTransform(mapTransform);
                if (mathTransform != null) {
                    result = new DefaultProjectedCRS(crsName, baseCrs, mathTransform, DefaultCartesianCS.PROJECTED);
                }
            } else if (Datum.WGS_72.equals(datum)) {
                final String crsName = "WGS 72 / " + mapTransform.getDescriptor().getName();
                final MathTransform mathTransform = getMathTransform(mapTransform);
                if (mathTransform != null) {
                    result = new DefaultProjectedCRS(crsName, WGS72, mathTransform, DefaultCartesianCS.PROJECTED);
                }
            } else if (Datum.WGS_84.equals(datum)) {
                final String crsName = "WGS 84 / " + mapTransform.getDescriptor().getName();
                final MathTransform mathTransform = getMathTransform(mapTransform);
                if (mathTransform != null) {
                    result = new DefaultProjectedCRS(crsName, WGS84, mathTransform, DefaultCartesianCS.PROJECTED);
                }
            }
        } catch (FactoryException e) {
            // ignore
        }

        return result;
    }

    private static MathTransform getMathTransform(MapTransform mapTransform) throws FactoryException {
        if (mapTransform.getDescriptor() instanceof AffineTransformDescriptor) {
            return new AffineTransform2D(new AffineTransform(mapTransform.getParameterValues()));
        }
        if (mapTransform instanceof AlbersEqualAreaConicDescriptor.AEAC) {
            return createAlbersConicEqualAreaMathTransform((AlbersEqualAreaConicDescriptor.AEAC) mapTransform);
        }
        if (mapTransform instanceof LambertConformalConicDescriptor.LCCT) {
            return createLambertConformalConicMathTransform((LambertConformalConicDescriptor.LCCT) mapTransform);
        }
        if (mapTransform instanceof StereographicDescriptor.ST) {
            return createStereographicMathTransform((StereographicDescriptor.ST) mapTransform);
        }
        if (mapTransform instanceof TransverseMercatorDescriptor.TMT) {
            return createTransverseMercatorMathTransform((TransverseMercatorDescriptor.TMT) mapTransform);
        }

        return null;
    }

    private static MathTransform createAlbersConicEqualAreaMathTransform(AlbersEqualAreaConicDescriptor.AEAC t)
            throws FactoryException {
        final MathTransformFactory transformFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
        final ParameterValueGroup parameters = transformFactory.getDefaultParameters("EPSG:9822");

        parameters.parameter("semi_major").setValue(t.getSemiMajor());
        parameters.parameter("semi_minor").setValue(t.getSemiMinor());
        parameters.parameter("central_meridian").setValue(t.getCentralMeridian());
        parameters.parameter("latitude_of_origin").setValue(t.getLatitudeOfOrigin());
        parameters.parameter("standard_parallel_1").setValue(t.getStandardParallel1());
        parameters.parameter("standard_parallel_2").setValue(t.getStandardParallel2());
        parameters.parameter("false_easting").setValue(t.getFalseEasting());
        parameters.parameter("false_northing").setValue(t.getFalseNorthing());

        return transformFactory.createParameterizedTransform(parameters);
    }

    private static MathTransform createLambertConformalConicMathTransform(LambertConformalConicDescriptor.LCCT t)
            throws FactoryException {
        final MathTransformFactory transformFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
        final ParameterValueGroup parameters = transformFactory.getDefaultParameters("ESRI:Lambert_Conformal_Conic");

        parameters.parameter("semi_major").setValue(t.getSemiMajor());
        parameters.parameter("semi_minor").setValue(t.getSemiMinor());
        parameters.parameter("central_meridian").setValue(t.getCentralMeridian());
        parameters.parameter("latitude_of_origin").setValue(t.getLatitudeOfOrigin());
        parameters.parameter("standard_parallel_1").setValue(t.getStandardParallel1());
        parameters.parameter("standard_parallel_2").setValue(t.getStandardParallel2());
        parameters.parameter("scale_factor").setValue(t.getScaleFactor());
        parameters.parameter("false_easting").setValue(t.getFalseEasting());
        parameters.parameter("false_northing").setValue(t.getFalseNorthing());

        return transformFactory.createParameterizedTransform(parameters);
    }

    private static MathTransform createStereographicMathTransform(StereographicDescriptor.ST t)
            throws FactoryException {
        final MathTransformFactory transformFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
        final ParameterValueGroup parameters;

        if (t.isPolar()) {
            parameters = transformFactory.getDefaultParameters("EPSG:9810");
        } else {
            parameters = transformFactory.getDefaultParameters("EPSG:9809");
        }

        parameters.parameter("semi_major").setValue(t.getSemiMajor());
        parameters.parameter("semi_minor").setValue(t.getSemiMinor());
        parameters.parameter("central_meridian").setValue(t.getCentralMeridian());
        parameters.parameter("latitude_of_origin").setValue(t.getLatitudeOfOrigin());
        parameters.parameter("scale_factor").setValue(t.getScaleFactor());
        parameters.parameter("false_easting").setValue(t.getFalseEasting());
        parameters.parameter("false_northing").setValue(t.getFalseNorthing());

        return transformFactory.createParameterizedTransform(parameters);
    }

    private static MathTransform createTransverseMercatorMathTransform(TransverseMercatorDescriptor.TMT t)
            throws FactoryException {

        final MathTransformFactory transformFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
        final ParameterValueGroup parameters = transformFactory.getDefaultParameters("EPSG:9807");

        parameters.parameter("semi_major").setValue(t.getSemiMajor());
        parameters.parameter("semi_minor").setValue(t.getSemiMinor());
        parameters.parameter("central_meridian").setValue(t.getCentralMeridian());
        parameters.parameter("latitude_of_origin").setValue(t.getLatitudeOfOrigin());
        parameters.parameter("scale_factor").setValue(t.getScaleFactor());
        parameters.parameter("false_easting").setValue(t.getFalseEasting());
        parameters.parameter("false_northing").setValue(t.getFalseNorthing());

        return transformFactory.createParameterizedTransform(parameters);
    }
}
