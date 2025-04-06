package com.example.demo.Service;

import com.example.demo.Util.IpAddressUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;


import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeoIpServiceTest {

    private static MockWebServer mockWebServer;
    private GeoIpService geoIpService;
    private WebClient.Builder webClientBuilder;

    @Mock
    private IpAddressUtil ipAddressUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void setUpServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDownServer() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void setUp() {
        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        webClientBuilder = WebClient.builder().baseUrl(baseUrl);

        geoIpService = new GeoIpService(webClientBuilder, ipAddressUtil);

        ReflectionTestUtils.setField(geoIpService, "apiUrlTemplate", baseUrl + "/geoip/{ip}");
        ReflectionTestUtils.setField(geoIpService, "apiTimeoutSeconds", 2L);
    }

    @AfterEach
    void cleanupMockWebServer() throws InterruptedException {
        RecordedRequest request;
        do {
            request = mockWebServer.takeRequest(10, TimeUnit.MILLISECONDS);
        } while (request != null); // Continue as long as requests are being dequeued
    }


    @Test
    @DisplayName("Happy Path - Should return country code when API call succeeds")
    void getCountryFromIp_Success() throws JsonProcessingException, InterruptedException {
        String ip = "8.8.8.8";
        String expectedCountry = "US";
        JsonNode responseJson = objectMapper.createObjectNode().put("country", expectedCountry);

        when(ipAddressUtil.isLocalhost(ip)).thenReturn(false);

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(responseJson))
                .addHeader("Content-Type", "application/json"));

        Optional<String> country = geoIpService.getCountryFromIp(ip);

        assertThat(country).isPresent().contains(expectedCountry);
        verify(ipAddressUtil).isLocalhost(ip);
        assertThat(mockWebServer.takeRequest().getPath()).isEqualTo("/geoip/8.8.8.8"); // Verify request path
    }

    @Test
    @DisplayName("Edge Case - API returns empty country string")
    void getCountryFromIp_ApiReturnsEmptyCountry() throws JsonProcessingException, InterruptedException {
        String ip = "1.2.3.4";
        JsonNode responseJson = objectMapper.createObjectNode().put("country", "");
        when(ipAddressUtil.isLocalhost(ip)).thenReturn(false);

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(responseJson))
                .addHeader("Content-Type", "application/json"));

        Optional<String> country = geoIpService.getCountryFromIp(ip);

        assertThat(country).isNotPresent();
        verify(ipAddressUtil).isLocalhost(ip);
        assertThat(mockWebServer.takeRequest().getPath()).isEqualTo("/geoip/1.2.3.4");
    }

    @Test
    @DisplayName("Edge Case - API returns JSON without country field")
    void getCountryFromIp_ApiReturnsMissingCountryField() throws JsonProcessingException, InterruptedException {
        String ip = "1.2.3.4";
        JsonNode responseJson = objectMapper.createObjectNode().put("otherField", "value");
        when(ipAddressUtil.isLocalhost(ip)).thenReturn(false);

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(responseJson))
                .addHeader("Content-Type", "application/json"));

        Optional<String> country = geoIpService.getCountryFromIp(ip);

        assertThat(country).isNotPresent();
        verify(ipAddressUtil).isLocalhost(ip);
        assertThat(mockWebServer.takeRequest().getPath()).isEqualTo("/geoip/1.2.3.4");
    }

    @Test
    @DisplayName("Error Case - API returns 4xx error")
    void getCountryFromIp_ApiReturns4xx() throws InterruptedException {
        String ip = "4.4.4.4";
        when(ipAddressUtil.isLocalhost(ip)).thenReturn(false);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("{\"error\":\"IP not found\"}")
                .addHeader("Content-Type", "application/json"));

        Optional<String> country = geoIpService.getCountryFromIp(ip);

        assertThat(country).isNotPresent();
        verify(ipAddressUtil).isLocalhost(ip);
        assertThat(mockWebServer.takeRequest().getPath()).isEqualTo("/geoip/4.4.4.4");
    }

    @Test
    @DisplayName("Error Case - API returns 5xx error")
    void getCountryFromIp_ApiReturns5xx() throws InterruptedException {
        String ip = "5.5.5.5";
        when(ipAddressUtil.isLocalhost(ip)).thenReturn(false);

        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        Optional<String> country = geoIpService.getCountryFromIp(ip);

        assertThat(country).isNotPresent();
        verify(ipAddressUtil).isLocalhost(ip);
        assertThat(mockWebServer.takeRequest().getPath()).isEqualTo("/geoip/5.5.5.5");
    }

    @Test
    @DisplayName("Edge Case - Localhost IP should not call API and return 'Localhost'")
    void getCountryFromIp_Localhost() throws InterruptedException {
        RecordedRequest leftoverRequest;
        do {
            leftoverRequest = mockWebServer.takeRequest(5, TimeUnit.MILLISECONDS);
        } while (leftoverRequest != null);
        int countBeforeTestLogic = mockWebServer.getRequestCount();
        String ip = "127.0.0.1";
        when(ipAddressUtil.isLocalhost(ip)).thenReturn(true);
        Optional<String> country = geoIpService.getCountryFromIp(ip);

        assertThat(country).isPresent().contains("Localhost");
        verify(ipAddressUtil).isLocalhost(ip);
        assertThat(mockWebServer.getRequestCount())
                .as("Request count should not change for localhost IP")
                .isEqualTo(countBeforeTestLogic);
        verifyNoMoreInteractions(ipAddressUtil);
    }

    @Test
    @DisplayName("Edge Case - 'Unknown' IP should return 'Unknown' without calling isLocalhost or API")
    void getCountryFromIp_Unknown() throws InterruptedException {
        RecordedRequest leftoverRequest;
        do {
            leftoverRequest = mockWebServer.takeRequest(5, TimeUnit.MILLISECONDS);
        } while (leftoverRequest != null);
        int countBeforeTestLogic = mockWebServer.getRequestCount();

        String ip = "Unknown";
        Optional<String> country = geoIpService.getCountryFromIp(ip);

        assertThat(country).isPresent().contains("Unknown");
        verify(ipAddressUtil, never()).isLocalhost(anyString());
        assertThat(mockWebServer.getRequestCount())
                .as("Request count should not change for 'Unknown' IP")
                .isEqualTo(countBeforeTestLogic);
        verifyNoMoreInteractions(ipAddressUtil);
    }

    @Test
    @DisplayName("Null/Blank Test - Null IP should return 'Unknown' without adding new requests")
    void getCountryFromIp_NullIp() throws InterruptedException {
        RecordedRequest leftoverRequest;
        do {
            leftoverRequest = mockWebServer.takeRequest(5, TimeUnit.MILLISECONDS);
        } while (leftoverRequest != null);

        int countBeforeTestLogic = mockWebServer.getRequestCount();
        String ip = null;
        Optional<String> country = geoIpService.getCountryFromIp(ip);

        assertThat(country).isPresent().contains("Unknown");
        verify(ipAddressUtil, never()).isLocalhost(any());
        assertThat(mockWebServer.getRequestCount())
                .as("Request count should not change during service call for null IP")
                .isEqualTo(countBeforeTestLogic);
        verifyNoMoreInteractions(ipAddressUtil);
    }
    @Test
    @DisplayName("Null/Blank Test - Blank IP should not call API and return 'Unknown'")
    void getCountryFromIp_BlankIp() throws InterruptedException {
        RecordedRequest leftoverRequest;
        do {
            leftoverRequest = mockWebServer.takeRequest(5, TimeUnit.MILLISECONDS);
        } while (leftoverRequest != null);
        int countBeforeTestLogic = mockWebServer.getRequestCount();

        String ip = "   ";
        Optional<String> country = geoIpService.getCountryFromIp(ip);

        assertThat(country).isPresent().contains("Unknown");
        verify(ipAddressUtil, never()).isLocalhost(any());
        assertThat(mockWebServer.getRequestCount())
                .as("Request count should not change for blank IP")
                .isEqualTo(countBeforeTestLogic);
        verifyNoMoreInteractions(ipAddressUtil);
    }
}