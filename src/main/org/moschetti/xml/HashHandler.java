package org.moschetti.xml;


import org.xml.sax.Attributes;

import java.util.Map;
import java.util.HashMap;
import java.util.Stack;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import java.util.Set;
import java.util.Iterator;
import java.util.Calendar;
import java.util.Date;

import java.math.BigDecimal;

/**
 *  HashHandler implements and extends the standard JDK
 *  org.xml.sax.helpers.DefaultHandler class for XML parsing.  Give it
 *  some "acceptable" XML (XML with good tag nesting and no free characters)
 *  and it will produce a Map of Map of data.  Maps contain a string key and
 *  a value which is either a string, a Map (nesting), or a List (nested 
 *  like-named tags).   CDATA in XML is interpreted simply as a big String,
 *  nothing more.
 *  <pre>
 *  //  Regular stuff:
 *  import javax.xml.parsers.SAXParserFactory;
 *  import javax.xml.parsers.SAXParser;
 *
 *  HashHandler hx = new HashHandler();
 *
 *  SAXParserFactory saxfactory = SAXParserFactory.newInstance();
 *  <b>saxfactory.setNamespaceAware(true); // REQUIRED!  See explain below...</b>
 *  SAXParser saxParser = saxfactory.newSAXParser();
 *
 *  InputStream is = new ByteArrayInputStream(buffer); // or FileInputStream, etc....
 *
 *  saxParser.parse(is, hx);
 * 
 *  Map m1 = hx.getNamespaces();
 *  Map m2 = hx.getRootMap();
 *
 *  // This input....
 *  &lt;?xml version="1.0"?&gt;
 *  &lt;SOAP:Envelope xmlns:SOAP="http://schema.xmlsoap.org/soap/envelope"&gt;
 *    &lt;h:MyHeader xmlns:h="urn:myheader"&gt;
 *      &lt;h:tag1&gt;Vtag1&lt;/h:tag1&gt;	     
 *    &lt;/h:MyHeader&gt;
 *  
 *    &lt;SOAP:Header&gt;
 *  	&lt;SOAP:tag7&gt;Vtag7&lt;/SOAP:tag7&gt;	     
 *    &lt;/SOAP:Header&gt;
 *  
 *    &lt;g:OtherHeader xmlns:g="urn:otherheader"&gt;
 *  	&lt;g:tag4 attr1="val1"&gt;Vtag4-1&lt;/g:tag4&gt;	     
 *  	&lt;g:tag4&gt;Vtag4-2&lt;/g:tag4&gt;	     
 *    &lt;/g:OtherHeader&gt;
 *  
 *    &lt;p:WrapData xmlns:p="outer"&gt;
 *      &lt;p:wrap1&gt;d1&lt;/p:wrap1&gt;
 *  	&lt;p:SomeData attr2="val2" attr3="val3"&gt;
 *  	    &lt;p:InnerData xmlns:p="inner"&gt;
 *  	      &lt;p:inner1&gt;d2-1&lt;/p:inner1&gt;
 *  	    &lt;/p:InnerData&gt;
 *  	    &lt;p:InnerData xmlns:p="inner" attr4="attr4"&gt;
 *  	      &lt;p:inner1&gt;d2-2&lt;/p:inner1&gt;
 *  	    &lt;/p:InnerData&gt;
 *  	&lt;/p:SomeData&gt;
 *    &lt;/p:WrapData&gt;
 *  &lt;/SOAP:Envelope&gt;
 *
 *  // Yields this output (taken from a Map walking util...)
 *  Map m1 (namespaces):
 *    urn:otherheader: [g]
 *    outer:           [p]
 *    inner:           [p]
 *    urn:myheader:    [h]
 *    http://schema.xmlsoap.org/soap/envelope: [SOAP]
 *
 *  Map m2 (data):
 *    Envelope (http://schema.xmlsoap.org/soap/envelope) 5 subs:
 *      __namespace: [http://schema.xmlsoap.org/soap/envelope]
 *      OtherHeader (urn:otherheader) 3 subs:
 *        __namespace: [urn:otherheader]
 *        tag4[0]:
 *          Vtag4-1
 *        tag4[1]:
 *          Vtag4-2
 *        __attributes_tag4[0]:
 *          attr1: [val]
 *        __attributes_tag4[1]:
 *          null
 *      Header (http://schema.xmlsoap.org/soap/envelope) 2 subs:
 *        tag7: [Vtag7]
 *        __namespace: [http://schema.xmlsoap.org/soap/envelope]
 *      WrapData (outer) 4 subs:
 *        wrap1: [d1]
 *        __namespace: [outer]
 *        __attributes_SomeData 2 subs:
 *          attr2: [val2]
 *          attr3: [val3]
 *        SomeData (outer) 3 subs:
 *          __namespace: [outer]
 *          InnerData[0]:
 *            inner1: [d2-1]
 *            __namespace: [inner]
 *          InnerData[1]:
 *            inner1: [d2-2]
 *            __namespace: [inner]
 *          __attributes_InnerData[0]:
 *            null
 *          __attributes_InnerData[1]:
 *            attr4: [attr4]
 *      MyHeader (urn:myheader) 2 subs:
 *        tag1: [Vtag1]
 *        __namespace: [urn:myheader]
 *  </pre>
 *  Important notes:
 *  <ol>
 *  <li><b>You must enable setNamespaceAware(true) on your
 *  <tt>SAXParserFactory saxfactory</tt></b>.  This will not effect
 *  tags without namespaces!  Material will be loaded into Maps 
 *  just the same except no special <tt>__namespace</tt>
 *  entry will be created in the Map.  The logic does not work if
 *  namespace handling is not activated and namespaces hit the handler 
 *  because the internal SAX machinery is watching for the special
 *  attribute <tt>xmlns</tt> and will do clever things re. the local
 *  name, the qualified name, etc.
 *
 *  <li>Like-named peer tags (like <tt>g:tag4</tt> and <tt>p:InnerData</tt>
 *  are turned into Lists of whatever they contain (a value or another 
 *  nested tag) because a HashMap cannot contain 2 or more of the same key name.
 *  For example, <tt>tag4</tt> above would be accessed like this:
 *  <pre>
 *    List l = m2.get("Envelope").get("OtherHeader").get("tag4");
 *  </pre>
 *  but <tt>tag7</tt> (from the SOAP namespace) as a single item in the collection
 *  would be a simple String:
 *  <pre>
 *    String s = m2.get("Envelope").get("Header").get("tag7");
 *  </pre>
 *
 *  <li>Attributes, if encountered, are placed into a map
 *  named <tt>__attributes_{owningTagName}</tt>.  This map will be a <b>peer</b>
 *  to the owningTagName, <b>not a child</b>.   This is because a tag can
 *  have attributes with no nested content, meaning the nested Map would be
 *  be empty (and likely null).  
 *  A Map has
 *  only one domain of data (the key space), not two like XML (attribute names
 *  for a tag are separate and distinct from nested tag names).  This gets
 *  particularly interesting when the List conversion described above takes place.
 *  In this case, a "parallel list" of <tt>__attributes_{owningTagName}</tt> is
 *  created, where <tt>__attributes_{owningTagName}[n]</tt> is a Map of attributes
 *  associated with TagName[n].  This Map will be null if element n of the 
 *  TagName list has no attributes (see InnerData above).
 *
 *  <li>The namespaces map uses the URNs as the key and the prefixes as the data.
 *  This may seem backwards, but remember that the prefixes are arbitrary and
 *  have no meaning except to create uniqueness in the namespace.  For example,
 *  in the tag <tt>&lt;g:tag4&gt;</tt>, it is not the "g" that is important; it
 *  is that <tt>tag4</tt> is associated with namespace <tt>urn:otherheader</tt>,
 *  which we know because the <tt>OtherHeader</tt> map contains a special
 *  entry <tt>__namespace</tt> whose value is <tt>urn:otherheader</tt>.  
 *  </ol>
 *
 */
