package com.example.demo.DTO;

import com.example.demo.Model.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class PaymentDTOTest {

    private Payment paymentEntity;
    private PaymentDTO paymentDTO;
    private UUID id = UUID.randomUUID();
    private LocalDateTime creationDate = LocalDateTime.now().minusDays(1);

    @BeforeEach
    void setUp() {
        paymentEntity = new Payment();
        paymentEntity.setId(id);
        paymentEntity.setAmount(new BigDecimal("123.45"));
        paymentEntity.setCancelation(new BigDecimal("0.50"));
        paymentEntity.setCurrency(Payment.Currency.EUR);
        paymentEntity.setDebtorIban("DE123");
        paymentEntity.setCreditorIban("DE456");
        paymentEntity.setDetails("Entity Details");
        paymentEntity.setBicCode("BICFOO");
        paymentEntity.setType(1);
        paymentEntity.setCreationDate(creationDate);

        paymentDTO = new PaymentDTO();
        paymentDTO.setId(id);
        paymentDTO.setAmount("987.65");
        paymentDTO.setCancelation(new BigDecimal("1.20"));
        paymentDTO.setCurrency(Payment.Currency.USD);
        paymentDTO.setDebtorIban("US789");
        paymentDTO.setCreditorIban("US012");
        paymentDTO.setDetails("DTO Details");
        paymentDTO.setBicCode("BICBAR");
        paymentDTO.setType(2);
        paymentDTO.setCreationDate(creationDate);
    }

    @Nested
    @DisplayName("toEntity Conversion")
    class ToEntityTests {

        @Test
        @DisplayName("Happy Path - Should correctly map all DTO fields to Entity")
        void toEntity_HappyPath() {
            PaymentDTO sourceDto = paymentDTO;

            Payment resultEntity = sourceDto.toEntity();

            assertThat(resultEntity.getId()).isEqualTo(sourceDto.getId());
            assertThat(resultEntity.getAmount()).isEqualTo(new BigDecimal("987.65"));
            assertThat(resultEntity.getCancelation()).isEqualTo(sourceDto.getCancelation());
            assertThat(resultEntity.getCurrency()).isEqualTo(sourceDto.getCurrency());
            assertThat(resultEntity.getDebtorIban()).isEqualTo(sourceDto.getDebtorIban());
            assertThat(resultEntity.getCreditorIban()).isEqualTo(sourceDto.getCreditorIban());
            assertThat(resultEntity.getDetails()).isEqualTo(sourceDto.getDetails());
            assertThat(resultEntity.getBicCode()).isEqualTo(sourceDto.getBicCode());
            assertThat(resultEntity.getType()).isEqualTo(sourceDto.getType());
            assertThat(resultEntity.getCreationDate()).isNotNull();
            assertThat(resultEntity.getCreationDate()).isAfter(creationDate);
        }

        @Test
        @DisplayName("Edge Case - Should handle null fields in DTO gracefully")
        void toEntity_NullFields() {
            PaymentDTO dtoWithNulls = new PaymentDTO();
            dtoWithNulls.setAmount("10.00");
            dtoWithNulls.setCurrency(Payment.Currency.EUR);
            dtoWithNulls.setDebtorIban("DE111");
            dtoWithNulls.setCreditorIban("DE222");

            Payment resultEntity = dtoWithNulls.toEntity();

            assertThat(resultEntity.getId()).isNull();
            assertThat(resultEntity.getAmount()).isEqualTo(new BigDecimal("10.00"));
            assertThat(resultEntity.getCancelation()).isNull();
            assertThat(resultEntity.getCurrency()).isEqualTo(Payment.Currency.EUR);
            assertThat(resultEntity.getDebtorIban()).isEqualTo("DE111");
            assertThat(resultEntity.getCreditorIban()).isEqualTo("DE222");
            assertThat(resultEntity.getDetails()).isNull();
            assertThat(resultEntity.getBicCode()).isNull();
            assertThat(resultEntity.getType()).isEqualTo(0);
            assertThat(resultEntity.getCreationDate()).isNotNull();
        }

        @Test
        @DisplayName("Edge Case - Should handle amount string with leading/trailing spaces")
        void toEntity_AmountWithSpaces() {
            PaymentDTO dtoWithSpaces = new PaymentDTO();
            dtoWithSpaces.setAmount("  55.66  ");
            dtoWithSpaces.setCurrency(Payment.Currency.EUR);
            dtoWithSpaces.setDebtorIban("DE111");
            dtoWithSpaces.setCreditorIban("DE222");


            Payment resultEntity = dtoWithSpaces.toEntity();

            assertThat(resultEntity.getAmount()).isEqualTo(new BigDecimal("55.66"));
        }


        @Test
        @DisplayName("Error Case - Should throw NumberFormatException for invalid amount string")
        void toEntity_InvalidAmountString() {
            PaymentDTO invalidDto = new PaymentDTO();
            invalidDto.setAmount("not-a-number");

            assertThatThrownBy(invalidDto::toEntity)
                    .isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("Null Test - Should throw NullPointerException if amount string is null")
        void toEntity_NullAmountString() {
            PaymentDTO nullAmountDto = new PaymentDTO();
            nullAmountDto.setAmount(null);
            nullAmountDto.setCurrency(Payment.Currency.EUR);
            nullAmountDto.setDebtorIban("DE111");
            nullAmountDto.setCreditorIban("DE222");

            assertThatThrownBy(nullAmountDto::toEntity)
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("fromEntity Conversion")
    class FromEntityTests {

        @Test
        @DisplayName("Happy Path - Should correctly map all Entity fields to DTO")
        void fromEntity_HappyPath() {
            Payment sourceEntity = paymentEntity;

            PaymentDTO resultDto = PaymentDTO.fromEntity(sourceEntity);

            assertThat(resultDto.getId()).isEqualTo(sourceEntity.getId());
            assertThat(resultDto.getAmount()).isEqualTo("123.45");
            assertThat(resultDto.getCancelation()).isEqualTo(sourceEntity.getCancelation());
            assertThat(resultDto.getCurrency()).isEqualTo(sourceEntity.getCurrency());
            assertThat(resultDto.getDebtorIban()).isEqualTo(sourceEntity.getDebtorIban());
            assertThat(resultDto.getCreditorIban()).isEqualTo(sourceEntity.getCreditorIban());
            assertThat(resultDto.getDetails()).isEqualTo(sourceEntity.getDetails());
            assertThat(resultDto.getBicCode()).isEqualTo(sourceEntity.getBicCode());
            assertThat(resultDto.getType()).isEqualTo(sourceEntity.getType());
            assertThat(resultDto.getCreationDate()).isEqualTo(sourceEntity.getCreationDate());
        }

        @Test
        @DisplayName("Edge Case - Should handle null fields in Entity gracefully")
        void fromEntity_NullFields() {
            Payment entityWithNulls = new Payment();
            entityWithNulls.setId(UUID.randomUUID());
            entityWithNulls.setAmount(new BigDecimal("50.00"));
            entityWithNulls.setCurrency(Payment.Currency.EUR);
            entityWithNulls.setDebtorIban("DE111");
            entityWithNulls.setCreditorIban("DE222");
            entityWithNulls.setCreationDate(LocalDateTime.now());

            PaymentDTO resultDto = PaymentDTO.fromEntity(entityWithNulls);

            assertThat(resultDto.getId()).isEqualTo(entityWithNulls.getId());
            assertThat(resultDto.getAmount()).isEqualTo("50.00");
            assertThat(resultDto.getCancelation()).isNull();
            assertThat(resultDto.getCurrency()).isEqualTo(Payment.Currency.EUR);
            assertThat(resultDto.getDebtorIban()).isEqualTo("DE111");
            assertThat(resultDto.getCreditorIban()).isEqualTo("DE222");
            assertThat(resultDto.getDetails()).isNull();
            assertThat(resultDto.getBicCode()).isNull();
            assertThat(resultDto.getType()).isEqualTo(0);
            assertThat(resultDto.getCreationDate()).isEqualTo(entityWithNulls.getCreationDate());
        }

        @Test
        @DisplayName("Edge Case - Should handle BigDecimal scale correctly (e.g., 100 -> '100')")
        void fromEntity_AmountScaleHandling() {
            Payment entity = new Payment();
            entity.setAmount(new BigDecimal("100"));

            PaymentDTO resultDto = PaymentDTO.fromEntity(entity);
            assertThat(resultDto.getAmount()).isEqualTo("100");

            entity.setAmount(new BigDecimal("200.50"));
            resultDto = PaymentDTO.fromEntity(entity);
            assertThat(resultDto.getAmount()).isEqualTo("200.50");

            entity.setAmount(new BigDecimal("300.1"));
            resultDto = PaymentDTO.fromEntity(entity);
            assertThat(resultDto.getAmount()).isEqualTo("300.1");
        }

        @Test
        @DisplayName("Null Test - Should throw NullPointerException if entity is null")
        void fromEntity_NullEntity() {
            Payment nullEntity = null;
            assertThatThrownBy(() -> PaymentDTO.fromEntity(nullEntity))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Null Test - Should throw NullPointerException if mandatory entity field (like amount) is null")
        void fromEntity_NullAmountInEntity() {
            Payment entity = new Payment();
            entity.setAmount(null);
            entity.setCurrency(Payment.Currency.EUR);
            entity.setDebtorIban("DE111");
            entity.setCreditorIban("DE222");
            entity.setCreationDate(LocalDateTime.now());


            assertThatThrownBy(() -> PaymentDTO.fromEntity(entity))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}