package com.example.demo.Service;

import com.example.demo.DTO.PaymentDTO;
import com.example.demo.Exception.CancellationNotAllowedException;
import com.example.demo.Model.Payment;
import com.example.demo.Repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CancellationService cancellationService;

    @InjectMocks
    private PaymentService paymentService;

    private Payment samplePayment;
    private PaymentDTO samplePaymentDTO;
    private UUID testUuid;

    @BeforeEach
    void setUp() {
        testUuid = UUID.randomUUID();

        samplePayment = new Payment();
        samplePayment.setId(testUuid);
        samplePayment.setAmount(new BigDecimal("100.50"));
        samplePayment.setCurrency(Payment.Currency.EUR);
        samplePayment.setDebtorIban("DE89370400440532013000");
        samplePayment.setCreditorIban("DE89370400440532013001");
        samplePayment.setType(1);
        samplePayment.setCreationDate(LocalDateTime.now().minusHours(1));
        samplePayment.setCancelation(null);

        samplePaymentDTO = new PaymentDTO();
        samplePaymentDTO.setId(testUuid);
        samplePaymentDTO.setAmount("100.50");
        samplePaymentDTO.setCurrency(Payment.Currency.EUR);
        samplePaymentDTO.setDebtorIban("DE89370400440532013000");
        samplePaymentDTO.setCreditorIban("DE89370400440532013001");
        samplePaymentDTO.setType(1);
    }

    @Nested
    @DisplayName("savePayment Tests")
    class SavePaymentTests {

        @Test
        @DisplayName("Happy Path - Should convert DTO, save entity, convert back, and return DTO")
        void savePayment_happyPath() {
            PaymentDTO inputDto = new PaymentDTO();
            inputDto.setAmount("200.00");
            inputDto.setCurrency(Payment.Currency.USD);
            inputDto.setDebtorIban("US123");
            inputDto.setCreditorIban("US456");
            inputDto.setType(2);

            Payment paymentToSave = inputDto.toEntity();
            assertThat(paymentToSave.getCreationDate()).isNotNull();

            Payment savedPayment = new Payment();
            savedPayment.setId(UUID.randomUUID());
            savedPayment.setAmount(new BigDecimal("200.00"));
            savedPayment.setCurrency(Payment.Currency.USD);
            savedPayment.setDebtorIban("US123");
            savedPayment.setCreditorIban("US456");
            savedPayment.setType(2);
            savedPayment.setCreationDate(paymentToSave.getCreationDate());

            when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

            PaymentDTO resultDto = paymentService.savePayment(inputDto);

            verify(paymentRepository).save(argThat(p ->
                    p.getAmount().compareTo(new BigDecimal("200.00")) == 0 &&
                            p.getCurrency() == Payment.Currency.USD &&
                            p.getId() == null &&
                            p.getCreationDate() != null
            ));

            assertThat(resultDto).isNotNull();
            assertThat(resultDto.getId()).isEqualTo(savedPayment.getId());
            assertThat(resultDto.getAmount()).isEqualTo("200.00");
            assertThat(resultDto.getCreationDate()).isEqualTo(savedPayment.getCreationDate());
            assertThat(resultDto.getCancelation()).isNull();
        }

        @Test
        @DisplayName("Edge Case - Should handle DTO with existing ID (overwrite)")
        void savePayment_dtoWithId() {
            UUID existingId = UUID.randomUUID();
            PaymentDTO inputDto = samplePaymentDTO;
            inputDto.setId(existingId);
            inputDto.setAmount("99.99");

            Payment paymentToSave = inputDto.toEntity();
            paymentToSave.setId(existingId);

            Payment savedPayment = new Payment();
            savedPayment.setId(existingId);
            savedPayment.setAmount(new BigDecimal("99.99"));
            savedPayment.setCreationDate(LocalDateTime.now());


            when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

            PaymentDTO resultDto = paymentService.savePayment(inputDto);

            verify(paymentRepository).save(argThat(p -> p.getId().equals(existingId) && p.getAmount().compareTo(new BigDecimal("99.99")) == 0));
            assertThat(resultDto.getId()).isEqualTo(existingId);
            assertThat(resultDto.getAmount()).isEqualTo("99.99");
        }

        @Test
        @DisplayName("Null Test - Should throw NullPointerException if input DTO is null")
        void savePayment_nullDto() {
            PaymentDTO nullDto = null;

            assertThatThrownBy(() -> paymentService.savePayment(nullDto))
                    .isInstanceOf(NullPointerException.class);

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Error Case - Should propagate DataAccessException from repository")
        void savePayment_repositoryThrowsException() {
            PaymentDTO inputDto = samplePaymentDTO;
            when(paymentRepository.save(any(Payment.class))).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("DB connection failed"));

            assertThatThrownBy(() -> paymentService.savePayment(inputDto))
                    .isInstanceOf(org.springframework.dao.DataAccessResourceFailureException.class);
        }

        @Test
        @DisplayName("Error Case - Should throw NumberFormatException if amount in DTO is invalid (before service call)")
        void savePayment_invalidAmountInDto() {
            PaymentDTO inputDto = new PaymentDTO();
            inputDto.setAmount("invalid-number");

            assertThatThrownBy(() -> paymentService.savePayment(inputDto))
                    .isInstanceOf(NumberFormatException.class);

            verify(paymentRepository, never()).save(any());
        }
    }


    @Nested
    @DisplayName("cancelPayment Tests")
    class CancelPaymentTests {

        @Test
        @DisplayName("Happy Path - Should call cancellationService, save, convert, and return DTO")
        void cancelPayment_happyPath() {
            Payment paymentToCancel = samplePayment;
            Payment cancelledPayment = new Payment();
            cancelledPayment.setId(paymentToCancel.getId());
            cancelledPayment.setAmount(paymentToCancel.getAmount());
            cancelledPayment.setCurrency(paymentToCancel.getCurrency());
            cancelledPayment.setDebtorIban(paymentToCancel.getDebtorIban());
            cancelledPayment.setCreditorIban(paymentToCancel.getCreditorIban());
            cancelledPayment.setType(paymentToCancel.getType());
            cancelledPayment.setCreationDate(paymentToCancel.getCreationDate());
            cancelledPayment.setCancelation(new BigDecimal("0.05"));

            when(cancellationService.cancelPayment(paymentToCancel)).thenReturn(cancelledPayment);
            when(paymentRepository.save(cancelledPayment)).thenReturn(cancelledPayment);

            PaymentDTO resultDto = paymentService.cancelPayment(paymentToCancel);

            verify(cancellationService).cancelPayment(paymentToCancel);
            verify(paymentRepository).save(cancelledPayment);

            assertThat(resultDto).isNotNull();
            assertThat(resultDto.getId()).isEqualTo(paymentToCancel.getId());
            assertThat(resultDto.getCancelation()).isEqualTo(new BigDecimal("0.05"));
        }

        @Test
        @DisplayName("Error Case - Should propagate CancellationNotAllowedException from cancellationService")
        void cancelPayment_whenCancellationNotAllowed() {
            Payment paymentToCancel = samplePayment;
            CancellationNotAllowedException exception = new CancellationNotAllowedException("Already cancelled");

            when(cancellationService.cancelPayment(paymentToCancel)).thenThrow(exception);

            assertThatThrownBy(() -> paymentService.cancelPayment(paymentToCancel))
                    .isInstanceOf(CancellationNotAllowedException.class)
                    .isEqualTo(exception);

            verify(cancellationService).cancelPayment(paymentToCancel);
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Null Test - Should throw NullPointerException if input Payment is null")
        void cancelPayment_nullPayment() {
            Payment nullPayment = null;

            when(cancellationService.cancelPayment(null)).thenThrow(NullPointerException.class);

            assertThatThrownBy(() -> paymentService.cancelPayment(nullPayment))
                    .isInstanceOf(NullPointerException.class);

            verify(cancellationService).cancelPayment(null);
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Error Case - Should propagate DataAccessException from repository save")
        void cancelPayment_repositoryThrowsExceptionOnSave() {
            Payment paymentToCancel = samplePayment;
            Payment cancelledPayment = new Payment();
            cancelledPayment.setId(paymentToCancel.getId());
            cancelledPayment.setCancelation(new BigDecimal("0.10"));


            when(cancellationService.cancelPayment(paymentToCancel)).thenReturn(cancelledPayment);
            when(paymentRepository.save(cancelledPayment)).thenThrow(new org.springframework.dao.OptimisticLockingFailureException("Concurrency issue"));

            assertThatThrownBy(() -> paymentService.cancelPayment(paymentToCancel))
                    .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);

            verify(cancellationService).cancelPayment(paymentToCancel);
            verify(paymentRepository).save(cancelledPayment);
        }
    }

    @Nested
    @DisplayName("getAllNonCancelledPayments Tests")
    class GetAllNonCancelledPaymentsTests {

        @Test
        @DisplayName("Happy Path - Should return list from repository")
        void getAllNonCancelledPayments_happyPath() {
            List<Payment> payments = List.of(samplePayment);
            when(paymentRepository.findByCancelationIsNull()).thenReturn(payments);

            List<Payment> result = paymentService.getAllNonCancelledPayments();

            verify(paymentRepository).findByCancelationIsNull();
            assertThat(result).isNotNull().isEqualTo(payments);
            assertThat(result.get(0).getCancelation()).isNull();
        }

        @Test
        @DisplayName("Edge Case - Should return empty list when repository returns empty")
        void getAllNonCancelledPayments_emptyList() {
            when(paymentRepository.findByCancelationIsNull()).thenReturn(Collections.emptyList());

            List<Payment> result = paymentService.getAllNonCancelledPayments();

            verify(paymentRepository).findByCancelationIsNull();
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Error Case - Should propagate DataAccessException from repository")
        void getAllNonCancelledPayments_repositoryThrowsException() {
            when(paymentRepository.findByCancelationIsNull()).thenThrow(new org.springframework.dao.InvalidDataAccessApiUsageException("Query error"));

            assertThatThrownBy(() -> paymentService.getAllNonCancelledPayments())
                    .isInstanceOf(org.springframework.dao.InvalidDataAccessApiUsageException.class);
        }
    }

    @Nested
    @DisplayName("getPaymentById Tests")
    class GetPaymentByIdTests {

        @Test
        @DisplayName("Happy Path - Should return Optional with Payment when found")
        void getPaymentById_whenFound() {
            when(paymentRepository.findById(testUuid)).thenReturn(Optional.of(samplePayment));

            Optional<Payment> result = paymentService.getPaymentById(testUuid);

            verify(paymentRepository).findById(testUuid);
            assertThat(result).isPresent().contains(samplePayment);
        }

        @Test
        @DisplayName("Not Found - Should return empty Optional when not found")
        void getPaymentById_whenNotFound() {
            UUID nonExistentUuid = UUID.randomUUID();
            when(paymentRepository.findById(nonExistentUuid)).thenReturn(Optional.empty());

            Optional<Payment> result = paymentService.getPaymentById(nonExistentUuid);

            verify(paymentRepository).findById(nonExistentUuid);
            assertThat(result).isNotPresent();
        }

        @Test
        @DisplayName("Null Test - Should likely throw IllegalArgumentException if ID is null (from repo)")
        void getPaymentById_nullId() {
            UUID nullUuid = null;
            when(paymentRepository.findById(null)).thenThrow(new IllegalArgumentException("ID must not be null"));

            assertThatThrownBy(() -> paymentService.getPaymentById(nullUuid))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(paymentRepository).findById(null);
        }

        @Test
        @DisplayName("Error Case - Should propagate DataAccessException from repository")
        void getPaymentById_repositoryThrowsException() {
            when(paymentRepository.findById(testUuid)).thenThrow(new org.springframework.dao.QueryTimeoutException("Timeout"));

            assertThatThrownBy(() -> paymentService.getPaymentById(testUuid))
                    .isInstanceOf(org.springframework.dao.QueryTimeoutException.class);

            verify(paymentRepository).findById(testUuid);
        }
    }
}