public class HashHandler extends org.xml.sax.helpers.DefaultHandler {


    /**
     *  State is either accumulating material or not.   Hahahah.  Accumulating
     *  material means picking up characters potentially to assign as the value
     *  to hash entry.
     *
     *  if startTag hit and accumulating = false then
     *      set accumulating = true; set priorTag = thisTag; copy attrs
     *  if startTag hit and accumulating = true then
     *      we have nested tag.  Dump accumulated material, if any.  priorTag
     *      (assume A above) must be a map, not a value.  Make a newNap
     *      and assign it to currentMap keyed by priorTag;
     *      push newMap onto stack and set currentMap = newMap;
     *      set accumulating = false;    i.e.:
     *            Hash newh = new Hash();
     *            currentMap.put(priorTag, newh);
     *            stack.push(currentMap);
     *            currentMap = newh;
     *      Note again that we DUMP accumulated material.   This parser
     *      scheme is for well-formed structured data (well, to the 
     *      degree that XML can be well-formed structured data).  
     *      It does not work for in-line markup e.g.
     *      <foo>
     *         <bar>value1</bar>
     *         Some data here
     *      </foo>
     *      That is perfectly legal XML.  "Some data here" is a text node
     *      attached to a node name.
     *  if endTag hit and accumulating = true then
     *      then we have content.  Assign accumulated material as value for
     *      key endTag in context of currentMap; set accumulating = false
     *  if endTag hit and accumulating = false then
     *      we are ending a nested tag.  Pop hash from the stack and assign
     *      to the currentMap.
     *
     *  Attribute handling is a little more complex.
     */


