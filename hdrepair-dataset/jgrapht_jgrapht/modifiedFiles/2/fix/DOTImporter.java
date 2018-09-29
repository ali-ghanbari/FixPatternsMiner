/* ==========================================
 * JGraphT : a free Java graph-theory library
 * ==========================================
 *
 * Project Info:  http://jgrapht.sourceforge.net/
 * Project Creator:  Barak Naveh (http://sourceforge.net/users/barak_naveh)
 *
 * (C) Copyright 2003-2008, by Barak Naveh and Contributors.
 *
 * This program and the accompanying materials are dual-licensed under
 * either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation, or (at your option) any
 * later version.
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation.
 */
/* ------------------
 * DOTImporter.java
 * ------------------
 * (C) Copyright 2015, by  Wil Selwood.
 *
 * Original Author:  Wil Selwood <wselwood@ijento.com>
 *
 */
package org.jgrapht.ext;

import java.util.*;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.graph.AbstractBaseGraph;
import org.jgrapht.graph.Multigraph;


/**
 * Imports a graph from a DOT file.
 *
 * <p>For a description of the format see <a
 * href="http://en.wikipedia.org/wiki/DOT_language">
 * http://en.wikipedia.org/wiki/DOT_language</a> and
 * <a href="http://www.graphviz.org/doc/info/lang.html">
 *    http://www.graphviz.org/doc/info/lang.html</a></p>
 *
 * @author Wil Selwood
 */
public class DOTImporter<V,E> {

   private VertexProvider<V>  vertexProvider;
   private VertexUpdater<V>   vertexUpdater;
   private EdgeProvider<V, E> edgeProvider;

   /**
    * Constructs a new DOTImporter with the given providers
    * @param vertexProvider Provider to create a vertex
    * @param edgeProvider Provider to create an edge
    */
   public DOTImporter(VertexProvider<V> vertexProvider,
                      EdgeProvider<V, E> edgeProvider)
   {
      this.vertexProvider = vertexProvider;
      this.vertexUpdater = null;
      this.edgeProvider = edgeProvider;
   }

   /**
    * Constructs a new DOTImporter with the given providers
    * @param vertexProvider Provider to create a vertex
    * @param edgeProvider Provider to create an edge
    */
   public DOTImporter(VertexProvider<V> vertexProvider,
                      EdgeProvider<V, E> edgeProvider,
                      VertexUpdater<V> updater)
   {
      this.vertexProvider = vertexProvider;
      this.vertexUpdater = updater;
      this.edgeProvider = edgeProvider;
   }

   /**
    * Read a dot formatted string and populate the provided graph.
    * @param input the content of a dot file.
    * @param graph the graph to update.
    * @throws ImportException if there is a problem parsing the file.
    */
   public void read(String input, AbstractBaseGraph<V, E> graph)
         throws ImportException
   {

      if (input == null || input.isEmpty()) {
         throw new ImportException("Dot string was empty");
      }

      String[] lines = input.split("[;\r\n]");

      validateLines(lines, graph);

      // cache of vertexes added to the graph.
      Map<String, V> vertexes = new HashMap<String, V>();

      for(int lineIndex = 1; lineIndex < lines.length - 1; lineIndex ++ ) {
         String line = lines[lineIndex].trim();

         // trim off line comments.
         if (line.contains("//")) {
            line = line.substring(0, line.indexOf("//"));
         }

         // with \r\n or just ;\n line ends we get blanks. Filter here.
         if(line.isEmpty()) {
            continue;
         }

         if (line.startsWith("#")) {
            // line comment so ignore
            // TODO: block comments
         } else if (!line.contains("[") && line.contains("=")) {
            throw new ImportException(
                  "graph level properties are not currently supported."
            );
         } else if (!line.contains("-")) {
            // probably a vertex
            Map<String, String> attributes = extractAttributes(line);

            String id = line.trim();
            int bracketIndex = line.indexOf('[');
            if (bracketIndex > 0) {
               id = line.substring(0, line.indexOf('[')).trim();
            }

            String label = attributes.get("label");
            if (label == null) {
               label = id;
            }

            V existing = vertexes.get(id);
            if (existing == null) {
               V vertex = vertexProvider.buildVertex(label, attributes);
               graph.addVertex(vertex);
               vertexes.put(id, vertex);
            } else {
               if (vertexUpdater != null) {
                  vertexUpdater.updateVertex(existing, attributes);
               } else {
                  throw new ImportException(
                        "Update required for vertex "
                        + label
                        + " but no vertexUpdater provided"
                  );
               }
            }
         } else {
            Map<String, String> attributes = extractAttributes(line);

            List<String> ids = extractEdgeIds(line);

            // for each pair of ids in the list create an edge.
            for(int i = 0; i < ids.size() - 1; i++) {
               V v1 = getVertex(ids.get(i), vertexes, graph);
               V v2 = getVertex(ids.get(i+1), vertexes, graph);

               E edge = edgeProvider.buildEdge(v1,
                                               v2,
                                               attributes.get("label"),
                                               attributes);
               graph.addEdge(v1, v2, edge);
            }
         }

      }

   }

