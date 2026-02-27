package com.bank.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateAccountRequest(
    @NotNull UUID customerId,
    @NotBlank String accountNumber,
    @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
    @NotNull @DecimalMin("0.00") BigDecimal openingBalance) {}
