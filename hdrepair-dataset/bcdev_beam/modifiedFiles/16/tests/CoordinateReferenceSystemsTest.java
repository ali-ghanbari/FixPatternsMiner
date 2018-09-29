package org.esa.beam.framework.datamodel;

import junit.framework.TestCase;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.IdentityTransformDescriptor;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapProjectionRegistry;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Projection;
import org.opengis.util.GenericName;

import java.util.Set;

public class CoordinateReferenceSystemsTest extends TestCase {

    public void testCoordinateReferenceSystems() throws FactoryException {
        final CRSAuthorityFactory factory = ReferencingFactoryFinder.getCRSAuthorityFactory("EPSG", null);
        final MapProjection gp = MapProjectionRegistry.getProjection(new IdentityTransformDescriptor().getName());

        assertSame(factory.createCoordinateReferenceSystem("EPSG:4918"),
                   CoordinateReferenceSystems.getCRS(gp, Datum.ITRF_97));
        assertSame(factory.createCoordinateReferenceSystem("EPSG:4322"),
                   CoordinateReferenceSystems.getCRS(gp, Datum.WGS_72));
        assertSame(factory.createCoordinateReferenceSystem("EPSG:4326"),
                   CoordinateReferenceSystems.getCRS(gp, Datum.WGS_84));

        for (final MapProjection projection : MapProjectionRegistry.getProjections()) {
            if (!(projection.getMapTransform().getDescriptor() instanceof IdentityTransformDescriptor)) {
                assertTrue(CoordinateReferenceSystems.getCRS(projection, Datum.ITRF_97) instanceof ProjectedCRS);
                assertTrue(CoordinateReferenceSystems.getCRS(projection, Datum.WGS_72) instanceof ProjectedCRS);
                assertTrue(CoordinateReferenceSystems.getCRS(projection, Datum.WGS_84) instanceof ProjectedCRS);
            }
        }
    }

    public void testProjectionNamesAndAliases() {
        final DefaultMathTransformFactory mtf = new DefaultMathTransformFactory();
        Set<OperationMethod> methods = mtf.getAvailableMethods(Projection.class);
        for (OperationMethod method : methods) {
            System.out.println("method.getName() = " + method.getName());
            for (final GenericName name : method.getAlias()) {
                if (name.toString().startsWith("EPSG")) {
                    System.out.println("method.getAlias() = " + name);
                }
            }
        }
    }

}
