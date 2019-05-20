package org.moschetti.xml;

/**
 *  One of the reasons we love SAX:  No 3rd party libs!
 */
import javax.xml.parsers.SAXParserFactory; 
import javax.xml.parsers.SAXParser;

import org.xml.sax.Locator;
import org.xml.sax.helpers.LocatorImpl;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 *  Primary to/from XML mapper.
 *  Responsible for creating and digesting MapOfMaps containing the
 *  following types:<br>
 *      String, Integer, Long, Double / BigDecimal, Map, or List<br>
 *
 *  Other fmt converters like JSONUtils need to do the same.
 *  
 */
public class XMLUtils {

    /**
     *  By default, all boolean options are false, strings are null,
     *  and integers are 0.  The options are designed to be selectively
     *  "turned on" as opposed to turned off, so you can create a
     *  ParseOptions object and just set 1 or 2 items to true for
     *  convenience.
     */
    public static class ParseOptions {

	/**
	 *  If storeBlanks is true, HashHandler will store maps of blank strings when
	 *  given things like this:
	 *     <tag></tag>   or    <tag/>
	 *  This will resolve in the containing map to:
	 *     String s = map.get("tag"); // s is ""
	 *  This can lead to "sparse" maps -- maps with a lot of keys but not
	 *  a lot of REAL data -- but the "advantage" is that you know
	 *  what tags came in.
	 *
	 *  If storeBlanks is false (default), then blank tags will not be
	 *  stored in the containing map.  Attributes in blank tags WILL be
	 *  stored, e.g.:
	 *     <tag attr1="foo" />
	 *  Map attrs = map.get("__attributes_tag"); // attrs["attr1"] = "foo"
	 *
	 */
	public boolean storeBlanks;

	/**
	 *  If true, data that sniffs like numbers will be converted into
	 *  Integer, Long, or Double / BigDecimal.   See useBigDecimal
	 */
	public boolean convertNumbers;

	/**
	 *  If true, data that sniffs like a date will be converted into
	 *  a java.util.Date
	 */
	public boolean convertDates;

	/**
	 *  (Only applies when convertNumbers is true) If true, data that
	 *  sniffs like a float will be converted into java.math.BigDecimal
	 *  instead of Double.  
	 */
	public boolean useBigDecimal;

	/**
	 *  Attributes for a tag are collapsed into the same space as
	 *  the data, prefixed with the string set here.  "@" is a very
	 *  popular prefix.
	 */
	public String attributePrefix;

	/**
	 *  If set, a field so named will be created in the root map
	 *  containing the runtime of the of the parse in nanoseconds.
	 */
	public String parseTimeInNanosField;

	/**
	 *  If set, a field so named will be created in each map
	 *  containing the URI as it was presented in the enclosing
	 *  XML tag.
	 */
	public String namespaceField;

    }


    /**
     *  Only need a single FACTORY for all threads.
     *  But you'll need a new parser instance for each whack at the pinata
     */
    private static final SAXParserFactory saxfactory;



    static {
	saxfactory = SAXParserFactory.newInstance();
	saxfactory.setNamespaceAware(true);
    }


    public static Map<String,Object> parseXML(InputStream is) 
	throws XMLParsingException
    {
	ParseOptions options = new ParseOptions();
	return parseXML(is, options);
    }


    public static Map<String,Object> parseXML(InputStream is, ParseOptions options)
	throws XMLParsingException
    {
	Map data = parseXML(is, HashMap.class, options);
	return data;
    }


    /**
     *  Declare the kind of concrete imp of Map you want the engine to use.
     *  HashMap.class is an easy one.  LinkedHashMap.class will preserve
     *  order of entry; thus the output map will "look" like the incoming
     *  XML without the tags being rearranged
     */
    public static Map<String,Object> parseXML(InputStream is, Class mapType)
	throws XMLParsingException
    {
	ParseOptions options = new ParseOptions();
	Map data = parseXML(is, mapType, options);
	return data;
    }


    /**
     *  Declare the kind of concrete imp of Map you want the engine to use.
     *  HashMap.class is an easy one.  LinkedHashMap.class will preserve
     *  order of entry; thus the output map will "look" like the incoming
     *  XML without the tags being rearranged
     */
    public static Map<String,Object> parseXML(InputStream is, Class mapType, ParseOptions options)
	throws XMLParsingException
    {
	Map<String,Object> data = null;

	Locator locator = new LocatorImpl();

	try {
	    SAXParser saxParser = saxfactory.newSAXParser();

	    HashHandler hx = new HashHandler(options.storeBlanks, mapType);

	    hx.setDocumentLocator(locator);

	    if(options.attributePrefix != null) {
		hx.setAttributePrefix(options.attributePrefix);
	    }
	    if(options.namespaceField != null) {
		hx.emitNamespace(options.namespaceField);
	    }

	    hx.convertNumbers(options.convertNumbers);
	    hx.convertDates(options.convertDates);
	    hx.useBigDecimal(options.useBigDecimal);

	    long startTime = 0;
	    long endTime = 0;

	    if(options.parseTimeInNanosField != null) {
		startTime = System.nanoTime();
	    }
	    saxParser.parse(is, hx); // The Juice!
	    if(options.parseTimeInNanosField != null) {
		endTime = System.nanoTime();
	    }

	    //System.out.println("namespaces:");
	    //m = hx.getNamespaces();
	    //showMap(m, 0, showAttrs);

	    //System.out.println("data:");	    

	    data = hx.getRootMap();

	    if(options.parseTimeInNanosField != null) {
		long nanos = (endTime - startTime);
		data.put(options.parseTimeInNanosField, nanos);
	    }

	} catch(Exception e) {
	    int cn = locator.getColumnNumber();
	    int ln = locator.getLineNumber();

	    String msg = "parsing/conversion failure near column " + cn + ", line " + ln + ": " + e;

	    //throw new XMLParsingException("cannot parse stream: " + e);
	    throw new XMLParsingException(msg);
	}

	return data;
    }



