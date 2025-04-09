package com.example.demo.Service;

import com.example.demo.Exception.CancellationNotAllowedException;
import com.example.demo.Exception.PaymentTypeIndeterminateException;
import com.example.demo.Model.Payment;
import com.example.demo.Strategy.CancellationFeeStrategy;
import com.example.demo.Strategy.CancellationFeeStrategyType1;
import com.example.demo.Strategy.CancellationFeeStrategyType2;
import com.example.demo.Strategy.CancellationFeeStrategyType3;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class CancellationService {

    private final CancellationFeeStrategy type1Strategy = new CancellationFeeStrategyType1();
    private final CancellationFeeStrategy type2Strategy = new CancellationFeeStrategyType2();
    private final CancellationFeeStrategy type3Strategy = new CancellationFeeStrategyType3();

    public Payment cancelPayment(Payment payment) {
        LocalDateTime cancellationTime = LocalDateTime.now();
        LocalDate cancellationDate = cancellationTime.toLocalDate();
        LocalDate creationDate = payment.getCreationDate().toLocalDate();

        if (!creationDate.equals(cancellationDate)) {
            throw new CancellationNotAllowedException("Payment can only be cancelled on the same day it was created. Creation date: " + creationDate + ", Attempted cancellation date: " + cancellationDate);
        }

        if (payment.getCancelation() != null) {
            throw new CancellationNotAllowedException("Payment with ID " + payment.getId() + " has already been cancelled.");
        }

        CancellationFeeStrategy selectedStrategy = getCancellationFeeStrategy(payment);

        BigDecimal cancellationFee = selectedStrategy.calculateFee(payment, cancellationTime);

        payment.setCancelation(cancellationFee);

        return payment;
    }

    private CancellationFeeStrategy getCancellationFeeStrategy(Payment payment) {
        CancellationFeeStrategy selectedStrategy;
        switch (payment.getType()){
            case 1:
                selectedStrategy = type1Strategy;
                break;
            case 2:
                selectedStrategy = type2Strategy;
                break;
            case 3:
                selectedStrategy = type3Strategy;
                break;
            default:
                throw new PaymentTypeIndeterminateException("Could not determine payment type for cancellation fee calculation for payment ID: " + payment.getId() + ". Currency: " + payment.getCurrency() + ", BIC present: " + (payment.getBicCode() != null && !payment.getBicCode().isBlank()) + ", Details present: " + (payment.getDetails() != null && !payment.getDetails().isBlank()));
        }
        return selectedStrategy;
    }
}