    private static int[] dm;
    private static int[] smm;
    private static int[] msmm;

    private static int[] tzsmm;
    private static int[] tzmsmm;

    static {
	dm = new int[] { 0,1,2,3,  5,6,  8,9};
	smm = new int[] { 0,1,2,3,  5,6,  8,9,  11,12,  14,15,  17,18 };
	tzsmm = new int[] { 0,1,2,3,  5,6,  8,9,  11,12,  14,15,  17,18, 20,21, 23,24};

	msmm =  new int[] { 0,1,2,3,  5,6,  8,9,  11,12,  14,15,  17,18,  20,21,22};
	tzmsmm = new int[] { 0,1,2,3,  5,6,  8,9,  11,12,  14,15,  17,18,  20,21,22, 24,25, 27,28};

    }
	
    private static final String default_apfx = "__attributes_";

    private String apfx = default_apfx;

    //private static final String apfx = "@";
    //private String apfx = "__attributes_";

    private String makeAttrName(String tag) {
	return apfx + tag;
    }


    private boolean accumulating = false;
	
    private Stack stack;
    private Map root;
    private HashMap namespaces;
	
    private StringBuilder carr;

    private Map priorAttr = null;
    private String priorTag = null;
    private Map currentMap = null;

    private boolean storeBlanks = true;

    private String namespaceField = null; 

    private boolean convertNumbers = false; 
    private boolean useBigDecimal = false; 

    // Why so small?  Because the largest number we support is a long
    // or double.  A long is 18446744073709600000
    // A double is ??? 746354657463546.475645
    private static int NUMBUF_SIZE = 512;
    private char[] numbuf = new char[NUMBUF_SIZE];

    private boolean convertDates = false; 

    private Class mapType;



    private void init(boolean storeBlanks, Class mapType) {
	
	this.mapType = mapType;

	try {
	    root = (Map) this.mapType.newInstance();
	} catch(Exception e) {
	    // ?!?  TBD here...
	}

	// root.put("__name", "root"); // MARK

	stack = new Stack();

	carr = new StringBuilder();

	namespaces = new HashMap();
	priorAttr = new HashMap();

	currentMap = root;

	this.storeBlanks = storeBlanks;
    }

    /**
     *  By default, HashHandler will store maps of blank strings when
     *  given things like this:
     *     <tag></tag>   or    <tag/>
     *  This will resolve in the containing map to:
     *     String s = map.get("tag"); // s is ""
     */
    public HashHandler() {
	init(true, java.util.HashMap.class);
    }

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
     *  If storeBlanks is false, then blank tags will not be stored in the 
     *  containing map.  Attributes in blank tags WILL be stored, e.g.:
     *     <tag attr1="foo" />
     *  Map attrs = map.get("__attributes_tag"); // attrs["attr1"] = "foo"
     *
     *
     */
    public HashHandler(boolean storeBlanks) {
	init(storeBlanks, java.util.HashMap.class);
    }


    /**
     *  Tell us what type of Maps you want
     */
    public HashHandler(boolean storeBlanks, Class mapType) {
	init(storeBlanks, mapType);
    }



    public void emitNamespace(String namespaceField) {
	this.namespaceField = namespaceField;
    }

    /**
     *  If convertNumbers is true, items that sniff like numbers will
     *  be turned into int, long, or double
     */
    public void convertNumbers(boolean yorn) {
	this.convertNumbers = yorn;
    } 

