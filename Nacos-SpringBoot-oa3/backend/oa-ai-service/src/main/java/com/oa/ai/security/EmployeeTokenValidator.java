package com.oa.ai.security;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class EmployeeTokenValidator {

    private final RestClient restClient;
    private final String internalToken;

    public EmployeeTokenValidator(RestClient.Builder restClientBuilder,
                                  @Value("${oa.employee-service.base-url}") String baseUrl,
                                  @Value("${oa.internal-token}") String internalToken) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2_000);
        requestFactory.setReadTimeout(3_000);
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.internalToken = internalToken;
    }

    public Integer validate(String employeeToken) {
        try {
            JsonNode response = restClient.get()
                    .uri("/internal/auth/employee")
                    .header("X-Internal-Token", internalToken)
                    .header("X-Emp-Token", employeeToken)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || response.path("code").asInt() != 200
                    || !response.path("data").canConvertToInt()) {
                return null;
            }
            return response.path("data").asInt();
        } catch (Exception exception) {
            throw new EmployeeAuthUnavailableException("员工认证服务不可用", exception);
        }
    }

    public static class EmployeeAuthUnavailableException extends RuntimeException {

        public EmployeeAuthUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
