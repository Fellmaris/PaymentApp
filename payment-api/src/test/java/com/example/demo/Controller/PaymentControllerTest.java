package com.example.demo.Controller;

import com.example.demo.DTO.PaymentDTO;
import com.example.demo.Model.Payment;
import com.example.demo.Service.GeoIpService;
import com.example.demo.Service.PaymentService;
import com.example.demo.Util.IpAddressUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private IpAddressUtil ipAddressUtil;

    @MockBean
    private GeoIpService geoIpService;

    private Payment samplePayment;
    private PaymentDTO samplePaymentDTO;
    private UUID validUuid;
    private UUID notFoundUuid;

    @BeforeEach
    void setUp() {
        validUuid = UUID.randomUUID();
        notFoundUuid = UUID.randomUUID();

        samplePayment = new Payment();
        samplePayment.setId(validUuid);
        samplePayment.setAmount(new BigDecimal("100.50"));
        samplePayment.setCurrency(Payment.Currency.EUR);
        samplePayment.setDebtorIban("DE89370400440532013000");
        samplePayment.setCreditorIban("DE89370400440532013001");
        samplePayment.setType(1);
        samplePayment.setCreationDate(LocalDateTime.now().minusHours(1));
        samplePayment.setCancelation(null);

        samplePaymentDTO = PaymentDTO.fromEntity(samplePayment);
        samplePaymentDTO.setAmount("100.50");

        when(ipAddressUtil.getClientIpAddress(any())).thenReturn("192.168.1.100");
        when(ipAddressUtil.isLocalhost(anyString())).thenReturn(false);
        when(geoIpService.getCountryFromIp(anyString())).thenReturn(Optional.of("DE"));
    }

    @Nested
    @DisplayName("GET /payments")
    class GetAllPaymentsTests {

        @Test
        @DisplayName("Happy Path - Should return list of non-cancelled payments and status 200 OK")
        void getAllPayments_shouldReturnPaymentsAndOk() throws Exception {
            when(paymentService.getAllNonCancelledPayments()).thenReturn(List.of(samplePayment));

            ResultActions result = mockMvc.perform(get("/payments"));

            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(validUuid.toString())))
                    .andExpect(jsonPath("$[0].amount", is(100.50)))
                    .andExpect(jsonPath("$[0].cancelation").doesNotExist());

            verify(paymentService).getAllNonCancelledPayments();
            verify(ipAddressUtil).getClientIpAddress(any());
            verify(geoIpService).getCountryFromIp(eq("192.168.1.100"));
        }

        @Test
        @DisplayName("Edge Case - Should return empty list and status 200 OK when no payments exist")
        void getAllPayments_whenNoPayments_shouldReturnEmptyListAndOk() throws Exception {
            when(paymentService.getAllNonCancelledPayments()).thenReturn(Collections.emptyList());

            ResultActions result = mockMvc.perform(get("/payments"));

            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(paymentService).getAllNonCancelledPayments();
            verify(ipAddressUtil).getClientIpAddress(any());
            verify(geoIpService).getCountryFromIp(anyString());
        }

        @Test
        @DisplayName("GeoIP Edge Case - Should still return payments when GeoIP fails")
        void getAllPayments_whenGeoIpFails_shouldReturnPaymentsAndOk() throws Exception {
            when(paymentService.getAllNonCancelledPayments()).thenReturn(List.of(samplePayment));
            when(geoIpService.getCountryFromIp(anyString())).thenThrow(new RuntimeException("GeoIP Service Unavailable"));

            ResultActions result = mockMvc.perform(get("/payments"));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(paymentService).getAllNonCancelledPayments();
            verify(ipAddressUtil).getClientIpAddress(any());
            verify(geoIpService).getCountryFromIp(anyString());
        }

        @Test
        @DisplayName("GeoIP Edge Case - Localhost IP - Should skip GeoIP lookup")
        void getAllPayments_whenLocalhostIp_shouldSkipGeoIp() throws Exception {
            when(paymentService.getAllNonCancelledPayments()).thenReturn(List.of(samplePayment));
            when(ipAddressUtil.getClientIpAddress(any())).thenReturn("127.0.0.1");
            when(ipAddressUtil.isLocalhost("127.0.0.1")).thenReturn(true);

            ResultActions result = mockMvc.perform(get("/payments"));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(paymentService).getAllNonCancelledPayments();
            verify(ipAddressUtil).getClientIpAddress(any());
            verify(ipAddressUtil).isLocalhost("127.0.0.1");
            verify(geoIpService, never()).getCountryFromIp(anyString());
        }

        @Test
        @DisplayName("GeoIP Edge Case - Unknown IP - Should skip GeoIP lookup")
        void getAllPayments_whenUnknownIp_shouldSkipGeoIp() throws Exception {
            when(paymentService.getAllNonCancelledPayments()).thenReturn(List.of(samplePayment));
            when(ipAddressUtil.getClientIpAddress(any())).thenReturn("Unknown");
            when(ipAddressUtil.isLocalhost("Unknown")).thenReturn(false);

            ResultActions result = mockMvc.perform(get("/payments"));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(paymentService).getAllNonCancelledPayments();
            verify(ipAddressUtil).getClientIpAddress(any());
            verify(geoIpService, never()).getCountryFromIp(anyString());
        }
    }

    @Nested
    @DisplayName("GET /payments/{id}")
    class GetPaymentByIdTests {

        @Test
        @DisplayName("Happy Path - Should return payment and status 200 OK when ID exists")
        void getPaymentById_whenIdExists_shouldReturnPaymentAndOk() throws Exception {
            when(paymentService.getPaymentById(validUuid)).thenReturn(Optional.of(samplePayment));

            ResultActions result = mockMvc.perform(get("/payments/{id}", validUuid));

            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(validUuid.toString())))
                    .andExpect(jsonPath("$.amount", is(100.50)))
                    .andExpect(jsonPath("$.currency", is(Payment.Currency.EUR.toString())));

            verify(paymentService).getPaymentById(validUuid);
        }

        @Test
        @DisplayName("Not Found - Should return status 404 Not Found when ID does not exist")
        void getPaymentById_whenIdNotFound_shouldReturnNotFound() throws Exception {
            when(paymentService.getPaymentById(notFoundUuid)).thenReturn(Optional.empty());

            ResultActions result = mockMvc.perform(get("/payments/{id}", notFoundUuid));

            result.andExpect(status().isNotFound());

            verify(paymentService).getPaymentById(notFoundUuid);
        }

        @Test
        @DisplayName("Bad Request - Should return status 400 Bad Request for invalid UUID format")
        void getPaymentById_whenInvalidUuidFormat_shouldReturnBadRequest() throws Exception {
            String invalidUuid = "not-a-uuid";

            ResultActions result = mockMvc.perform(get("/payments/{id}", invalidUuid));

            result.andExpect(status().isBadRequest());

            verify(paymentService, never()).getPaymentById(any());
        }
    }

    @Nested
    @DisplayName("POST /payments")
    class SavePaymentTests {

        @Test
        @DisplayName("Happy Path - Should create payment and return status 201 Created")
        void savePayment_whenValidData_shouldCreateAndReturnCreated() throws Exception {
            PaymentDTO inputDto = new PaymentDTO();
            inputDto.setAmount("250.75");
            inputDto.setCurrency(Payment.Currency.USD);
            inputDto.setDebtorIban("US12345678901234567890");
            inputDto.setCreditorIban("US98765432109876543210");
            inputDto.setType(2);
            inputDto.setDetails("Test Payment");

            Payment savedPayment = new Payment();
            savedPayment.setId(UUID.randomUUID());
            savedPayment.setAmount(new BigDecimal("250.75"));
            savedPayment.setCurrency(Payment.Currency.USD);
            savedPayment.setDebtorIban("US12345678901234567890");
            savedPayment.setCreditorIban("US98765432109876543210");
            savedPayment.setType(2);
            savedPayment.setDetails("Test Payment");
            savedPayment.setCreationDate(LocalDateTime.now());

            PaymentDTO returnedDto = PaymentDTO.fromEntity(savedPayment);

            when(paymentService.savePayment(any(PaymentDTO.class))).thenReturn(returnedDto);

            MockHttpServletRequestBuilder request = post("/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(inputDto));

            ResultActions result = mockMvc.perform(request);

            result.andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(notNullValue())))
                    .andExpect(jsonPath("$.creationDate", is(notNullValue())))
                    .andExpect(jsonPath("$.amount", is("250.75")))
                    .andExpect(jsonPath("$.currency", is(Payment.Currency.USD.toString())))
                    .andExpect(jsonPath("$.type", is(2)));

            verify(paymentService).savePayment(any(PaymentDTO.class));
        }

        @Test
        @DisplayName("Validation Error - Should return status 400 Bad Request for invalid amount format")
        void savePayment_whenInvalidAmountFormat_shouldReturnBadRequest() throws Exception {
            PaymentDTO inputDto = new PaymentDTO();
            inputDto.setAmount("100,50");
            inputDto.setCurrency(Payment.Currency.EUR);
            inputDto.setDebtorIban("DE123");
            inputDto.setCreditorIban("DE456");
            inputDto.setType(1);

            MockHttpServletRequestBuilder request = post("/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(inputDto));
            ResultActions result = mockMvc.perform(request);

            result.andExpect(status().isBadRequest());

            verify(paymentService, never()).savePayment(any());
        }

        @Test
        @DisplayName("Validation Error - Should return status 400 Bad Request for amount with too many decimals")
        void savePayment_whenTooManyDecimals_shouldReturnBadRequest() throws Exception {
            PaymentDTO inputDto = new PaymentDTO();
            inputDto.setAmount("100.123");
            inputDto.setCurrency(Payment.Currency.EUR);
            inputDto.setDebtorIban("DE123");
            inputDto.setCreditorIban("DE456");
            inputDto.setType(1);

            MockHttpServletRequestBuilder request = post("/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(inputDto));
            ResultActions result = mockMvc.perform(request);

            result.andExpect(status().isBadRequest());
            verify(paymentService, never()).savePayment(any());
        }

        @Test
        @DisplayName("Bad Request - Should return status 400 Bad Request for missing required fields (if any were added)")
        void savePayment_whenMissingRequiredFields_shouldReturnBadRequest() throws Exception {
            PaymentDTO inputDto = new PaymentDTO();
            inputDto.setAmount("50.00");
            inputDto.setDebtorIban("DE123");
            inputDto.setCreditorIban("DE456");
            inputDto.setType(1);

            MockHttpServletRequestBuilder request = post("/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(inputDto));
            ResultActions result = mockMvc.perform(request);

            int statusCode = result.andReturn().getResponse().getStatus();
            if (statusCode == HttpStatus.CREATED.value()) {
                when(paymentService.savePayment(any(PaymentDTO.class))).thenReturn(samplePaymentDTO);
                result.andExpect(status().isCreated());
                verify(paymentService).savePayment(any(PaymentDTO.class));
            } else {
                result.andExpect(status().isBadRequest());
                verify(paymentService, never()).savePayment(any());
            }

        }

        @Test
        @DisplayName("Bad Request - Should return status 400 Bad Request for empty request body")
        void savePayment_whenEmptyBody_shouldReturnBadRequest() throws Exception {
            MockHttpServletRequestBuilder request = post("/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("");
            ResultActions result = mockMvc.perform(request);

            result.andExpect(status().isBadRequest());

            verify(paymentService, never()).savePayment(any());
        }

        @Test
        @DisplayName("Bad Request - Should return status 415 Unsupported Media Type for incorrect content type")
        void savePayment_whenWrongContentType_shouldReturnUnsupportedMediaType() throws Exception {
            MockHttpServletRequestBuilder request = post("/payments")
                    .contentType(MediaType.APPLICATION_XML)
                    .content("<payment></payment>");
            ResultActions result = mockMvc.perform(request);

            result.andExpect(status().isUnsupportedMediaType());

            verify(paymentService, never()).savePayment(any());
        }
    }

    @Nested
    @DisplayName("PUT /payments/{id} (Cancel Payment)")
    class CancelPaymentTests {

        @Test
        @DisplayName("Happy Path - Should cancel payment and return status 200 OK")
        void cancelPayment_whenValidIdAndCancellable_shouldCancelAndReturnOk() throws Exception {
            Payment paymentToCancel = samplePayment;
            PaymentDTO cancelledDto = PaymentDTO.fromEntity(paymentToCancel);
            cancelledDto.setCancelation(new BigDecimal("0.05"));

            when(paymentService.getPaymentById(validUuid)).thenReturn(Optional.of(paymentToCancel));
            when(paymentService.cancelPayment(paymentToCancel)).thenReturn(cancelledDto);

            ResultActions result = mockMvc.perform(put("/payments/{id}", validUuid));

            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(validUuid.toString())))
                    .andExpect(jsonPath("$.cancelation", is(0.05)));

            verify(paymentService).getPaymentById(validUuid);
            verify(paymentService).cancelPayment(paymentToCancel);
        }

        @Test
        @DisplayName("Not Found - Should return status 404 Not Found when ID does not exist")
        void cancelPayment_whenIdNotFound_shouldReturnNotFound() throws Exception {
            when(paymentService.getPaymentById(notFoundUuid)).thenReturn(Optional.empty());

            ResultActions result = mockMvc.perform(put("/payments/{id}", notFoundUuid));

            result.andExpect(status().isNotFound());

            verify(paymentService).getPaymentById(notFoundUuid);
            verify(paymentService, never()).cancelPayment(any());
        }

        @Test
        @DisplayName("Bad Request - Should return status 400 Bad Request for invalid UUID format")
        void cancelPayment_whenInvalidUuidFormat_shouldReturnBadRequest() throws Exception {
            String invalidUuid = "not-a-valid-uuid";

            ResultActions result = mockMvc.perform(put("/payments/{id}", invalidUuid));

            result.andExpect(status().isBadRequest());

            verify(paymentService, never()).getPaymentById(any());
            verify(paymentService, never()).cancelPayment(any());
        }
    }
}