    /**
     *  If useBigDecimal is true, items that sniff like floats will
     *  be turned into BigDecimal instead of Double.
     */
    public void useBigDecimal(boolean yorn) {
	this.useBigDecimal = yorn;
    } 

    public void convertDates(boolean yorn) {
	this.convertDates = yorn;
    }


    public void setAttributePrefix(String apfx) {
	if(apfx == null) {
	    this.apfx = default_apfx;
	} else {
	    this.apfx = apfx;
	}
    }



    public Map<String,Object> getRootMap() {
	return (Map<String,Object>)root;
    }
    public Map getNamespaces() {
	return namespaces;
    }

	
    
    public void characters(char[] ch, int start, int len) {
	//System.out.println("** chars " + len + "; accum = " + accumulating);

	if(accumulating) {  // ignore random chars outside the start-end state machine...
	    carr.append(ch, start, len); // ah!  nice!
	}
    }
	

    /**
     *  This will FIRST clear the target map, then copy in attribute values.
     *  If Attributes a has length 0, then no problem; the target map will be
     *  of size 0.
     */
    private static void saveAttrs(Map m, Attributes a) {
	m.clear();

	int n = a.getLength();
	for(int kk = 0; kk < n; kk++) {
	    String ln = a.getLocalName(kk);
	    String vv = a.getValue(kk);
	    //String uri = a.getURI(kk);

	    m.put(ln, vv);

	    /**
	     *   TBD TBD TBD   What to do with namespaces on attrs?
	    if("".equals(uri)) {
		m.put(ln, vv);
	    } else {

		// Must construct a namespace-safe rep!
		StringBuilder sb = new StringBuilder();
		sb.append(vv);
		sb.append(":");
		sb.append(uri);
		m.put(ln, sb.toString());
	    }
	    */

	}
    }




    /**
     *  attrs can be null and may need to be if attributes have been List-ified
     *
     *  You cannot call putAttrsInMap until AFTER the peer data has been placed
     *  into the target Map.   This function uses the data peer as a "guide" to
     *  see if the attribute should be set up at all.
     */
    private void putAttrsInMap(Map target, String tag, Map attrs) {
	
	String atag = makeAttrName(tag);

	/**
	 *  We only only put attrs in the map when they appear.  It is 
	 *  possible that in a List situation, several peer data Maps have
	 *  already been "List-ified" and thus the attrs have to "catch up."
	 *  For example, if the peer data List is length 4 and our attr List
	 *  is null, then we have to "backfill" the attr List with nulls for
	 *  items 0, 1, and 2.
	 */
	Object o = target.get(atag);
	
	Object o2 = target.get(tag); // The peer data to the attr

	if(o == null) {

	    // First time in, i.e. no attrs exist at all....

	    if(attrs != null) {

		if(o2 == null) {
		    // tag is empty but has attribute, e.g.:
		    // <foo attr="value" />

		    setMap("P2", target, atag, attrs);

		} else if(o2 instanceof List) {

		    List a = new ArrayList();
		    
		    // The data has been List-ified.  Get length
		    // and fill an attr list MINUS ONE:
		    
		    int len = ((List)o2).size();
		    
		    for(int kk = 0; kk < len - 1; kk++) {
			a.add(null);
		    }
		    
		    a.add(attrs);
		    
		    setMap("Q", target, atag, a);
		    
		} else { 
		    
		    // o2 is either a Map or a String; either way, it
		    // is a SINGLE.  But here, if attr is null, then 
		    // we do NOT put it in the map.  Single entries
		    // do NOT need placeholders like Lists...
		    
		    setMap("P", target, atag, attrs);
		}
	    }


	} else { // o != null therefor attrs exist

	    if(o instanceof List) {
		// Already List-ified.  Simply add attrs, even
		// if they are null:

		((List)o).add(attrs);

	    } else if(o instanceof Map) { 

		// Singleton exists.   Need to List-ify it.

		List a = new ArrayList();

		a.add(o); 
		a.add(attrs);

		setMap("R", target, atag, a);
	    }
	}
    }



