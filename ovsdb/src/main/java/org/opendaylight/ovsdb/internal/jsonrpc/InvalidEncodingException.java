package org.opendaylight.ovsdb.internal.jsonrpc;

public class InvalidEncodingException extends RuntimeException {

    private final String actual;

    public InvalidEncodingException(String actual, String message) {
          super(message);
          this.actual = actual;
      }

    public String getActual() {
        return actual;
    }
}
