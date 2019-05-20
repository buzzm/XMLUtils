package org.moschetti.xml;

/**
 * This exception is thrown if a stream cannot be parsed, i.e. syntax or
 * encoding or out of memory or similar awful thing...
 */
@SuppressWarnings("serial")
public class XMLParsingException extends Exception {

    public XMLParsingException(String message) {
        super(message);
    }

}