    public void startElement(String uri, String localName, String qName, Attributes attr) {
	//System.out.println("** startElement " + uri + " " + localName + "; attrs " + attr.getLength() + "; accum = " + accumulating);

	if(accumulating == false) {
	    accumulating = true;
	    priorTag = localName; 

	    //System.out.println("  ++ copy attrs for " + localName + " to priorAttr");
	    saveAttrs(priorAttr, attr);
    

	} else {   // accumulating = true thus we got a <start> within another <start>

	    carr.setLength(0); // dump any accumulated stuff...

	    Map newMap = null;

	    try {
		newMap = (Map) this.mapType.newInstance();
	    } catch(Exception e) {
		// ???  TBD Need to do something here...
	    }

	    // newMap.put("__name", priorTag); // MARK
	    
	    // System.out.println("current map is " + currentMap.get("__name") + ", namespace " + currentMap.get("__namespace") + "; " + priorTag + " will be new map " + newMap.get("__name") + " in it.");

	    Object o = currentMap.get(priorTag);

	    if(o != null) {

		//System.out.println("!!! dupe detected for " + priorTag);

		if(o instanceof List) {
		    ((List)o).add(newMap);

		} else {   // was if(o instanceof Map) {
		    
		    // Not yet rejiggered!
		    //System.out.println("!!! is Map; must rejigger");
		    
		    List v = new ArrayList();
		    //List v = new Vector();
		    
		    v.add(o);  // it could be a Map OR a simple String!
		    v.add(newMap);
		    
		    //currentMap.put(priorTag, v);
		    setMap("C", currentMap, priorTag, v);

		}

	    } else {

		//currentMap.put(priorTag, newMap); 	   
		setMap("F", currentMap, priorTag, newMap); 	   
	    }



	    Map safe_prior_attrs = null;

	    if(priorAttr.size() > 0) {
		// Overwrite null with new map....
		safe_prior_attrs = new HashMap(priorAttr);		
	    }

	    // OK to call this with safe_prior_attrs = null or not:
	    putAttrsInMap(currentMap, priorTag, safe_prior_attrs);

	    saveAttrs(priorAttr, attr); // Get the NEW attr for this tag

	    stack.push(currentMap);

	    currentMap = newMap;

	    //System.out.println("current map is now " + currentMap.get("__name"));
	    //System.out.println("\n");

	    priorTag = localName; // new String(localName);  // TBD ... copy...?
	}
    }
	

