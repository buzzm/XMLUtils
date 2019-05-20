package org.moschetti.xml;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.util.Map;
import java.util.List;

/**
 *  A perfectly adequte XML emitter for our Maps.
 *  
 */
public class Map2XML {

    public static final int EMIT_TYPE_NONE = 0;
    public static final int EMIT_TYPE_XSD = 1;

    private final static String DEFAULT_TYPE_NAME="__defaultType__";
    private final static int START = 1;
    private final static int END = 2;
    private final static int ITEM = 3;
    private final static int LIST = 4;



    private static void emit(OutputStream os, String s) 
	throws java.io.IOException 
    {
	os.write(s.getBytes());
    }

    private static int writeXMLVal(String fval, boolean cdata, OutputStream os) 
	throws java.io.IOException {

	final byte[] cdata_start = "<![CDATA[".getBytes();
	final byte[] cdata_end = "]]>".getBytes();

	byte[] b = null;

	int nbytes = 0;

	if(cdata) {
	    os.write(cdata_start); nbytes += cdata_start.length;
	}

	b = fval.getBytes();

	os.write(b); nbytes += b.length;

	if(cdata) {
	    os.write(cdata_end); nbytes += cdata_end.length;
	}

	return nbytes;
    }



    private static StringBuilder XMLgroomStringInit(StringBuilder existing, String in, int i) {
	StringBuilder sb = existing;

	if(existing == null) {
	    sb = new StringBuilder();
	    // Further optimize if i != 0
	    if(i != 0) {
		sb.append(in.substring(0, i));
	    }
	}
	return sb;
    }


    private static String XMLgroomString(String in, boolean includeBar) {

	// Most of the time, nothing to do.   So only
	// take the grooming hit if we find someething...

	StringBuilder sb = null;

	int n = in.length();

	for(int i = 0; i < n; i++) {
	    char c = in.charAt(i);

	    switch(c) {

	    default:
		if(sb != null) {
		    sb.append(c);
		}
		break;

	    case '<':
		sb = XMLgroomStringInit(sb, in , i);
		sb.append("&lt;"); 
		break;

	    case '>':
		sb = XMLgroomStringInit(sb, in , i);
		sb.append("&gt;"); 
		break;

	    case '&':
		sb = XMLgroomStringInit(sb, in , i);
		sb.append("&amp;"); 
		break;


	    case '|':
		if(includeBar) {
		    sb = XMLgroomStringInit(sb, in , i);
		    sb.append("\\|"); 
		} else if(sb != null) {
		    sb.append(c);
		}
		break;
	    }
	}

	if(sb == null) {
	    // nothing to groom: return ORIGNAL string!
	    return in;
	} else {
	    return sb.toString();
	}
    }


    public static void writeXML(OutputStream out, Map<String,Object> xmap, String enclosingTag)
	throws IOException
    {
	walkMap(out, enclosingTag, xmap, 0, "  ");
    }



    
    private static void walkMap(OutputStream os, String enclosingTag, Map<String,Object> m, int level, String indentString)
	throws IOException
    {
	writeTag(LIST, START, os, enclosingTag, level, indentString);

	java.util.Iterator<String> ii = m.keySet().iterator();
	while(ii.hasNext()) {
	    String key = ii.next();
	    Object ov = m.get(key);

	    if(ov != null) {

		if(ov instanceof Map) {
		    walkMap(os, key, (Map)ov, level+1, indentString);


		} else if(ov instanceof List) {
		    String nenct = key + "_LIST";
		    writeTag(LIST, START, os, nenct, level+1, indentString);
		    walkList(os, key, (List)ov, level+1, indentString);
		    writeTag(LIST, END, os, nenct, level+1,  indentString);

		} else {
		    writeTag(ITEM, START, os, key, level+1, indentString);

		    // switch type to groom....

		    String xx = XMLgroomString(ov.toString(), false);

		    writeXMLVal(xx, false, os);

		    writeTag(ITEM, END, os, key, level+1, indentString);
		}
	    }
	}

	writeTag(LIST, END, os, enclosingTag, level, indentString);
    }



    private static void walkList(OutputStream os, String enclosingTag, List l, int level, String indentString) 
	throws IOException
    {
	for(int jj = 0; jj < l.size(); jj++) {
	    Object ov = l.get(jj);
	    
	    if(ov != null) {
		if(ov instanceof Map) {
		    walkMap(os, enclosingTag, (Map)ov, level+1, indentString);
		} else if(ov instanceof List) {
		    walkList(os, enclosingTag, (List)ov, level+1, indentString);
		} else {
		    writeTag(ITEM, START, os, enclosingTag, level+1, indentString);
		    String xx = XMLgroomString(ov.toString(), false);

		    writeXMLVal(xx, false, os);

		    writeTag(ITEM, END, os, enclosingTag, level+1, indentString);
		}
	    }
	}
    }    



    private static void writeTag(int type, int soe, OutputStream os, String tag, int indentLevel, String indentString)
	throws java.io.IOException {

	//  For ITEM:
	//    [whitespace]<ns:tag [XSDtype=]>
	//    </ns:tag>
	//
	//  For LIST:
	//    [whitespace]<ns:tag [XSDtype=] xmlns:ns="uri">
	//    [whitespace]</ns:tag>

	int n = 0;

	StringBuilder sb = new StringBuilder();

	if(soe == START || type == LIST) {
	    if(indentLevel > 0) {
		for(int a = 0; a < indentLevel; a++) {
		    sb.append(indentString);
		}
	    }
	}

	sb.append("<");

	if(soe == END) {
	    sb.append("/");
	}

	String prefix = null;
	String uri = null;

	sb.append(tag);

	sb.append(">");

	byte[] bb = sb.toString().getBytes();
	os.write(bb);

	if(soe == END || type == LIST) {
	    if(indentString != null) {
		os.write('\n'); 
	    }
	}
    }

}