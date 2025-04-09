package com.example.demo.Service;
import com.example.demo.DTO.PaymentDTO;
import com.example.demo.Model.Payment;
import com.example.demo.Repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CancellationService cancelationService;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, CancellationService cancelationService) {
        this.paymentRepository = paymentRepository;
        this.cancelationService = cancelationService;
    }

    public PaymentDTO savePayment(PaymentDTO paymentDTO) {
        Payment payment = paymentDTO.toEntity();
        Payment savedPayment = paymentRepository.save(payment);
        return PaymentDTO.fromEntity(savedPayment);
    }

    public PaymentDTO cancelPayment(Payment payment) {
        Payment savedPayment = paymentRepository.save(cancelationService.cancelPayment(payment));
        return PaymentDTO.fromEntity(savedPayment);
    }

    public List<Payment> getAllNonCancelledPayments() {
        return paymentRepository.findByCancelationIsNull();
    }

    public Optional<Payment> getPaymentById(UUID id) {
        return paymentRepository.findById(id);
    }

}
