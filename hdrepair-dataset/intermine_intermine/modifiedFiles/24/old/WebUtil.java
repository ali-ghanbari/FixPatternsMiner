package org.intermine.web.logic;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.search.SearchRepository;
import org.intermine.web.logic.tagging.TagTypes;
import org.intermine.web.logic.widget.BenjaminiHochberg;
import org.intermine.web.logic.widget.Bonferroni;
import org.intermine.web.logic.widget.ErrorCorrection;
import org.intermine.web.logic.widget.Hypergeometric;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
/**
 * Utility methods for the web package.
 *
 * @author Kim Rutherford
 * @author Julie Sullivan
 */

public abstract class WebUtil
{
    protected static final Logger LOG = Logger.getLogger(WebUtil.class);

    /**
     * Lookup an Integer property from the SessionContext and return it.
     * @param session the current session
     * @param propertyName the property to find
     * @param defaultValue the value to return if the property isn't present
     * @return the int value of the property
     */
    public static int getIntSessionProperty(HttpSession session, String propertyName,
                                            int defaultValue) {
        Map webProperties =
            (Map) session.getServletContext().getAttribute(Constants.WEB_PROPERTIES);
        String n = (String) webProperties.get(propertyName);

        int intVal = defaultValue;

        try {
            intVal = Integer.parseInt(n);
        } catch (NumberFormatException e) {
            LOG.warn("Failed to parse " + propertyName + " property: " + n);
        }

        return intVal;
    }

