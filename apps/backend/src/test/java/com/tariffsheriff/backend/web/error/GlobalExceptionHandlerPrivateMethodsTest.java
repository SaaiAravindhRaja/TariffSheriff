package com.tariffsheriff.backend.web.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerPrivateMethodsTest {

    @Test
    void makeUserFriendly_null_returnsGeneric() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Method m = GlobalExceptionHandler.class.getDeclaredMethod("makeUserFriendly", String.class);
        m.setAccessible(true);

        String out = (String) m.invoke(handler, new Object[] { null });
        assertEquals("I encountered an issue processing your request. Please try again.", out);
    }

    @Test
    void makeUserFriendly_detectsJsonAndParse() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Method m = GlobalExceptionHandler.class.getDeclaredMethod("makeUserFriendly", String.class);
        m.setAccessible(true);

        String out = (String) m.invoke(handler, "Failed to parse JSON payload");
        assertEquals("I had trouble understanding the format of your request. Please check your input and try again.", out);
    }

    @Test
    void makeUserFriendly_detectsMissing_required() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Method m = GlobalExceptionHandler.class.getDeclaredMethod("makeUserFriendly", String.class);
        m.setAccessible(true);

        String out = (String) m.invoke(handler, "The 'name' field is required and missing");
        assertEquals("Some required information is missing from your request. Please provide all necessary details.", out);
    }

    @Test
    void makeUserFriendly_detectsInvalid_orIllegal() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Method m = GlobalExceptionHandler.class.getDeclaredMethod("makeUserFriendly", String.class);
        m.setAccessible(true);

        String out = (String) m.invoke(handler, "Invalid value for amount");
        assertEquals("The information provided doesn't match what I expected. Please check your input and try again.", out);
    }

    @SuppressWarnings("deprecation")
    @Test
    void extractMessage_returnsMostSpecificCauseMessageWhenAvailable() throws Exception {
        Method m = GlobalExceptionHandler.class.getDeclaredMethod("extractMessage", Exception.class);
        m.setAccessible(true);

        RuntimeException cause = new RuntimeException("underlying cause message");
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("unreadable", cause);

        String out = (String) m.invoke(null, ex);
        assertEquals("underlying cause message", out);
    }

    @Test
    void build_createsErrorResponseWithFields() throws Exception {
        Method m = GlobalExceptionHandler.class.getDeclaredMethod("build", HttpStatus.class, String.class, String.class);
        m.setAccessible(true);

        Object res = m.invoke(null, HttpStatus.BAD_REQUEST, "bad input", "/path");
        assertNotNull(res);
        assertTrue(res instanceof ErrorResponse);
        ErrorResponse er = (ErrorResponse) res;
        assertEquals(400, er.getStatus());
        assertEquals("Bad Request", er.getError());
        assertEquals("bad input", er.getMessage());
        assertEquals("/path", er.getPath());
        assertNotNull(er.getTimestamp());
    }
    @Test
    void makeUserFriendly_unknownMessage_returnsDefaultFallback() throws Exception {
        // This test covers the final 'return' statement in makeUserFriendly
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Method m = GlobalExceptionHandler.class.getDeclaredMethod("makeUserFriendly", String.class);
        m.setAccessible(true);

        String out = (String) m.invoke(handler, "A generic database error occurred");
        
        // It should not match any specific keyword and return the default fallback
        assertEquals("I had trouble processing your request. Please check your input and try again.", out);
    }

    @Test
    void extractMessage_returnsExceptionMessageWhenNoSpecificCause() throws Exception {
        // This tests the 'else' path of extractMessage for a generic exception
        Method m = GlobalExceptionHandler.class.getDeclaredMethod("extractMessage", Exception.class);
        m.setAccessible(true);

        IllegalArgumentException ex = new IllegalArgumentException("This is the main message");
        String out = (String) m.invoke(null, ex);
        
        assertEquals("This is the main message", out);
    }
    
    @SuppressWarnings("deprecation")
    @Test
    void extractMessage_returnsTopMessageWhenCauseIsNull() throws Exception {
        // This tests the 'else' path for an HttpMessageNotReadableException that has no cause
        Method m = GlobalExceptionHandler.class.getDeclaredMethod("extractMessage", Exception.class);
        m.setAccessible(true);

        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Top level message only");

        String out = (String) m.invoke(null, ex);
        assertEquals("Top level message only", out);
    }
}
