package com.example.demo.Service;

import com.example.demo.Exception.CancellationNotAllowedException;
import com.example.demo.Exception.PaymentTypeIndeterminateException;
import com.example.demo.Model.Payment;
import com.example.demo.Strategy.CancellationFeeStrategyType1;
import com.example.demo.Strategy.CancellationFeeStrategyType2;
import com.example.demo.Strategy.CancellationFeeStrategyType3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


class CancellationServiceTest {

    private CancellationService cancellationService;
    private Payment payment;

    @BeforeEach
    void setUp() {
        cancellationService = new CancellationService();
        payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setAmount(new BigDecimal("1000.00"));
        payment.setCurrency(Payment.Currency.EUR);
        payment.setCreationDate(LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 0, 0)));
        payment.setCancelation(null);
    }

    @Nested
    @DisplayName("Fee Calculation Tests")
    class FeeCalculation {

        @Test
        @DisplayName("Type 1 - Should calculate fee based on 0.05 * hours passed")
        void cancelPayment_Type1_CalculatesCorrectFee() {
            payment.setType(1);
            LocalDateTime creationTime = LocalDateTime.now().minusHours(3).minusMinutes(30);
            payment.setCreationDate(creationTime);
            BigDecimal expectedFee = new BigDecimal("0.15");
            CancellationFeeStrategyType1 strategy = new CancellationFeeStrategyType1();
            BigDecimal calculatedFee = strategy.calculateFee(payment, LocalDateTime.now());
            Payment cancelledPayment = cancellationService.cancelPayment(payment);

            assertThat(calculatedFee).isEqualByComparingTo(expectedFee);
            assertThat(cancelledPayment.getCancelation()).isNotNull();
            assertThat(cancelledPayment.getCancelation()).isEqualByComparingTo(expectedFee);
        }

        @Test
        @DisplayName("Type 2 - Should calculate fee based on 0.10 * hours passed")
        void cancelPayment_Type2_CalculatesCorrectFee() {
            payment.setType(2);
            LocalDateTime cancellationTime = payment.getCreationDate().plusHours(5).plusMinutes(15);
            BigDecimal expectedFee = BigDecimal.valueOf(5).multiply(new BigDecimal("0.10"));

            Payment cancelledPayment = cancellationService.cancelPayment(payment);
            BigDecimal fee = new CancellationFeeStrategyType2().calculateFee(payment, cancellationTime);
            cancelledPayment.setCancelation(fee);


            assertThat(cancelledPayment.getCancelation()).isNotNull();
            assertThat(cancelledPayment.getCancelation()).isEqualByComparingTo(expectedFee);
        }

        @Test
        @DisplayName("Type 3 - Should calculate fee based on 0.15 * hours passed")
        void cancelPayment_Type3_CalculatesCorrectFee() {
            payment.setType(3);
            LocalDateTime cancellationTime = payment.getCreationDate().plusHours(1).plusMinutes(59);
            BigDecimal expectedFee = BigDecimal.valueOf(1).multiply(new BigDecimal("0.15"));

            Payment cancelledPayment = cancellationService.cancelPayment(payment);
            BigDecimal fee = new CancellationFeeStrategyType3().calculateFee(payment, cancellationTime);
            cancelledPayment.setCancelation(fee);

            assertThat(cancelledPayment.getCancelation()).isNotNull();
            assertThat(cancelledPayment.getCancelation()).isEqualByComparingTo(expectedFee);
        }

        @Test
        @DisplayName("Edge Case - Should calculate zero fee if cancelled within the first hour")
        void cancelPayment_LessThanOneHour_ZeroFee() {
            payment.setType(1);
            LocalDateTime cancellationTime = payment.getCreationDate().plusMinutes(30);
            BigDecimal expectedFee = BigDecimal.ZERO;

            Payment cancelledPayment = cancellationService.cancelPayment(payment);
            BigDecimal fee = new CancellationFeeStrategyType1().calculateFee(payment, cancellationTime);
            cancelledPayment.setCancelation(fee);

            assertThat(cancelledPayment.getCancelation()).isNotNull();
            assertThat(cancelledPayment.getCancelation().compareTo(expectedFee)).isEqualTo(0);
        }

        @Test
        @DisplayName("Edge case - Should throw CancellationNotAllowedException when cancelling across midnight boundary")
        void cancelPayment_AcrossMidnightBoundary_ThrowsException() { // Renamed for clarity
            payment.setType(1);
            LocalDateTime creationTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 55, 0));
            payment.setCreationDate(creationTime);
            LocalDateTime specificCancellationTime = creationTime.plusMinutes(10); // e.g., 00:05 the next day
            try (MockedStatic<LocalDateTime> mockedLocalDateTime = Mockito.mockStatic(LocalDateTime.class)) {
                mockedLocalDateTime.when(LocalDateTime::now).thenReturn(specificCancellationTime);
                assertThatThrownBy(() -> cancellationService.cancelPayment(payment))
                        .isInstanceOf(CancellationNotAllowedException.class)
                        .hasMessageContaining("Payment can only be cancelled on the same day it was created");
            }
        }

        @Test
        @DisplayName("Boundary Case - Cancellation exactly at creation time - Zero Fee")
        void cancelPayment_ExactCreationTime_ZeroFee() {
            payment.setType(1);
            LocalDateTime cancellationTime = payment.getCreationDate();
            BigDecimal expectedFee = BigDecimal.ZERO;

            Payment cancelledPayment = cancellationService.cancelPayment(payment);
            BigDecimal fee = new CancellationFeeStrategyType1().calculateFee(payment, cancellationTime);
            cancelledPayment.setCancelation(fee);

            assertThat(cancelledPayment.getCancelation()).isNotNull();
            assertThat(cancelledPayment.getCancelation().compareTo(expectedFee)).isEqualTo(0);
        }

        @Test
        @DisplayName("Boundary Case - Cancellation just before next hour - Fee uses full hours passed")
        void cancelPayment_JustBeforeNextHour_UsesFullHours() {
            payment.setType(1);
            LocalDateTime cancellationTime = payment.getCreationDate().plusHours(1).plusMinutes(59).plusSeconds(59);
            BigDecimal expectedFee = BigDecimal.valueOf(1).multiply(new BigDecimal("0.05"));

            Payment cancelledPayment = cancellationService.cancelPayment(payment);
            BigDecimal fee = new CancellationFeeStrategyType1().calculateFee(payment, cancellationTime);
            cancelledPayment.setCancelation(fee);

            assertThat(cancelledPayment.getCancelation()).isNotNull();
            assertThat(cancelledPayment.getCancelation()).isEqualByComparingTo(expectedFee);
        }
    }

    @Nested
    @DisplayName("Validation and Exception Tests")
    class ValidationExceptions {

        @Test
        @DisplayName("Error - Should throw CancellationNotAllowedException if cancelled on a different day")
        void cancelPayment_DifferentDay_ThrowsException() {
            payment.setType(1);
            payment.setCreationDate(LocalDateTime.now().minusDays(1));

            assertThatThrownBy(() -> cancellationService.cancelPayment(payment))
                    .isInstanceOf(CancellationNotAllowedException.class)
                    .hasMessageContaining("Payment can only be cancelled on the same day");
        }

        @Test
        @DisplayName("Error - Should throw CancellationNotAllowedException if payment already cancelled")
        void cancelPayment_AlreadyCancelled_ThrowsException() {
            payment.setType(1);
            payment.setCancelation(new BigDecimal("0.10"));

            assertThatThrownBy(() -> cancellationService.cancelPayment(payment))
                    .isInstanceOf(CancellationNotAllowedException.class)
                    .hasMessageContaining("has already been cancelled");
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 4, -1})
        @DisplayName("Error - Should throw PaymentTypeIndeterminateException for unknown payment type")
        void cancelPayment_UnknownType_ThrowsException(int invalidType) {
            payment.setType(invalidType);

            assertThatThrownBy(() -> cancellationService.cancelPayment(payment))
                    .isInstanceOf(PaymentTypeIndeterminateException.class)
                    .hasMessageContaining("Could not determine payment type");
        }

        @Test
        @DisplayName("Null Test - Should throw NullPointerException if payment is null")
        void cancelPayment_NullPayment_ThrowsNPE() {
            Payment nullPayment = null;

            assertThatThrownBy(() -> cancellationService.cancelPayment(nullPayment))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Happy Path - Does not throw exception for valid types on same day")
        void cancelPayment_ValidTypesSameDay_DoesNotThrow() {
            payment.setType(1);
            payment.setCreationDate(LocalDateTime.now().minusHours(2));

            assertDoesNotThrow(() -> {
                cancellationService.cancelPayment(payment);
            });

            payment.setType(2);
            payment.setCancelation(null);
            assertDoesNotThrow(() -> {
                cancellationService.cancelPayment(payment);
            });

            payment.setType(3);
            payment.setCancelation(null);
            assertDoesNotThrow(() -> {
                cancellationService.cancelPayment(payment);
            });
        }
    }
}