package com.example.demo.Strategy;

import com.example.demo.Model.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CancellationFeeStrategyTest {

    private Payment payment;
    private LocalDateTime creationTime;

    @BeforeEach
    void setUp() {
        payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setAmount(new BigDecimal("100.00"));
        creationTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        payment.setCreationDate(creationTime);
    }

    @DisplayName("Type 1 Strategy Tests (Coefficient 0.05)")
    @Nested
    class Type1Tests {
        private final CancellationFeeStrategy strategy = new CancellationFeeStrategyType1();
        private final BigDecimal coefficient = new BigDecimal("0.05");

        @ParameterizedTest(name = "Hours Passed: {0}, Expected Fee: {1}")
        @CsvSource({
                "0,  0.00",
                "1,  0.05",
                "5,  0.25",
                "23, 1.15"
        })
        void calculateFee_Type1(long hoursPassed, String expectedFeeStr) {
            LocalDateTime cancellationTime = creationTime.plusHours(hoursPassed).plusMinutes(1);
            BigDecimal expectedFee = new BigDecimal(expectedFeeStr);

            BigDecimal actualFee = strategy.calculateFee(payment, cancellationTime);

            assertThat(actualFee).isEqualByComparingTo(expectedFee);
        }

        @Test
        @DisplayName("Type 1 - Less than one hour results in zero fee")
        void calculateFee_Type1_LessThanOneHour() {
            LocalDateTime cancellationTime = creationTime.plusMinutes(59);
            BigDecimal actualFee = strategy.calculateFee(payment, cancellationTime);
            assertThat(actualFee).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Type 1 - Negative duration (clock skew) results in zero fee")
        void calculateFee_Type1_NegativeDuration() {
            LocalDateTime cancellationTime = creationTime.minusHours(1);
            BigDecimal actualFee = strategy.calculateFee(payment, cancellationTime);
            assertThat(actualFee).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }


    @DisplayName("Type 2 Strategy Tests (Coefficient 0.10)")
    @Nested
    class Type2Tests {
        private final CancellationFeeStrategy strategy = new CancellationFeeStrategyType2();
        private final BigDecimal coefficient = new BigDecimal("0.10");

        @ParameterizedTest(name = "Hours Passed: {0}, Expected Fee: {1}")
        @CsvSource({
                "0,  0.00",
                "1,  0.10",
                "5,  0.50",
                "23, 2.30"
        })
        void calculateFee_Type2(long hoursPassed, String expectedFeeStr) {
            LocalDateTime cancellationTime = creationTime.plusHours(hoursPassed).plusMinutes(1);
            BigDecimal expectedFee = new BigDecimal(expectedFeeStr);
            BigDecimal actualFee = strategy.calculateFee(payment, cancellationTime);
            assertThat(actualFee).isEqualByComparingTo(expectedFee);
        }

        @Test
        @DisplayName("Type 2 - Less than one hour results in zero fee")
        void calculateFee_Type2_LessThanOneHour() {
            LocalDateTime cancellationTime = creationTime.plusMinutes(59);
            BigDecimal actualFee = strategy.calculateFee(payment, cancellationTime);
            assertThat(actualFee).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Type 2 - Negative duration (clock skew) results in zero fee")
        void calculateFee_Type2_NegativeDuration() {
            LocalDateTime cancellationTime = creationTime.minusHours(1);
            BigDecimal actualFee = strategy.calculateFee(payment, cancellationTime);
            assertThat(actualFee).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @DisplayName("Type 3 Strategy Tests (Coefficient 0.15)")
    @Nested
    class Type3Tests {
        private final CancellationFeeStrategy strategy = new CancellationFeeStrategyType3();
        private final BigDecimal coefficient = new BigDecimal("0.15");

        @ParameterizedTest(name = "Hours Passed: {0}, Expected Fee: {1}")
        @CsvSource({
                "0,  0.00",
                "1,  0.15",
                "5,  0.75",
                "23, 3.45"
        })
        void calculateFee_Type3(long hoursPassed, String expectedFeeStr) {
            LocalDateTime cancellationTime = creationTime.plusHours(hoursPassed).plusMinutes(1);
            BigDecimal expectedFee = new BigDecimal(expectedFeeStr);
            BigDecimal actualFee = strategy.calculateFee(payment, cancellationTime);
            assertThat(actualFee).isEqualByComparingTo(expectedFee);
        }

        @Test
        @DisplayName("Type 3 - Less than one hour results in zero fee")
        void calculateFee_Type3_LessThanOneHour() {
            LocalDateTime cancellationTime = creationTime.plusMinutes(59);
            BigDecimal actualFee = strategy.calculateFee(payment, cancellationTime);
            assertThat(actualFee).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Type 3 - Negative duration (clock skew) results in zero fee")
        void calculateFee_Type3_NegativeDuration() {
            LocalDateTime cancellationTime = creationTime.minusHours(1);
            BigDecimal actualFee = strategy.calculateFee(payment, cancellationTime);
            assertThat(actualFee).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}