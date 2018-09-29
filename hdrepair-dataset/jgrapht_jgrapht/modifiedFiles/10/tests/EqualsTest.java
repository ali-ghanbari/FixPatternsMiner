/* ==========================================
 * JGraphT : a free Java graph-theory library
 * ==========================================
 *
 * Project Info:  http://jgrapht.sourceforge.net/
 * Project Creator:  Barak Naveh (http://sourceforge.net/users/barak_naveh)
 *
 * (C) Copyright 2003-2008, by Barak Naveh and Contributors.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */
/* --------------
 * EqualsTest.java
 * --------------
 * (C) Copyright 2012, by Vladimir Kostyukov and Contributors.
 *
 * Original Author:  Vladimir Kostyukov
 * Contributor(s):   -
 *
 * $Id$
 *
 * Changes
 * -------
 * 22-May-2012 : Initial revision (VK);
 *
 */

package org.jgrapht.graph;

import org.jgrapht.*;

public class EqualsTest
    extends EnhancedTestCase
{
    //~ Instance fields --------------------------------------------------------

    private String v1 = "v1";
    private String v2 = "v2";
    private String v3 = "v3";
    private String v4 = "v4";

    //~ Constructors -----------------------------------------------------------

    /**
     * @see junit.framework.TestCase#TestCase(java.lang.String)
     */
    public EqualsTest(String name)
    {
        super(name);
    }

    /**
     * Tests equals() method of DefaultDirectedGraph.
     */
    public void testDefaultDirectedGraph()
    {
        DirectedGraph<String, DefaultEdge> g1 =
            new DefaultDirectedGraph<String, DefaultEdge>(
                DefaultEdge.class);
        g1.addVertex(v1);
        g1.addVertex(v2);
        g1.addVertex(v3);
        g1.addVertex(v4);
        g1.addEdge(v1, v2);
        g1.addEdge(v2, v3);
        g1.addEdge(v3, v1);

        DirectedGraph<String, DefaultEdge> g2 = 
             new DefaultDirectedGraph<String, DefaultEdge>(
                 DefaultEdge.class);
        g2.addVertex(v4);
        g2.addVertex(v3);
        g2.addVertex(v2);
        g2.addVertex(v1);
        g2.addEdge(v3, v1);
        g2.addEdge(v2, v3);
        g2.addEdge(v1, v2);

        DirectedGraph<String, DefaultEdge> g3 = 
            new DefaultDirectedGraph<String, DefaultEdge>(
                DefaultEdge.class);
       g3.addVertex(v4);
       g3.addVertex(v3);
       g3.addVertex(v2);
       g3.addVertex(v1);
       g3.addEdge(v3, v1);
       g3.addEdge(v2, v3);

        assertTrue(g2.equals(g1));
        assertTrue(!g3.equals(g2));
    }

    /**
     * Tests equals() method of SimpleGraph.
     */
    public void testSimpleGraph()
    {
        UndirectedGraph<String, DefaultEdge> g1 =
            new SimpleGraph<String, DefaultEdge>(
                DefaultEdge.class);
        g1.addVertex(v1);
        g1.addVertex(v2);
        g1.addVertex(v3);
        g1.addVertex(v4);
        g1.addEdge(v1, v2);
        g1.addEdge(v2, v3);
        g1.addEdge(v3, v1);

        UndirectedGraph<String, DefaultEdge> g2 = 
             new SimpleGraph<String, DefaultEdge>(
                 DefaultEdge.class);
        g2.addVertex(v4);
        g2.addVertex(v3);
        g2.addVertex(v2);
        g2.addVertex(v1);
        g2.addEdge(v3, v1);
        g2.addEdge(v2, v3);
        g2.addEdge(v1, v2);

        UndirectedGraph<String, DefaultEdge> g3 = 
            new SimpleGraph<String, DefaultEdge>(
                DefaultEdge.class);
       g3.addVertex(v4);
       g3.addVertex(v3);
       g3.addVertex(v2);
       g3.addVertex(v1);
       g3.addEdge(v3, v1);
       g3.addEdge(v2, v3);

        assertTrue(g2.equals(g1));
        assertTrue(!g3.equals(g2));
    }

    /**
     * Tests equals() method for diefferent graphs.
     */
    public void testDifferentGraphs()
    {
        DirectedGraph<String, DefaultEdge> g1 =
            new DefaultDirectedGraph<String, DefaultEdge>(
                DefaultEdge.class);
        g1.addVertex(v1);
        g1.addVertex(v2);
        g1.addVertex(v3);
        g1.addVertex(v4);
        g1.addEdge(v1, v2);
        g1.addEdge(v2, v3);
        g1.addEdge(v3, v1);

        UndirectedGraph<String, DefaultEdge> g2 = 
             new SimpleGraph<String, DefaultEdge>(
                 DefaultEdge.class);
        g2.addVertex(v4);
        g2.addVertex(v3);
        g2.addVertex(v2);
        g2.addVertex(v1);
        g2.addEdge(v3, v1);
        g2.addEdge(v2, v3);
        g2.addEdge(v1, v2);

        UndirectedGraph<String, DefaultEdge> g3 = 
            new SimpleGraph<String, DefaultEdge>(
                DefaultEdge.class);
        g3.addVertex(v1);
        g3.addVertex(v2);
        g3.addVertex(v3);
        g3.addVertex(v4);
        g3.addEdge(v2, v3);
        g3.addEdge(v3, v1);
        g3.addEdge(v1, v2);

        assertTrue(!g2.equals(g1));
        assertTrue(g3.equals(g2));
    }
}
