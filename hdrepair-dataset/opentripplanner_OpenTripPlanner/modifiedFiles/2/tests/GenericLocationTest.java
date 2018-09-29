package org.opentripplanner.common.model;

import static org.junit.Assert.*;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;

public class GenericLocationTest {

    @Test
    public void testEmpty() {
        GenericLocation loc = new GenericLocation();
        assertEquals("", loc.getName());
        assertEquals("", loc.getPlace());
        
        NamedPlace np = loc.getNamedPlace();
        assertEquals("", np.name);
        assertEquals("", np.place);
        
        assertNull(loc.getLat());
        assertNull(loc.getLng());
        assertNull(loc.getCoordinate());
        assertFalse(loc.hasName());
        assertFalse(loc.hasPlace());
    }

    @Test
    public void testFromNamePlace() {
        GenericLocation loc = new GenericLocation("name", "12345");
        assertEquals("name", loc.getName());
        assertEquals("12345", loc.getPlace());
        
        NamedPlace np = loc.getNamedPlace();
        assertEquals("name", np.name);
        assertEquals("12345", np.place);
        
        assertFalse(loc.hasCoordinate());
        assertFalse(loc.hasHeading());
        assertTrue(loc.hasName());
        assertTrue(loc.hasPlace());
        assertNull(loc.getLat());
        assertNull(loc.getLng());
        assertNull(loc.getCoordinate());
    }

    @Test
    public void testFromNamePlaceWithCoord() {
        GenericLocation loc = new GenericLocation("name", "-1.0,2.5");
        assertEquals("name", loc.getName());
        assertEquals("-1.0,2.5", loc.getPlace());


        NamedPlace np = loc.getNamedPlace();
        assertEquals("name", np.name);
        assertEquals("-1.0,2.5", np.place);
        assertTrue(loc.hasName());
        assertTrue(loc.hasPlace());
        
        assertTrue(loc.hasCoordinate());
        assertFalse(loc.hasHeading());
        assertEquals(new Double(-1.0), loc.getLat());
        assertEquals(new Double(2.5), loc.getLng());
        assertEquals(new Coordinate(2.5, -1.0), loc.getCoordinate());

        loc = new GenericLocation("name", "1.0,-2.5");
        assertEquals(new Double(1.0), loc.getLat());
        assertEquals(new Double(-2.5), loc.getLng());
        assertEquals(new Coordinate(-2.5, 1.0), loc.getCoordinate());
    }

    @Test
    public void testFromOldStyleString() {
        GenericLocation loc = GenericLocation.fromOldStyleString("name::12345");
        assertEquals("name", loc.getName());
        assertEquals("12345", loc.getPlace());
        
        NamedPlace np = loc.getNamedPlace();
        assertEquals("name", np.name);
        assertEquals("12345", np.place);
        assertTrue(loc.hasName());
        assertTrue(loc.hasPlace());
        
        assertFalse(loc.hasCoordinate());
        assertFalse(loc.hasHeading());
        assertNull(loc.getLat());
        assertNull(loc.getLng());
        assertNull(loc.getCoordinate());
    }

    @Test
    public void testFromStringWithEdgeAndHeading() {
        String s = "40.75542978896869,-73.97618338000376 heading=29.028895183287617 edgeId=2767";
        GenericLocation loc = GenericLocation.fromOldStyleString(s);
        assertEquals(29.028895183287617, loc.getHeading(), 0.00001);
        assertEquals(2767, loc.getEdgeId().intValue());
        
        assertEquals(40.75542978896869, loc.getLat(), 0.00001);
        assertEquals(-73.97618338000376, loc.getLng(), 0.00001);
    }
    
    @Test
    public void testFromOldStyleStringWithCoord() {
        GenericLocation loc = GenericLocation.fromOldStyleString("name::1.0,2.5");
        assertEquals("name", loc.getName());
        assertEquals("1.0,2.5", loc.getPlace());
        
        NamedPlace np = loc.getNamedPlace();
        assertEquals("name", np.name);
        assertEquals("1.0,2.5", np.place);
        assertTrue(loc.hasName());
        assertTrue(loc.hasPlace());
        
        assertTrue(loc.hasCoordinate());
        assertFalse(loc.hasHeading());
        assertEquals(new Double(1.0), loc.getLat());
        assertEquals(new Double(2.5), loc.getLng());
        assertEquals(new Coordinate(2.5, 1.0), loc.getCoordinate());
    }
 
    @Test
    public void testToString() {
        String input = "name::1.0,2.5";
        GenericLocation loc = GenericLocation.fromOldStyleString(input);
        assertEquals(input, loc.toString());
        assertTrue(loc.hasCoordinate());
        assertFalse(loc.hasHeading());
        
        input = "name::12345";
        loc = GenericLocation.fromOldStyleString(input);
        assertEquals(input, loc.toString());
        assertFalse(loc.hasCoordinate());
        assertFalse(loc.hasHeading());
        
        input = "name";
        loc = GenericLocation.fromOldStyleString(input);
        assertEquals(input, loc.toString());
        assertFalse(loc.hasCoordinate());
        assertFalse(loc.hasHeading());
    }
    
    @Test
    public void testFromLatLng() {
        GenericLocation loc = new GenericLocation(1.0, 2.0);
        Coordinate expectedCoord = new Coordinate(2.0, 1.0);
        assertEquals(expectedCoord, loc.getCoordinate());
        assertEquals("1.0,2.0", loc.toString());
        assertTrue(loc.hasCoordinate());
        assertFalse(loc.hasHeading());
        
        assertFalse(loc.hasName());
        assertFalse(loc.hasPlace());   
    }
    
    @Test
    public void testFromLatLngHeading() {
        GenericLocation loc = new GenericLocation(1.0, 2.0, 137.2);
        Coordinate expectedCoord = new Coordinate(2.0, 1.0);
        assertEquals(expectedCoord, loc.getCoordinate());
        assertEquals(137.2, loc.getHeading().doubleValue(), 0.0);
        assertEquals("1.0,2.0", loc.toString());
        assertTrue(loc.hasCoordinate());
        assertTrue(loc.hasHeading());
        
        assertFalse(loc.hasName());
        assertFalse(loc.hasPlace());
    }
    
    @Test
    public void testFromCoord() {
        Coordinate expectedCoord = new Coordinate(2.0, 1.0);
        GenericLocation loc = new GenericLocation(expectedCoord);
        assertEquals(expectedCoord, loc.getCoordinate());
        assertEquals("1.0,2.0", loc.toString());
        assertTrue(loc.hasCoordinate());
        assertFalse(loc.hasHeading());
    }
    
    public void testClone() {
        Coordinate expectedCoord = new Coordinate(2.0, 1.0);
        GenericLocation loc = new GenericLocation(expectedCoord);
        loc.setHeading(137.2);
        GenericLocation cloned = loc.clone();
        
        assertEquals(expectedCoord, cloned.getCoordinate());
        assertEquals(loc.getHeading(), cloned.getHeading());
        assertEquals(loc.getNamedPlace(), cloned.getNamedPlace());
    }
}