   private void validateLines(String[] lines,
                              AbstractBaseGraph<V,E> graph)
         throws ImportException
   {
      if(lines.length < 2) {
         throw new ImportException("Dot string was invalid");
      }
      // validate the first line
      String[] firstLine = lines[0].split(" ", 4);
      if(firstLine.length < 3) {
         throw new ImportException("not enough parts on first line");
      }

      int i = 0;
      if (graph.isAllowingMultipleEdges() && firstLine[i].equals("strict")) {
         throw new ImportException(
               "graph defines strict but Multigraph given."
         );
      } else if (firstLine[i].equals("strict")) {
         i = i + 1;
      }

      if (graph instanceof DirectedGraph && firstLine[i].equals("graph") ) {
         throw new ImportException(
               "input asks for undirected graph and directed graph provided."
         );
      } else if (!(graph instanceof DirectedGraph)
                 && firstLine[i].equals("digraph")) {
         throw new ImportException(
               "input asks for directed graph but undirected graph provided."
         );
      } else if(!firstLine[i].equals("graph")
                && !firstLine[i].equals("digraph")){
         throw new ImportException("unknown graph type");
      }
   }

   // if a vertex id doesn't already exist create one for it
   // with no attributes.
   private V getVertex(String id, Map<String, V> vertexes, Graph<V, E> graph)
   {
      V v = vertexes.get(id);
      if (v == null) {
         v = vertexProvider.buildVertex(id, new HashMap<String, String>());
         graph.addVertex(v);
         vertexes.put(id, v);
      }
      return v;
   }

   private List<String> extractEdgeIds(String line)
   {
      String idChunk = line;
      int bracketIndex = line.indexOf('[');
      if (bracketIndex > 1) {
         idChunk = idChunk.substring(0, bracketIndex).trim();
      }
      int index = 0;
      List<String> ids = new ArrayList<String>();
      while(index < idChunk.length()) {
         int nextSpace = idChunk.indexOf(' ', index);
         String chunk;
         if ( nextSpace > 0) { // is this the last chunk
            chunk = idChunk.substring(index, nextSpace);
            index = nextSpace + 1;
         } else {
            chunk = idChunk.substring(index);
            index = idChunk.length() + 1;
         }
         if(!chunk.equals("--") && !chunk.equals("->")) {  // a label then?
            ids.add(chunk);
         }

      }

      return ids;
   }

   private Map<String, String> extractAttributes(String line)
   {
      Map<String, String> attributes = new HashMap<String, String>();
      int bracketIndex = line.indexOf("[");
      if (bracketIndex > 0) {
         attributes = splitAttributes(
               line.substring(bracketIndex + 1, line.lastIndexOf(']')).trim()
         );
      }
      return attributes;
   }

   private Map<String, String> splitAttributes(String input)
   {
      int index = 0;
      Map<String, String> result = new HashMap<String, String>();
      while(index < input.length()) {
         int nextEquals = input.indexOf('=', index);
         String key = input.substring(index, nextEquals).trim();
         int firstQuote = input.indexOf('\"', nextEquals) + 1;
         int secondQuote = findNextQuote(input, firstQuote);
         String value = input.substring(firstQuote, secondQuote);
         result.put(key, value);
         index = secondQuote+1;
      }
      return result;
   }


   private int findNextQuote(String input, int start) {
      int result = start;
      do {
         result = input.indexOf('\"', result + 1);
         // if the previous character is an escape then keep going
      } while(input.charAt(result - 1) == '\\'
              && !(input.charAt(result - 1) == '\\'
                   && input.charAt(result - 2) == '\\')); // unless its escaped
      return result;
   }
}
