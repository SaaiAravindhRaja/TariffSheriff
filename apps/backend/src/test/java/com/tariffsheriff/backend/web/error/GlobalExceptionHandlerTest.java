package com.tariffsheriff.backend.web.error;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import com.tariffsheriff.backend.tariff.exception.TariffRateNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleTariffRateNotFound_returns404_withBody() {
        TariffRateNotFoundException ex = new TariffRateNotFoundException("not found foo");
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/test");

        ResponseEntity<ErrorResponse> resp = handler.handleTariffRateNotFound(ex, req);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        ErrorResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals(404, body.getStatus());
        assertEquals("Not Found", body.getError());
        assertEquals("not found foo", body.getMessage());
        assertEquals("/api/test", body.getPath());
    }

    @Test
    void handleBadRequest_withIllegalArgument_producesFriendlyMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid input provided");
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/x");

        ResponseEntity<ErrorResponse> resp = handler.handleBadRequest(ex, req);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        ErrorResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals(400, body.getStatus());
        assertEquals("Bad Request", body.getError());
        // message should be user friendly; check it contains a short hint
        assertTrue(body.getMessage().toLowerCase().contains("check")
                || body.getMessage().toLowerCase().contains("try again")
                || body.getMessage().toLowerCase().contains("doesn't"));
        assertEquals("/api/x", body.getPath());
    }

    @SuppressWarnings("deprecation")
    @Test
    void handleBadRequest_withHttpMessageNotReadable_usesMostSpecificCause() {
        RuntimeException cause = new RuntimeException("JSON parse failed at [1:2]");
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("unreadable", cause);
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/y");

        ResponseEntity<ErrorResponse> resp = handler.handleBadRequest(ex, req);

        assertEquals(400, resp.getStatusCode().value());
        ErrorResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals("/api/y", body.getPath());
        // The handler maps parse/json messages to a format-related friendly message
        String msg = body.getMessage().toLowerCase();
        assertTrue(msg.contains("format") || msg.contains("understanding") || msg.contains("trouble"));
    }

    // Dummy method used to create a MethodParameter for MethodArgumentNotValidException
    @SuppressWarnings("unused")
    private void validationTarget(String ignored) {}

    @Test
    void handleValidation_buildsAggregatedMessageFromBindingResult() throws Exception {
        // build a MethodParameter pointing to the first parameter of validationTarget
        MethodParameter mp = new MethodParameter(this.getClass().getDeclaredMethod("validationTarget", String.class), 0);

        // create a BindingResult with a single field error
        BeanPropertyBindingResult br = new BeanPropertyBindingResult(new Object(), "testObject");
        br.addError(new FieldError("testObject", "myField", "must not be null"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mp, br);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/validate");

        ResponseEntity<ErrorResponse> resp = handler.handleValidation(ex, req);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        ErrorResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals(400, body.getStatus());
        assertEquals("Bad Request", body.getError());
        // aggregated message should contain both the field name and the default message
        String msg = body.getMessage();
        assertTrue(msg.contains("myField"));
        assertTrue(msg.contains("must not be null"));
        assertEquals("/api/validate", body.getPath());
    }

}