    public void endElement(String uri, String localName, String qName) {
	//System.out.println("** endElement " + uri + " " + localName + "; accum = " + accumulating);

	if(accumulating == true) {
	    //
	    //  This is a scalar end tag, i.e. where the prior tag is a matching
	    //  start tag:
	    //                    <a>value</a>
	    //
	    Object value = null;

	    if(carr.length() > 0 || storeBlanks == true) {

		Object o = currentMap.get(priorTag);

		String s6 = carr.toString();

		/*
		  ATTENTION!
		  Because we cannot guarantee that 20160405 is YYYYMMDD
		  vs. YYYYDDMM, we will let convertNumbers gobble it up!
		  The Date sniffer will look for YYYY-MM-DD which clearly
		  is not an int!
		*/
		if(convertNumbers) {
		    boolean isNumber = true;
		    boolean isFloat = false;

		    int end = carr.length();
		    if(end > NUMBUF_SIZE) {
			numbuf = new char[end + 1];
		    }
		    
		    carr.getChars(0, end, numbuf, 0);

		    //System.out.println("candidate: " + carr);

		    boolean foundDot = false;

		    int jj = 0;
		    int start = 0;

		    if(numbuf[0] == '-') {
			if(end == 1) { // end is length, NOT pos...  
			    // Just a dash; not a number!
			    isNumber = false;
			} else {
			    jj = 1;
			    start = 1;
			}
		    }

		    for(; jj < end; jj++) {
			if(numbuf[jj] == '.') {
			    if(foundDot == true) { // more than one dot!
				//System.out.println("  BAD more than one dot");
				isNumber = false;
				break;
			    }
			    foundDot = true;
			    isFloat = true;
			} else if(numbuf[jj] < '0' || numbuf[jj] > '9') {
			    //System.out.println("  BAD not 0-9");
			    isNumber = false;
			    break;
			}
		    }

		    // If isNumber = true, then double check that
		    // we don't have leading zero, e.g.
		    // 0        // OK!  Just integer 0
		    // 00232    // bad; treat as string
		    // 00.232   // bad; treat as string
		    // 0.123,   // OK! treat as float
		    // .123,    // OK! treat as float
		    // .        // bad; treat as string
		    if(isNumber == true) {
			if(numbuf[start] == '0' && end > 1 && numbuf[start+1] != '.') {
			    //System.out.println("  BAD leading zeroes");
			    isNumber = false;
			    isFloat = false;
			} else if(numbuf[start] == '.' && end == 1) {
			    //System.out.println("  BAD solo dot");
			    isNumber = false;
			    isFloat = false;
			}
		    }

		    if(isNumber == true && isFloat == true) {
			try {
			    if(useBigDecimal) {
				value = new BigDecimal(s6);
			    } else {
				value = Double.parseDouble(s6);
			    }

			} catch(Exception e) {
			    System.out.println("float parse failure, tag " + localName);
			}
			//System.out.println("  OK double: " + value);

		    } else if(isNumber == true) {
			try {
			    double dd4 = Double.parseDouble(s6);

			    if(dd4 > Long.MAX_VALUE || dd4 < Long.MIN_VALUE) {
				value = dd4;
			    } else {
				if(dd4 > Integer.MAX_VALUE || dd4 < Integer.MIN_VALUE) {
				    value = Long.parseLong(s6);
				} else {
				    value = (int)dd4;
				}
				//Integer.parseInt(s6);
			    }
			    //System.out.println("  OK int: " + value);
			} catch(Exception e) {
			    System.out.println("double/long parse failure of [" + s6 + "], tag " + localName);
			}
		    } 

		}
		
		if(value == null && convertDates == true) {
		    value = sniffString(s6);
		}

		if(value == null) {
		    value = s6;
		}

		if(o != null) {
		    // System.out.println("!!! dupe detected for " + localName);

		    // Need to List-ify or not, but either way, you must add
		    // a real value or null (empty tag)

		    if(o instanceof List) {
			((List)o).add(value); // was localName?!?

		    } else {
			//System.out.println("!!! is not List; must rejigger");

			List v = new ArrayList();
			//List v = new Vector();
		    
			v.add(o);
			v.add(value);
		    
			//currentMap.put(localName, v);
			setMap("G", currentMap, localName, v);
		    }
		
		} else { // Not a dupe.
		
		    //currentMap.put(localName, value);
		    setMap("H", currentMap, localName, value);
		}

	    }


	    if(priorAttr.size() > 0) {
		// Must make a copy!  Be careful!
		Map am = new HashMap(priorAttr);
		putAttrsInMap(currentMap, localName, am);
	    } else {
		putAttrsInMap(currentMap, localName, null);
	    }

	    priorAttr.clear();
		
	    accumulating = false;
	    carr.setLength(0);

	} else {  // accumulating = false 

	    //
	    //  This is a structure end tag, i.e. where the prior tag is not a matching
	    //  end tag:
	    //          <a>
	    //             <b>value</b>
	    //          </a>	    
	    //
	    if(namespaceField != null) {
		if(uri != null && !uri.equals("")) {
		    currentMap.put(namespaceField, uri); 
		}
	    }

	    // System.out.println("set map " + currentMap.get("__name") + " to namespace " + currentMap.get("__namespace"));

	    priorAttr.clear();

	    currentMap = (Map)stack.pop();

	    // System.out.println("popping; current map is now: " + currentMap.get("__name"));
	}
    }

	
    public void startPrefixMapping(String prefix, String uri) {
	//System.out.println("map [" + prefix + "] to " + uri);

	// uri is the key, NOT the prefix.   We sort of do not care about
	// the prefix.  In fact, since prefixes can eclipse each other,
	// you can have more than one uri mapping to a single prefix!  For
	// example, the following XML is perfectly legal:
	//
	//  <p:WrapData xmlns:p="outer">
	//    <p:wrap1>d1</p:wrap1>
	//    <p:InnerData xmlns:p="inner">
	//      <p:inner1>d2</p:inner1>
	//    </p:InnerData>
	//  </p:WrapData>
	//
	//  The startTag event on InnerData will "push the stack" and locally
	//  reassign prefix p to "inner", thus overriding the assignment of 
	//  p to "outer" in WrapData.
	//
	namespaces.put(uri, prefix);
    }





    // ------------------------------------------------------------
    //  Below here is debug and print stuff; not really part of imp
    // ------------------------------------------------------------


    private static void emit(int level, String msg) {
	for(int kk = 0; kk < level; kk++) {
	    System.out.print("  ");
	}
	System.out.println(msg);
    }


