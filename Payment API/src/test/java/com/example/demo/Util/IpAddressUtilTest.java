package com.example.demo.Util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class IpAddressUtilTest {

    @Mock
    private HttpServletRequest request;

    private final IpAddressUtil ipAddressUtil = new IpAddressUtil();

    @Test
    @DisplayName("getClientIpAddress - X-Forwarded-For (Single IP)")
    void getClientIpAddress_XForwardedFor_Single() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1");
        assertThat(ipAddressUtil.getClientIpAddress(request)).isEqualTo("1.1.1.1");
    }

    @Test
    @DisplayName("getClientIpAddress - X-Forwarded-For (Multiple IPs)")
    void getClientIpAddress_XForwardedFor_Multiple() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1, 2.2.2.2, 3.3.3.3");
        assertThat(ipAddressUtil.getClientIpAddress(request)).isEqualTo("1.1.1.1");
    }

    @Test
    @DisplayName("getClientIpAddress - X-Forwarded-For (Unknown prefix)")
    void getClientIpAddress_XForwardedFor_UnknownPrefix() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown, 1.1.1.1");
        when(request.getRemoteAddr()).thenReturn("4.4.4.4");
        assertThat(ipAddressUtil.getClientIpAddress(request)).isEqualTo("4.4.4.4");
    }

    @Test
    @DisplayName("getClientIpAddress - Proxy-Client-IP")
    void getClientIpAddress_ProxyClientIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        when(request.getHeader("Proxy-Client-IP")).thenReturn("5.5.5.5");
        assertThat(ipAddressUtil.getClientIpAddress(request)).isEqualTo("5.5.5.5");
    }

    @Test
    @DisplayName("getClientIpAddress - WL-Proxy-Client-IP")
    void getClientIpAddress_WlProxyClientIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);  // Add this line

        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn("6.6.6.6");
        assertThat(ipAddressUtil.getClientIpAddress(request)).isEqualTo("6.6.6.6");
    }

    @Test
    @DisplayName("getClientIpAddress - RemoteAddr Fallback")
    void getClientIpAddress_RemoteAddr() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn("");
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn("unknown");
        when(request.getRemoteAddr()).thenReturn("7.7.7.7");
        assertThat(ipAddressUtil.getClientIpAddress(request)).isEqualTo("7.7.7.7");
    }

    @Test
    @DisplayName("getClientIpAddress - RemoteAddr is IPv6 Localhost")
    void getClientIpAddress_RemoteAddr_Ipv6Localhost() {
        when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");
        assertThat(ipAddressUtil.getClientIpAddress(request)).isEqualTo("0:0:0:0:0:0:0:1");
    }

    @Test
    @DisplayName("getClientIpAddress - All headers and RemoteAddr are null/invalid")
    void getClientIpAddress_AllNull() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(null);
        assertThat(ipAddressUtil.getClientIpAddress(request)).isEqualTo("Unknown");
    }

    @Test
    @DisplayName("getClientIpAddress - Header contains invalid IP")
    void getClientIpAddress_InvalidIpInHeader() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("not-an-ip");
        when(request.getRemoteAddr()).thenReturn("8.8.8.8");
        assertThat(ipAddressUtil.getClientIpAddress(request)).isEqualTo("8.8.8.8");
    }


    @ParameterizedTest
    @CsvSource({
            "127.0.0.1, true",
            "0:0:0:0:0:0:0:1, true",
            "192.168.1.1, false",
            "10.0.0.1, false",
            "8.8.8.8, false",
            "fe80::1, false",
            "Unknown, false",
            "'', false",
            ", false"
    })
    @DisplayName("isLocalhost Tests")
    void isLocalhost_VariousInputs(String ip, boolean expected) {
        if (ip == null || ip.isEmpty()) {
            assertThat(ipAddressUtil.isLocalhost(null)).isEqualTo(expected);
            assertThat(ipAddressUtil.isLocalhost("")).isEqualTo(expected);
        } else {
            assertThat(ipAddressUtil.isLocalhost(ip)).isEqualTo(expected);
        }
    }
}