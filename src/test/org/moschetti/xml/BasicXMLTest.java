package test;

import org.moschetti.xml.XMLUtils;

import org.junit.Test;
import org.junit.Before;
import org.junit.Assert;

import java.math.BigDecimal;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import java.io.InputStream;
import java.io.FileInputStream;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;

public class BasicXMLTest {
    private int s;


    //@Test
    public void toXML() {

	Map<String,Object> a1 = new HashMap<String,Object>();
	a1.put("a1-n1", "v1");

	// Yep.  This appears in the input map as a Date, but upon round
	// trip ( Map -> JSON -> Map) it will be rehydrated as a Long.
	a1.put("a1-d1", new java.util.Date());

	Map a2 = new HashMap();

	List<Map> l1 = new java.util.ArrayList<Map>();
	for(int j = 0; j < 2; j++) {
	    Map a3 = new HashMap();
	    a3.put("a3", "s1");
	    a3.put("a4", 12 + j);
	    a3.put("a5", 3.14159 * (j+1));
	    a3.put("a6", "cats & dogs");
	    l1.add(a3);
	}
	a2.put("a2-n2", l1); // list l1 into a2-n2
	a2.put("a2-n1", "corn"); // string "corn" into a2-n1
	a1.put("a1-n3", a2);

	a1.put("a1-int1", 238473);
	a1.put("a1-long1", 1366754049390L);
	a1.put("a1-bigd1", new BigDecimal("24.10"));
	a1.put("a1-bigd2", new BigDecimal("-.001"));

	a1.put("a1-yikes1", "Buzz & Avi");
	a1.put("a1-yikes2", "1 is < 2 but > 0");

	a1.put("a1-nasties", "%^&#@$!~\\\"\'<<>>");

	/**
	 *  TBD:   $ and other nasties will cause a fail
	 *  for XML as a key name
	 */
	/*******
	a1.put("$foo", "bar");
	a1.put("f$oo", "bar");
	a1.put("foo$", "bar");
	*******/

	List<String> l2 = new java.util.ArrayList<String>();
	for(int j = 0; j < 2; j++) {
	    l2.add("scalar" + j);
	}
	a2.put("a1-n9", l2);

	walkMap(a1, 0);

	try {
	    ByteArrayOutputStream b = new ByteArrayOutputStream();

	    XMLUtils.writeXMLHeader(b);
	    XMLUtils.writeXML(b, a1, "envelope");

	    String ss = b.toString();
	    System.out.println("XMLUtils.writeXML():");
	    System.out.println(ss);

	    System.out.println("\n");
	    
	    InputStream is = new ByteArrayInputStream(ss.getBytes());

	    XMLUtils.ParseOptions xx = new XMLUtils.ParseOptions();
	    xx.attributePrefix = "@";
	    xx.convertNumbers = true;
	    xx.useBigDecimal = true;
	    xx.convertDates = true;
	    xx.parseTimeInNanosField = "__parseTime";

	    Map<String,Object> qq = XMLUtils.parseXML(is, xx);
	    System.out.println("from XML");
	    walkMap(qq, 0);

	    System.out.println("\n");

	    XMLUtils.deListify(qq);
	    System.out.println("after deListify()");
	    walkMap(qq, 0);

	    
	} catch(Exception e) {
	    System.out.println("epic fail: " + e);
	    e.printStackTrace();
	}
    }

    private static void writeString(OutputStream out, String s) {
	try {
	    out.write(s.getBytes());
	} catch(Exception e) {
	    System.out.println("epic fail: " + e);
	    e.printStackTrace();
	}
    }



    //@Test
    public void singleTags() {
	try {
	    ByteArrayOutputStream b = new ByteArrayOutputStream();
	    XMLUtils.writeXMLHeader(b);

	    writeString(b, "<data>");
	    writeString(b, "  <justOne/>");
	    writeString(b, "  <another></another>");
	    writeString(b, "</data>");

	    InputStream is = new ByteArrayInputStream(b.toString().getBytes());

	    XMLUtils.ParseOptions xx = new XMLUtils.ParseOptions();
	    xx.attributePrefix = "@";
	    xx.convertNumbers = true;
	    xx.convertDates = true;
	    //	    xx.storeBlanks = true;

	    Map<String,Object> qq = XMLUtils.parseXML(is, xx);

	    Map<String,Object> qq2 = (Map)qq.get("data");
	    if(qq2 == null) {
		System.out.println("FAIL");
	    } else {
		// Map should be EMPTY
		if(qq2.size() != 0) {
		    System.out.println("FAIL; map should be empty");
		}
	    }
	    System.out.println("fromXML");
	    walkMap(qq, 0);


	} catch(Exception e) {
	    System.out.println("epic fail: " + e);
	    e.printStackTrace();
	}

    }


