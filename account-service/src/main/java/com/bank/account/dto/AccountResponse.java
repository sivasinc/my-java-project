package com.bank.account.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountResponse(
    UUID id,
    UUID customerId,
    String accountNumber,
    String currency,
    BigDecimal balance,
    String status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
