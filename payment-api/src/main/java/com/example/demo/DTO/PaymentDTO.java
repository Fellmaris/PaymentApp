package com.example.demo.DTO;

import com.example.demo.Model.Payment;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {

    private UUID id;
    @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "Invalid amount format. Use digits with up to two decimal places (e.g., 123.45)")
    private String amount;
    private BigDecimal cancelation;
    private Payment.Currency currency;
    private String debtorIban;
    private String creditorIban;
    private String details;
    private String bicCode;
    private int type;
    private LocalDateTime creationDate;

    public void setType(int type) {
        this.type = type;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public void setCancelation(BigDecimal cancelation) {
        this.cancelation = cancelation;
    }

    public void setCurrency(Payment.Currency currency) {
        this.currency = currency;
    }

    public void setDebtorIban(String debtorIban) {
        this.debtorIban = debtorIban;
    }

    public String getCreditorIban() {
        return creditorIban;
    }

    public void setCreditorIban(String creditorIban) {
        this.creditorIban = creditorIban;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public void setBicCode(String bicCode) {
        this.bicCode = bicCode;
    }

    public UUID getId() {
        return id;
    }

    public String getAmount() {
        return amount;
    }

    public BigDecimal getCancelation() {
        return cancelation;
    }

    public Payment.Currency getCurrency() {
        return currency;
    }

    public String getDebtorIban() {
        return debtorIban;
    }

    public String getDetails() {
        return details;
    }

    public String getBicCode() {
        return bicCode;
    }

    public int getType() {
        return type;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public Payment toEntity() {
        Payment payment = new Payment();
        payment.setId(this.id);
        payment.setAmount(new BigDecimal(this.amount.trim()));
        payment.setCancelation(this.cancelation);
        payment.setCurrency(this.currency);
        payment.setDebtorIban(this.debtorIban);
        payment.setCreditorIban(this.creditorIban);
        payment.setDetails(this.details);
        payment.setBicCode(this.bicCode);
        payment.setType(this.type);
        payment.setCreationDate(LocalDateTime.now());
        return payment;
    }

    public static PaymentDTO fromEntity(Payment payment) {
         PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setId(payment.getId());
        paymentDTO.setAmount(payment.getAmount().toString());
        paymentDTO.setCancelation(payment.getCancelation());
        paymentDTO.setCurrency(payment.getCurrency());
        paymentDTO.setDebtorIban(payment.getDebtorIban());
        paymentDTO.setCreditorIban(payment.getCreditorIban());
        paymentDTO.setDetails(payment.getDetails());
        paymentDTO.setBicCode(payment.getBicCode());
        paymentDTO.setType(payment.getType());
        paymentDTO.setCreationDate(payment.getCreationDate());
        return paymentDTO;
    }
}