    enum CT { STRING, INT32, INT64, DATE, DOUBLE };

    //@Test
    public void convertTypes2() {

	class D {
	    public String v;
	    public CT t;
	    public D(String v, CT t) {
		this.v = v;
		this.t = t;
	    }
	};

	StringBuilder xml = new StringBuilder();
	xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

	List<D> sxl = new ArrayList<D>();


	sxl.add(new D("", CT.STRING));

	sxl.add(new D("text", CT.STRING));
	sxl.add(new D("0text", CT.STRING));
	sxl.add(new D("text0", CT.STRING));
	sxl.add(new D("0", CT.INT32));
	sxl.add(new D("00", CT.STRING));
	sxl.add(new D("001", CT.STRING));
	sxl.add(new D("00.", CT.STRING));
	sxl.add(new D("00.0", CT.STRING));
	sxl.add(new D("0.0", CT.DOUBLE));
	sxl.add(new D("0.123", CT.DOUBLE));
	sxl.add(new D(".123", CT.DOUBLE));
	sxl.add(new D(".", CT.STRING));
	sxl.add(new D("-", CT.STRING));
	sxl.add(new D("-0.123", CT.DOUBLE));
	sxl.add(new D("10.123", CT.DOUBLE));
	sxl.add(new D("-10.123", CT.DOUBLE));
	sxl.add(new D("10123", CT.INT32));
	sxl.add(new D("230000000", CT.INT32));
	sxl.add(new D("2300000000", CT.INT64));
	sxl.add(new D("-2300000000", CT.INT64));
	sxl.add(new D("82983646557672300000000", CT.DOUBLE));
	sxl.add(new D("-82983646557672300000000", CT.DOUBLE));
	
	sxl.add(new D("20160102", CT.INT32)); // will be picked up as int!  nothing we can do..., CT.STRING))
	
	sxl.add(new D("2016-01-02", CT.DATE));
	sxl.add(new D("2009-09-28T14:07:00", CT.DATE));
	sxl.add(new D("2009-09-28T14:07:00Z", CT.DATE));
	sxl.add(new D("2009-09-28T14:07:00+06:00", CT.DATE));
	sxl.add(new D("2009-09-28T14:07:00-06:00", CT.DATE));
	sxl.add(new D("2009-09-28T14:07:00_06:00" , CT.STRING)); // bad date, must keep as string
	
	// This is STILL OK; 88 is 60+28 !
	sxl.add(new D("2018-02-28T04:88:00.456", CT.DATE));
	
	sxl.add(new D("2018-11-28T04:00:00.456", CT.DATE));
	sxl.add(new D("2018-11-28T04:00:00.456Z", CT.DATE));
	sxl.add(new D("2018-11-28T04:00:00.456-08:30", CT.DATE));
	sxl.add(new D("2018-11-28T04:00:00.456+08:30", CT.DATE));


	xml.append("<data>\n");
	int k = 0;
	for(D d : sxl) {
	    String ks = String.format("%03d", k);
	    xml.append("<a" + ks + ">" + d.v + "</a" + ks + ">\n");
	    k++;
	}
	xml.append("</data>\n");

	System.out.println("input XML");
	System.out.println(xml);
	
	try {
	    InputStream is = new ByteArrayInputStream(xml.toString().getBytes());

	    XMLUtils.ParseOptions xx = new XMLUtils.ParseOptions();
	    xx.attributePrefix = "@";
	    xx.convertNumbers = true;
	    xx.convertDates = true;

	    Map<String,Object> qq = XMLUtils.parseXML(is, xx);
	    /*
	      System.out.println("map output");
	      walkMap(qq, 0);
	    */

	    Map<String,Object> qq2 = (Map)qq.get("data");
	    k = 0;
	    for(D d : sxl) {
		String ks = String.format("a%03d", k); // watch out for that leading a!
		Object o = qq2.get(ks);
		if(o != null) {
		    //System.out.println(o + " " + o.getClass() + d.t);


		    if(o instanceof String && d.t != CT.STRING) {
			System.out.println("FAIL");

		    } else if(o instanceof Double && d.t != CT.DOUBLE) {
			System.out.println("FAIL");

		    } else if(o instanceof java.lang.Integer && d.t != CT.INT32) {
			System.out.println("FAIL");
		    } else if(o instanceof java.lang.Long && d.t != CT.INT64) {
			System.out.println("FAIL");
		    } else if(o instanceof java.util.Date && d.t != CT.DATE) {
			System.out.println("FAIL");
		    }
		}
		k++;
	    }

	} catch(Exception e) {
	    System.out.println("epic fail: " + e);
	    e.printStackTrace();
	}
    }