    private static void showMap(Map m, String ctx, int level) {
	Set s = m.keySet();

	if(s.size() == 0) {
	    System.out.println(ctx + ": map " + m + " is empty");

	} else {
	    String key;

	    if((key = (String) m.get("__name")) != null) {
		emit(level, ctx + ":" + key);
	    }

	    Iterator ii;

	    // FOr ease of reading, 2 iters:  First gets keys,
	    // second recurses....
	    ii = s.iterator();
	    while(ii.hasNext()) {
		key = (String) ii.next();
		Object o = m.get(key);		
		if(o instanceof String) {
		    String val = (String)o;
		    if(!val.equals("__name")) {
			emit(level, ctx + ":" + key + ": [" + val + "]");
		    }
		}
	    }


	    ii = s.iterator();
	    while(ii.hasNext()) {
		key = (String) ii.next();
		Object o = m.get(key);
		if(o instanceof Map) {
		    Map sm = (Map)o;
		    // emit(level, ctx + ": map " + sm.get("__name") + "; recurse...");
		    showMap((Map)o, ctx, level+1);

		} else if(o instanceof List) {
		    List sl = (List)o;
		    int n = sl.size();
		    for(int kk = 0; kk < n; kk++) {
			Map sm = (Map)sl.get(kk);
			showMap(sm, ctx, level);
		    }
		} 
	    }
	}
    }
    

    private static void setMap(String marker, Map m, String key, Object value) {

	m.put(key, value);

	if(false) { // DEBUG...
	    String s;

	    if((s = (String) m.get("__name")) == null) {
		s = "";
	    }
	    
	    if(value == null) {
		System.out.println(" @@ " + marker + " setMap " + s + "(" + key + ", null)");
	    } else {
		if(value instanceof String) {
		    System.out.println(" @@ " + marker + " setMap " + s + "(" + key + ",\"" + (String)value + "\")");
		} else if(value instanceof Map) {
		    System.out.println(" @@ " + marker + " setMap " + s + "(" + key + ", Map " + ((Map)value).get("__name") + ")");
		} else {
		    System.out.println(" @@ " + marker + " setMap " + s + "(" + key + "," + value.getClass().getName() + ")");
		}
	    }

	    //dumpMap(m);
	}
    }

    private static void dumpMap(Map m) {
	Set s = m.keySet();
	Iterator ii = s.iterator();

	int i = 0;

	while(ii.hasNext()) {
	    String key = (String) ii.next();

	    Object o4 = m.get(key);

	    if(o4 instanceof String) {
		System.out.println(i + ": " + key + ": string [" + (String)o4 + "}");
	    } else {
		System.out.println(i + ": " + key + ": " + o4.getClass().getName());
	    }
	    i++;
	}
    }


