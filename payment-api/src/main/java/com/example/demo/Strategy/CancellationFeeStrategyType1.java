package com.example.demo.Strategy;

import com.example.demo.Model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

public class CancellationFeeStrategyType1 implements CancellationFeeStrategy {

    private static final BigDecimal COEFFICIENT = new BigDecimal("0.05");
    private static final Logger log = LoggerFactory.getLogger(CancellationFeeStrategyType1.class);

    @Override
    public BigDecimal calculateFee(Payment payment, LocalDateTime cancellationTime) {
        Duration duration = Duration.between(payment.getCreationDate(), cancellationTime);
        long fullHours = duration.toHours();

        if (fullHours < 0) {
            log.error("Error processing time (likely due to clock issues). The time passed creation date is : {}", duration);
            fullHours = 0;
        }

        return BigDecimal.valueOf(fullHours).multiply(COEFFICIENT);
    }
}