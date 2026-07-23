package com.oa2.controller;

import com.oa2.service.OnlineUserTracker;
import com.oa2.util.RESP;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalAuthControllerTest {

    private final OnlineUserTracker onlineUserTracker = mock(OnlineUserTracker.class);
    private final InternalAuthController controller =
            new InternalAuthController(onlineUserTracker, "internal-secret");

    @Test
    void wrongInternalTokenIsRejectedBeforeEmployeeLookup() {
        RESP response = controller.validateEmployee("wrong-secret", "employee-token");

        assertThat(response.getCode()).isEqualTo(403);
        verify(onlineUserTracker, never()).getEmployeeNumberByToken("employee-token");
    }

    @Test
    void invalidEmployeeTokenIsRejected() {
        when(onlineUserTracker.getEmployeeNumberByToken("invalid-token")).thenReturn(null);

        RESP response = controller.validateEmployee("internal-secret", "invalid-token");

        assertThat(response.getCode()).isEqualTo(401);
    }

    @Test
    void validEmployeeTokenReturnsEmployeeNumberAndRefreshesActivity() {
        when(onlineUserTracker.getEmployeeNumberByToken("valid-token")).thenReturn(1001);

        RESP response = controller.validateEmployee("internal-secret", "valid-token");

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEqualTo(1001);
        verify(onlineUserTracker).markEmployeeTokenOnline("valid-token");
    }
}