    public static void writeXMLHeader(OutputStream out)
	throws IOException
    {
	out.write("<?xml version=\"1.0\"?>\n".getBytes());
    }


    public static void writeXML(OutputStream out, Map<String,Object> xmap, String enclosingTag)
    {
	try {

	    Map2XML.writeXML(out, xmap, enclosingTag);

	} catch(Exception e) {
	    System.out.println("epic fail on writeXML: " + e);

	}
    }


    /**
     *  Map to XML presents a problem with structured data and lists
     *  of maps.   In structured data, a list is list, not a 
     *  repeating set of tags with the same name.  For example:
     *  <pre>
     *  Map / JSON                 XML
     *  --------------------       -------------------------
     *  {                          &lt;ROOT&gt;
     *     items = [                 &lt;items&gt;	    
     *        {a="v1", b="v2"},        &lt;item&gt;	    
     *        {a="v3", b="v4"},          &lt;a&gt;v1&lt;/a&gt;
     *     ]                             &lt;b&gt;v2&lt;/b&gt;
     *  }                              &lt;/item&gt;    
     *                                 &lt;item&gt;	    
     *                                   &lt;a&gt;v3&lt;/a&gt;
     *                                   &lt;b&gt;v4&lt;/b&gt;
     *                                 &lt;/item&gt;    
     *                               &lt;/items&gt;     
     *                             &lt;/ROOT&gt;
     *  </pre>
     *  There are several challenges:
     *  <ol>
     *  <li>XML requires a named root element
     *  <li>For the XML to be friendly, it must indicate that item
     *      is part of a list (repeating tags with no enclosing
     *      wrapper is considered unfriendly because the existence
     *      of a single item can be misinterpreted as single item
     *      instead of a List container!
     *  <li>There is no straightforward way to "invent" the names
     *      of the in-List tags (here we stripped the s off the end
     *      to depluralize it but that is conpletely case specific)
     *  </ol>
     * 
     *  The Map2XML() function will handle this by changing the
     *  name of the wrapper to be <i>name</i>_LIST:
     *  <pre>
     *  &lt;ROOT&gt;
     *    &lt;items_LIST&gt;	    
     *      &lt;items&gt;	    
     *        &lt;a&gt;v1&lt;/a&gt;
     *        &lt;b&gt;v2&lt;/b&gt;
     *      &lt;/items&gt;    
     *      &lt;items&gt;	    
     *        &lt;a&gt;v3&lt;/a&gt;
     *        &lt;b&gt;v4&lt;/b&gt;
     *      &lt;/items&gt;    
     *    &lt;/items_LIST&gt;     
     *  &lt;/ROOT&gt;
     *  </pre>
     *  deListify() assumes an inbound structure like the above and
     *  upon finding Map elements <i>name</i>_LIST, if sees a single child
     *  element <i>name</i> of type List, then it will "promote" the 
     *  child list into the grandparent array as <i>name</i> and remove the
     *  <i>name</i>_LIST map.   In other words, this:
     *  <pre>
     *  parent: 
     *    items_LIST: java.util.HashMap
     *      items: java.util.ArrayList
     *        0: java.util.HashMap
     *          a: java.lang.String: v1
     *          b: java.lang.String: v2
     *        1: java.util.HashMap
     *          a: java.lang.String: v3
     *          b: java.lang.String: v4
     *  </pre>
     *  becomes
     *  <pre>
     *  parent: 
     *    items: java.util.ArrayList
     *      0: java.util.HashMap
     *        a: java.lang.String: v1
     *        b: java.lang.String: v2
     *      1: java.util.HashMap
     *        a: java.lang.String: v3
     *        b: java.lang.String: v4
     *  
     *  </pre>
     */
    public static void deListify(Map<String,Object> xmap)
    {
	walkMap(xmap);
    }


    private static void walkMap(Map<String,Object> m)
    {
	/**
	 *  Need to use String[] here, not iterator, because we will be
	 *  inserting and deleting from the Map.   We need a start of
	 *  function "point in time" capture of the keyset, not an
	 *  iteration thru it....
	 */
	Object[] keys = m.keySet().toArray(); // handy!
	
	int max = keys.length;
	//java.util.Iterator<String> ii = m.keySet().iterator();

	for(int idx = 0; idx < max; idx++) {

	    String key = (String) keys[idx];
	    Object ov = m.get(key);

	    if(ov != null) {

		if(ov instanceof Map) {
		    if(key.endsWith("_LIST")) {

			//System.out.println("target " + key);

			Map sub = (Map)ov; // dip in

			if(sub != null && sub.size() == 1) {

			    //  If key was claims_LIST, then
			    //  z is claims

			    int zn = key.indexOf("_LIST");
			    String z = key.substring(0, zn);
			    
			    Object ov2 = sub.get(z);

			    if(ov2 != null && ov2 instanceof List) {
				m.put(z, ov2);
				m.remove(key);
				//System.out.println("promote " + key + " to " + z);
			    }
			}

		    } else {
			walkMap((Map)ov);
		    }

		} else if(ov instanceof List) {
		    walkList((List)ov);
		}
	    }
	}
    }


    private static void walkList(List l)
    {
	for(int jj = 0; jj < l.size(); jj++) {
	    Object ov = l.get(jj);
	    
	    if(ov != null) {
		if(ov instanceof Map) {
		    walkMap((Map)ov);
		} else if(ov instanceof List) {
		    walkList((List)ov);
		} else {
		    // No need to worry about scalars (for once...!)
		}
	    }
	}
    }

}