    private String makeTag(int n, String val) {
	StringBuilder sb = new StringBuilder();
	String tag = "a" + n;
	sb.append("<");
	sb.append(tag);
	sb.append(">");
	sb.append(val);
	sb.append("</");
	sb.append(tag);
	sb.append(">\n");
	return sb.toString();
    }

    @Test
    public void moreFun() {

	StringBuilder xml = new StringBuilder();
	xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

	xml.append("<data>");
	xml.append("  <foo attr1=\"val1\" attr2=\"val2\" />");
	xml.append("  <bar attr1=\"val1\" attr2=\"val2\">yow</bar>");
	xml.append("  <a0>zzz</a0>");
	xml.append("  <a2>zzz</a2>");
	xml.append("  <a4>zzz &gt; </a4>");
	xml.append("</data>");

	try {
	    InputStream is = new ByteArrayInputStream(xml.toString().getBytes());
	    
	    XMLUtils.ParseOptions xx = new XMLUtils.ParseOptions();
	    xx.attributePrefix = "%";
	    
	    Map<String,Object> qq = XMLUtils.parseXML(is, java.util.LinkedHashMap.class, xx);
	    walkMap(qq, 0);

	} catch(Exception e) {
	    System.out.println("epic fail: " + e);
	    e.printStackTrace();
	}

    }



    //@Test
    public void convertTypesPerf() {

	StringBuilder xml = new StringBuilder();
	xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

	xml.append("<data>\n");
	int k = 0;
	for(int jj = 0; jj < 100000; jj++) {
	    xml.append(makeTag(k,"text")); k++;
	    xml.append(makeTag(k,"more text")); k++;
	    xml.append(makeTag(k,"0.0")); k++;
	    xml.append(makeTag(k,"103")); k++;
	    xml.append(makeTag(k,"20160405")); k++;
	    xml.append(makeTag(k,"2016-04-05")); k++;
	}
	xml.append("</data>\n");

	System.out.println("convertNums perf for " + k + " items");
	
	try {
	    InputStream is = new ByteArrayInputStream(xml.toString().getBytes());

	    java.util.Date start =  new java.util.Date();

	    XMLUtils.ParseOptions xx = new XMLUtils.ParseOptions();
	    xx.attributePrefix = "@";
	    xx.convertNumbers = true;
	    xx.convertDates = true;

	    //Map<String,Object> qq = XMLUtils.parseXML(is, xx);
	    Map<String,Object> qq = XMLUtils.parseXML(is, java.util.HashMap.class, xx);
	    //Map<String,Object> qq = XMLUtils.parseXML(is, java.util.LinkedHashMap.class, xx);

	    java.util.Date end = new java.util.Date();
	    long diff = end.getTime() - start.getTime();
	    System.out.println(diff + " millis for " + k + " evals; " + k/diff*1000 + "/sec)");


	} catch(Exception e) {
	    System.out.println("epic fail: " + e);
	    e.printStackTrace();
	}
    }



    private static void walkMap(Map m, int depth) {
	java.util.Iterator<String> ii = m.keySet().iterator();
	while(ii.hasNext()) {
	    String key = ii.next();
	    Object ov = m.get(key);
	    for(int kk = 0; kk < depth; kk++) {
		System.out.print("  ");
	    }
	    //System.out.println(key + ": " + ov.getClass().getName() + ": " + ov.toString());
	    System.out.print(key + ": " + ov.getClass().getName());
	    if(ov instanceof Map) {
		System.out.println();
		walkMap((Map)ov,depth+1);
	    } else if(ov instanceof List) {
		System.out.println();
		walkList((List)ov,depth+1);
	    } else {
		System.out.println(": [" + ov.toString() + "]");
	    }
	}
    }
    private static void walkList(List l, int depth) {
	for(int jj = 0; jj < l.size(); jj++) {
	    Object ov = l.get(jj);
	    for(int kk = 0; kk < depth; kk++) {
		System.out.print("  ");
	    }

	    System.out.print(jj + ": " + ov.getClass().getName());
	    if(ov instanceof Map) {
		System.out.println();
		walkMap((Map)ov,depth+1);
	    } else if(ov instanceof List) {
		System.out.println();
		walkList((List)ov,depth+1);
	    } else {
		System.out.println(": " + ov.toString());
	    }
	}
    }
}
