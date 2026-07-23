package com.oa.ai.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmployeeAuthInterceptorTest {

    private final EmployeeTokenValidator validator = mock(EmployeeTokenValidator.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EmployeeAuthInterceptor interceptor =
            new EmployeeAuthInterceptor(validator, objectMapper);

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.path("code").asInt()).isEqualTo(401);
    }

    @Test
    void validTokenBindsEmployeeNumber() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Emp-Token", "valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(validator.validate("valid-token")).thenReturn(1001);

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        assertThat(request.getAttribute(EmployeeAuthInterceptor.EMPLOYEE_NUMBER_ATTRIBUTE))
                .isEqualTo(1001);
    }

    @Test
    void unavailableEmployeeServiceReturnsServiceUnavailable() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Emp-Token", "any-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(validator.validate("any-token")).thenThrow(
                new EmployeeTokenValidator.EmployeeAuthUnavailableException(
                        "unavailable", new RuntimeException()));

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(503);
    }
}
