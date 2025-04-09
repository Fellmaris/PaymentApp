package com.example.demo.Strategy;

import com.example.demo.Model.Payment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CancellationFeeStrategy {
    BigDecimal calculateFee(Payment payment, LocalDateTime cancellationTime);
}