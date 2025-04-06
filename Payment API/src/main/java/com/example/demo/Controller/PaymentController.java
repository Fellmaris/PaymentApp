package com.example.demo.Controller;

import com.example.demo.DTO.PaymentDTO;
import com.example.demo.Model.Payment;
import com.example.demo.Service.GeoIpService;
import com.example.demo.Service.PaymentService;
import com.example.demo.Util.IpAddressUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;
    private final IpAddressUtil ipAddressUtil;
    private final GeoIpService geoIpService;

    @Autowired
    public PaymentController(PaymentService paymentService,
                             IpAddressUtil ipAddressUtil,
                             GeoIpService geoIpService) {
        this.paymentService = paymentService;
        this.ipAddressUtil = ipAddressUtil;
        this.geoIpService = geoIpService;
    }

    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments(HttpServletRequest request) {
        try {
            String clientIp = ipAddressUtil.getClientIpAddress(request);
            if (!"Unknown".equals(clientIp)  && !ipAddressUtil.isLocalhost(clientIp)) {
                String country = geoIpService.getCountryFromIp(clientIp)
                        .orElse("Unknown");

                log.info("User connection from country: {} (IP: {}) accessing payment list.", country, clientIp);
            } else {
                log.debug("Skipping GeoIP logging for internal/unknown IP: {}", clientIp);
            }
        } catch (Exception e) {
            log.error("Error during GeoIP lookup for payment list access: {}", e.getMessage(), e);
        }

        List<Payment> payments = paymentService.getAllNonCancelledPayments();
        return new ResponseEntity<>(payments, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable UUID id) {
        Optional<Payment> payment = paymentService.getPaymentById(id);
        return payment.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PaymentDTO> savePayment(@Valid @RequestBody PaymentDTO paymentDTO) {
        PaymentDTO savedPayment = paymentService.savePayment(paymentDTO);
        return new ResponseEntity<>(savedPayment, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentDTO> cancelPayment(@PathVariable UUID id) {
        Optional<Payment> payment = paymentService.getPaymentById(id);
        if (payment.isPresent()) {
            return ResponseEntity.ok(paymentService.cancelPayment(payment.get()));
        }
        return ResponseEntity.notFound().build();
    }

}