    private static Date sniffString(String s) 
    {
	Date d = null;

	//   Thank goodness all these formats have unique lengths!!!

	//   YYYY-MM-DD                 length is 10

	//   YYYY-MM-DDThh:mm:ss        length is 19
	//   YYYY-MM-DDThh:mm:ssZ       length is 20
	//   YYYY-MM-DDThh:mm:ss+hh:mm  length is 25

	//   YYYY-MM-DDThh:mm:ss.sss    length is 23
	//   YYYY-MM-DDThh:mm:ss.sssZ   length is 24
	//   YYYY-MM-DDThh:mm:ss.sss+hh:mm  length is 29

	// s[10] must be 'T'
	if(s != null) {
	    int l = s.length();

	    boolean pass1 = false;
	    int[] xmm = null;

	    int tzhrs = -1; // i.e. unset
	    int tzmin = -1; // i.e. unset

	    if(l == 10) {
		if(s.charAt(4) == '-'
		   && s.charAt(7) == '-') {

		    xmm = dm;

		    pass1 = true;
		}
	    
	    } else if(l == 19 || l == 20 || l == 25
		      || l == 23 || l == 24 | l == 29) {

		if(s.charAt(4) == '-'
		    && s.charAt(7) == '-'
		    && s.charAt(10) == 'T'
		    && s.charAt(13) == ':'
		    && s.charAt(16) == ':') {

		    if(l == 19) {
			xmm = smm;
			pass1 = true;

		    } else if(l == 20) {
			xmm = smm;
			if(s.charAt(19) == 'Z') {
			    tzhrs = 0; // Z means assume 00:00 offset
			    tzmin = 0;
			    pass1 = true;
			} // Not 'Z' so not valid

		    } else if(l == 25) {
			xmm = tzsmm;
			if((s.charAt(19) == '+' || s.charAt(19) == '-')
			   && s.charAt(22) == ':') {
			    pass1 = true;
			} // Not '+-' and ':' so not valid			


			// ms variants:
		    } else if(l == 23) {
			xmm = msmm;
			pass1 = true;

		    } else if(l == 24) {
			xmm = msmm;
			if(s.charAt(23) == 'Z') {
			    tzhrs = 0; // Z means assume 00:00 offset
			    tzmin = 0;
			    pass1 = true;
			}

		    } else if(l == 29) {
			xmm = tzmsmm;
			if((s.charAt(23) == '+' || s.charAt(23) == '-')
			   && s.charAt(26) == ':') {
			    pass1 = true;
			}
		    }
		}
	    } 

	    if(pass1 == true) {
		// Basic skeleton of delims is correct, so
		// check for everything else being between 0 and 9:
		boolean restAreNums = true;

		for(int j = 0; j < xmm.length; j++) {
		    char c = s.charAt(xmm[j]);
		    if(c < '0' || c > '9') {
			restAreNums = false;
			break;
		    }
		}
		
		if(restAreNums == true) {
		    
		    // Structural OK; try to build a real Date
		    int year = 0;
		    int month = 0;
		    int day = 0;
		    
		    int hour = 0;
		    int min = 0;
		    int sec = 0;
		    int msec = 0;

		    int mult = 1;
		    
		    year  = buildNumber(s, 0, 4);
		    month = buildNumber(s, 5, 2);
		    day   = buildNumber(s, 8, 2);

		    if(l != 10) {
			hour  = buildNumber(s, 11, 2);
			min   = buildNumber(s, 14, 2);
			sec   = buildNumber(s, 17, 2);
		    
			if(l == 23 || l == 24 || l == 29) {
			    msec = buildNumber(s, 20, 3);
			} 

			if(l == 25) {
			    tzhrs = buildNumber(s, 20, 2);
			    tzmin = buildNumber(s, 23, 2);

			    if(s.charAt(19) == '-') {
				mult = -1;
			    }

			} else if(l == 29) {
			    tzhrs = buildNumber(s, 24, 2);
			    tzmin = buildNumber(s, 27, 2);

			    if(s.charAt(23) == '-') {
				mult = -1;
			    }
			}
		    }
		    
		    /********
			     System.out.println("got here with " + s);
			     System.out.println("ymd hms: " + year
			     + "," + month
			     + "," + day
			     + "," + hour
			     + "," + min
			     + "," + sec
			     + "," + msec);
		    ********/

		    Calendar cal = Calendar.getInstance();
		    cal.clear();

		    // If we explicitly set up GMT offset, then FORCE the Calendar
		    // object into GMT mode.   Otherwise, when we do cal.getTime() later
		    // the local time JVM offset will automagically be added in!  Yuck
		    if(tzhrs != -1 || tzmin != -1) {
			cal.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
		    }

		    /*
		      public void setTimeZone(TimeZone value)

		     */

		    cal.set( Calendar.YEAR,  year );
		    cal.set( Calendar.MONTH, month - 1); // YOW!
		    cal.set( Calendar.DATE,  day ); 
		    cal.set( Calendar.HOUR_OF_DAY, hour ); 
		    cal.set( Calendar.MINUTE, min );
		    cal.set( Calendar.SECOND, sec );
		    cal.set( Calendar.MILLISECOND, msec ); 	    
		    
		    // Still kinda unsure about all this...
		    if(tzhrs != -1) {
			cal.add( Calendar.HOUR_OF_DAY, tzhrs * mult);
		    }
		    if(tzmin != -1) {
			cal.add( Calendar.MINUTE, tzmin * mult);
		    }

		    d = cal.getTime();					
		}
	    }
	}
	return d;
    }

    // buildNumber(s, 0, 4);
    private static int buildNumber(String s, int idx, int len) {
	int item = 0;

	// k is going 0 to len-1. i.e. 0 to 3 for year...
	for(int k = 0; k < len; k++) {

	    // Given 2013, jump to 3 
	    int nx = idx + (len-1) - k;

	    char c = s.charAt(nx);
	    switch(k) {
	    case 3:
		item += (c - '0') * 1000;
		break;
	    case 2:
		item += (c - '0') * 100;
		break;
	    case 1:
		item += (c - '0') * 10;
		break;
	    case 0:
		item += (c - '0');
		break;
	    }
	}

	return item;
    }



}