    /**
     * Gets the cache directory.
     * @param servletContext the servlet context
     * @return cache directory
     */
    public static File getCacheDirectory(ServletContext servletContext) {
        Properties p = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        String dir = p.getProperty("webapp.cachedir");
        if (StringUtils.isEmpty(dir)) {
            throw new RuntimeException("Please define webapp.cachedir in your build properties");
        }
        File cacheDir = new File(dir);
        if (!cacheDir.exists()) {
            throw new RuntimeException("No such directory: " + cacheDir.getPath());
        }
        String version = p.getProperty("project.releaseVersion");
        cacheDir = new File(cacheDir, version);
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdir()) {
                throw new RuntimeException("Failed to create directory: " + cacheDir.getPath());
            }
        }
        return cacheDir;
    }

    /**new Integer(
     * Given an image on disk, write the image to the client. Assumes content type from
     * the file extensions.
     * @param imgFile image file
     * @param response the http response object
     * @throws IOException if something goes wrong
     */
    public static void sendImageFile(File imgFile, HttpServletResponse response)
        throws IOException {
        String type = StringUtils.substringAfterLast(imgFile.getName(), ".");
        response.setContentType("image/" + type);
        IOUtils.copy(new FileReader(imgFile), response.getOutputStream());
    }

    /**
     * Convert an SQL LIKE/NOT LIKE expression to a * wildcard expression.
     *
     * @param exp  the wildcard expression
     * @return     the SQL LIKE parameter
     */
    public static String wildcardSqlToUser(String exp) {
        StringBuffer sb = new StringBuffer();

        // To quote a '%' in PostgreSQL we need to pass \\% because it strips one level of
        // backslashes when parsing a string and another when parsing a LIKE expression.
        // Java needs backslashes to be backslashed in strings, hence all the blashslashes below
        // see. http://www.postgresql.org/docs/7.3/static/functions-matching.html

        for (int i = 0; i < exp.length(); i++) {
            String substring = exp.substring(i);
            if (substring.startsWith("%")) {
                sb.append("*");
            } else {
                if (substring.startsWith("_")) {
                    sb.append("?");
                } else {
                    if (substring.startsWith("\\\\%")) {
                        sb.append("%");
                        i += 2;
                    } else {
                        if (substring.startsWith("\\\\_")) {
                            sb.append("_");
                            i += 2;
                        } else {
                            if (substring.startsWith("*")) {
                                sb.append("\\*");
                            } else {
                                if (substring.startsWith("?")) {
                                    sb.append("\\?");
                                } else {
                                    if (substring.startsWith("\\\\\\\\")) {
                                        i += 3;
                                        sb.append("\\\\");
                                    } else {
                                        sb.append(substring.charAt(0));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return sb.toString();
    }

    /**
     * Turn a user supplied wildcard expression with * into an SQL LIKE/NOT LIKE
     * expression with %'s.
     *
     * @param exp  the SQL LIKE parameter
     * @return     the equivalent wildcard expression
     */
    public static String wildcardUserToSql(String exp) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < exp.length(); i++) {
            String substring = exp.substring(i);
            if (substring.startsWith("*")) {
                sb.append("%");
            } else if (substring.startsWith("?")) {
                sb.append("_");
            } else if (substring.startsWith("\\*")) {
                sb.append("*");
                i++;
            } else if (substring.startsWith("\\?")) {
                sb.append("?");
                i++;
            } else if (substring.startsWith("%")) {
                sb.append("\\\\%");
            } else if (substring.startsWith("_")) {
                sb.append("\\\\_");
            } else if (substring.startsWith("\\")) {
                sb.append("\\\\\\\\");
                i++;
            } else {
                sb.append(substring.charAt(0));
            }
        }

        return sb.toString();
    }

    /**
     * Make a copy of a Results object, but with a different batch size.
     * @param oldResults the original Results objects
     * @param newBatchSize the new batch size
     * @return a new Results object with a new batch size
     */
    public static Results changeResultBatchSize(Results oldResults, int newBatchSize) {
        Results newResults = oldResults.getObjectStore().execute(oldResults.getQuery());
        newResults.setBatchSize(newBatchSize);
        return newResults;
    }

    /**
     * Verifies names (bags, queries, etc) only contain A-Z, a-z, 0-9, underscores and
     * dashes.  And spaces.  And dots.
     * @param name Name of bag/query/template to be validated
     * @return isValid Returns true if this name is correct, false if this name contains a bad char
     */
    public static boolean isValidName(String name) {
        if (name == null) {
            return false;
        } else {
            Pattern p = Pattern.compile("[^\\w\\s\\.:]");
            Matcher m = p.matcher(name);
            return !m.find();
        }
    }

    /**
     * Returns the word value of special characters (ie returns _AMPERSAND_ for &, etc).  Used for
     * the forced renaming of queries/templates in the query/template import.
     * @param specialCharacter The special character, ie &
     * @return wordEquivalent The special character's name, ie AMPERSAND
     */
    public static String getSpecCharToText(String specialCharacter) {

        HashMap specCharToText = mapChars();
        String wordEquivalent = (String) specCharToText.get(specialCharacter);
        wordEquivalent = "_" + wordEquivalent + "_";
        return wordEquivalent;

    }


    /**
     * Takes a string and replaces special characters with the text value, e.g. it would change
     * "a&b" to "a_AMPERSAND_b".  This is used in the query/template imports to handle special
     * characters.
     * @param name Name of query/template
     * @return rebuiltName Name of query/template with the special characters removed
     */
    public static String replaceSpecialChars(String name) {
        String tmp = name;
        String rebuiltName = "";

        for (int i = 0; i < tmp.length(); i++) {
            char c = tmp.charAt(i);
            String str = String.valueOf(c);

            if (!WebUtil.isValidName(str)) {
                rebuiltName += WebUtil.getSpecCharToText(str);
            } else {
                rebuiltName += str;
            }
        }
        return rebuiltName;
    }


    private static HashMap<String, String> mapChars() {

        HashMap<String, String> specCharToText = new HashMap<String, String> ();

        specCharToText.put("‘", new String("QUOTE"));
        specCharToText.put("’", new String("QUOTE"));
        specCharToText.put("“", new String("QUOTE"));
        specCharToText.put("”", new String("QUOTE"));
        specCharToText.put("‹", new String("LESS_THAN_SIGN"));
        specCharToText.put("›", new String("GREATER_THAN_SIGN"));
        specCharToText.put("!", new String("EXCLAMATION_POINT"));
        specCharToText.put("£", new String("POUND_SIGN"));
        specCharToText.put("$", new String("DOLLAR_SIGN"));
        specCharToText.put("%", new String("PERCENT_SIGN"));

        specCharToText.put("^", new String("CARET"));
        specCharToText.put("&", new String("AMPERSAND"));
        specCharToText.put("(", new String("LEFT_PARENTHESIS"));
        specCharToText.put(")", new String("RIGHT_PARENTHESIS"));
        specCharToText.put("+", new String("PLUS_SIGN"));
        specCharToText.put("=", new String("EQUALS_SIGN"));
        specCharToText.put("{", new String("LEFT_BRACKET"));
        specCharToText.put("}", new String("RIGHT_BRACKET"));
        specCharToText.put("[", new String("LEFT_BRACKET"));
        specCharToText.put("]", new String("RIGHT_BRACKET"));
        specCharToText.put(":", new String("COLON"));

        specCharToText.put(";", new String("SEMICOLON"));
        specCharToText.put("@", new String("AT_SIGN"));
        specCharToText.put(",", new String("COMMA"));
        specCharToText.put("?", new String("QUESTION_MARK"));
        specCharToText.put("~", new String("TILDE"));
        specCharToText.put("#", new String("HASH"));
        specCharToText.put("<", new String("LESS_THAN"));
        specCharToText.put(">", new String("GREATER_THAN"));
        specCharToText.put("'", new String("APOSTROPHE"));
        specCharToText.put("/", new String("FORWARD_SLASH"));
        specCharToText.put("\\", new String("BACK_SLASH"));
        specCharToText.put("*", new String("STAR"));

        return specCharToText;
    }

    /**
     * takes a map and puts it in random order
     * also shortens the list to be map.size() = max
     * @param map The map to be randomised - the Map will be unchanged after the call
     * @param max the number of items to be in the final list
     * @return the newly randomised, shortened map
     */
    public static <V> Map<String, V> shuffle(Map<String, V> map, int max) {
        List<String> keys = new ArrayList<String>(map.keySet());

        Collections.shuffle(keys);

        if (keys.size() > max) {
            keys = keys.subList(0, max);
        }

        Map<String, V> returnMap = new HashMap<String, V>();

        for (String key: keys) {
            returnMap.put(key, map.get(key));
        }
        return returnMap;
    }

    /**
     * Returns all bags of a given type
     * @param bagMap a Map from bag name to InterMineBag
     * @param type the type
     * @param model the Model
     * @return a Map of bag name to bag
     */
    public static Map getBagsOfType(Map<String, InterMineBag> bagMap, String type, Model model) {
        type = model.getPackageName() + "." + type;
        Set<String> classAndSubs = new HashSet<String>();
        classAndSubs.add(type);
        Iterator subIter = model.getAllSubs(model.getClassDescriptorByName(type)).iterator();
        while (subIter.hasNext()) {
            classAndSubs.add(((ClassDescriptor) subIter.next()).getType().getName());
        }

        TreeMap map = new TreeMap();
        for (Iterator iter = bagMap.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iter.next();
            InterMineBag bag = (InterMineBag) entry.getValue();
            if (classAndSubs.contains(model.getPackageName() + "." + bag.getType())) {
                map.put(entry.getKey(), bag);
            }
        }
        return map;

    }

    /**
     * @param servletContext servlet context
     * @param userBags list of user's bags
     * @return map containing all bags
     */
    public static Map<String, InterMineBag> getAllBags(Map<String, InterMineBag> userBags,
                                                       ServletContext servletContext) {
        Map<String, InterMineBag> searchBags = new HashMap<String, InterMineBag>();

        SearchRepository searchRepository =
            SearchRepository.getGlobalSearchRepository(servletContext);
        Map<String, InterMineBag> publicBagMap =
            (Map<String, InterMineBag>) searchRepository.getWebSearchableMap(TagTypes.BAG);

        if (publicBagMap != null) {
            searchBags.putAll(publicBagMap);
        }

        // user bags override public ones
        searchBags.putAll(userBags);
        return searchBags;
    }

    /**
     * Return the contents of the page given by prefixURLString + '/' + path as a String.  Any
     * relative links in the page will be modified to go via showStatic.do
     * @param prefixURLString the prefix (including "http://...") of the web site to read from.
     *    eg. http://www.flymine.org/doc/help
     * @param path the page to retrieve eg. manualFlyMineHome.shtml
     * @return the contents of the page
     * @throws IOException if there is a problem while reading
     */
    public static String getStaticPage(String prefixURLString, String path)
        throws IOException {
        StringBuffer buf = new StringBuffer();

        URL url = new URL(prefixURLString + '/' + path);
        URLConnection connection = url.openConnection();
        InputStream is = connection.getInputStream();
        Reader reader = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(reader);
        String line;
        while ((line = br.readLine()) != null) {
            // replace relative urls ie. href="manualExportfasta.shtml"
            line = line.replaceAll("href=\"([^\"]+)\"",
                                   "href=\"showStatic.do?path=$1\"");
            buf.append(line + "\n");
        }
        return buf.toString();
    }

    private static Map<String, List> statsCalcCache = new HashMap<String, List>();

    /**
     * Takes two queries.  Runs both and compares the results.
     * @param os the object store
     * @param queryPopulation The query to get the entire population, ie all genes in the database
     * @param querySample The query to get the sample, ie all genes in the bag
     * @param bag the bag we are analysing
     * @param total total number of the entire population
     * @param maxValue maximum value to return
     * @param errorCorrection which error correction algorithm to use, Bonferroni
     * or Benjamini Hochberg
     * @return array of three results maps
     * @throws ObjectStoreException bag has been lost
     */
    public static ArrayList<Map> statsCalc(ObjectStoreInterMineImpl os,
                                      Query queryPopulation,
                                      Query querySample,
                                      InterMineBag bag,
                                      int total,                                      
                                      Double maxValue,
                                      String errorCorrection) 
                                      throws ObjectStoreException {

            ArrayList<Map> maps = new ArrayList<Map>();
            int numberOfObjectsInBag;
            try {
                numberOfObjectsInBag = bag.size();
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException("couldn't calculate bag size", e);
            }
            // run bag query
            Results r = os.execute(querySample);
            r.setBatchSize(10000);
            Iterator iter = r.iterator();
            HashMap<String, Long> countMap = new HashMap<String, Long>();
            HashMap<String, String> idMap = new HashMap<String, String>();

            while (iter.hasNext()) {

                // extract results
                ResultsRow rr =  (ResultsRow) iter.next();

                // id of item
                String id = (String) rr.get(0);

                // count of item
                Long count = (Long) rr.get(1);

                // id & count
                countMap.put(id, count);

                // id & label
                idMap.put(id, (String) rr.get(2));

            }

            // run population query
            List rAll = statsCalcCache.get(queryPopulation.toString());
            if (rAll == null) {
                rAll = os.execute(queryPopulation);
                ((Results) rAll).setBatchSize(10000);
                rAll = new ArrayList(rAll);
                statsCalcCache.put(queryPopulation.toString(), rAll);
            }
            LOG.error("statCalcCache: " + statsCalcCache);
            LOG.error("from: " + querySample.getFrom());
            
            Iterator itAll = rAll.iterator();

            Hypergeometric h = new Hypergeometric(total);
            HashMap<String, BigDecimal> resultsMap = new HashMap<String, BigDecimal>();
            
            while (itAll.hasNext()) {

                ResultsRow rrAll =  (ResultsRow) itAll.next();

                String id = (String) rrAll.get(0);

                if (countMap.containsKey(id)) {

                    Long countBag = countMap.get(id);
                    Long countAll = (java.lang.Long) rrAll.get(1);

                    double p = h.calculateP(numberOfObjectsInBag, countBag.intValue(),
                                            countAll.intValue(), total);
                    try {
                        resultsMap.put(id, new BigDecimal(p));
                    } catch (Exception e) {
                        String msg = p + " isn't a double.  calculated using sample size: " 
                        + countBag + " and population size: " + countAll + ".  population query " 
                        + "used: " + queryPopulation.toString();
                        throw new RuntimeException(msg, e);
                    }
                }
            }

           Map<String, BigDecimal> adjustedResultsMap = resultsMap;

            if (!errorCorrection.equals("None")) {
                adjustedResultsMap = calcErrorCorrection(errorCorrection, maxValue, resultsMap);
            }

            SortableMap sortedMap = new SortableMap(adjustedResultsMap);
            sortedMap.sortValues();

            maps.add(0, sortedMap);
            maps.add(1, countMap);
            maps.add(2, idMap);
            return maps;
        }

    /**
     * See online help docs for detailed description of what error correction is and why we need it.
     * Briefly, in all experiments certain things happen that look interesting but really just 
     * happened by chance.  We need to account for this phenonomenon to ensure our numbers are 
     * interesting behaviour and not just random happenstance.
     * 
     * To do this we take all of our p-values and adjust them.  Here we are using on of our two
     * methods available - which one we use is determined by the user.
     * @param errorCorrection which multiple hypothesis test correction to use - Bonferroni or
     * BenjaminiHochberg
     * @param maxValue maximum value we're interested in
     * as these cannot possibly be over-represented
     * @param resultsMap map containing unadjusted p-values
     * @return map of all the adjusted p-values
     */
    protected static Map<String, BigDecimal> calcErrorCorrection(String errorCorrection, 
                                                 Double maxValue,
                                                 HashMap<String, BigDecimal> resultsMap) {

        ErrorCorrection e = null;

        if (errorCorrection != null && errorCorrection.equals("Bonferroni")) {
            e = new Bonferroni(resultsMap);
        } else {
            e = new BenjaminiHochberg(resultsMap);
        }
        e.calculate(maxValue);
        return e.getAdjustedMap();
    }
}
