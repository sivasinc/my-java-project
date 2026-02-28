package com.bank.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.account.dto.AccountResponse;
import com.bank.account.dto.CreateAccountRequest;
import com.bank.account.entity.Account;
import com.bank.account.repository.AccountRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  @Mock
  private AccountRepository accountRepository;

  private AccountService accountService;

  @BeforeEach
  void setUp() {
    accountService = new AccountService(accountRepository);
  }

  @Test
  void createShouldPersistAndReturnAccount() {
    UUID customerId = UUID.randomUUID();
    CreateAccountRequest request = new CreateAccountRequest(
        customerId, "ACCUT001", "USD", BigDecimal.valueOf(1000.00));

    when(accountRepository.findByAccountNumber("ACCUT001")).thenReturn(Optional.empty());
    when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
      Account toSave = invocation.getArgument(0, Account.class);
      Account saved = new Account();
      saved.setCustomerId(toSave.getCustomerId());
      saved.setAccountNumber(toSave.getAccountNumber());
      saved.setCurrency(toSave.getCurrency());
      saved.setBalance(toSave.getBalance());
      saved.setStatus(toSave.getStatus());
      saved.setCreatedAt(toSave.getCreatedAt());
      saved.setUpdatedAt(toSave.getUpdatedAt());
      return saved;
    });

    AccountResponse response = accountService.create(request);

    assertThat(response.customerId()).isEqualTo(customerId);
    assertThat(response.accountNumber()).isEqualTo("ACCUT001");
    assertThat(response.currency()).isEqualTo("USD");
    assertThat(response.balance()).isEqualByComparingTo("1000.00");
    assertThat(response.status()).isEqualTo("ACTIVE");
    verify(accountRepository).save(any(Account.class));
  }

  @Test
  void createShouldReturnConflictWhenAccountNumberExists() {
    CreateAccountRequest request = new CreateAccountRequest(
        UUID.randomUUID(), "ACCUT002", "USD", BigDecimal.valueOf(10.00));

    when(accountRepository.findByAccountNumber("ACCUT002")).thenReturn(Optional.of(new Account()));

    ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> accountService.create(request));
    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(ex.getReason()).contains("already exists");
  }

  @Test
  void getShouldReturnNotFoundForMissingAccount() {
    UUID id = UUID.randomUUID();
    when(accountRepository.findById(id)).thenReturn(Optional.empty());

    ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> accountService.get(id));
    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void getShouldReturnAccountWhenPresent() throws Exception {
    UUID id = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now();
    Account account = new Account();
    java.lang.reflect.Field idField = Account.class.getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(account, id);
    account.setCustomerId(customerId);
    account.setAccountNumber("ACCUT003");
    account.setCurrency("USD");
    account.setBalance(BigDecimal.valueOf(250.25));
    account.setStatus("ACTIVE");
    account.setCreatedAt(now);
    account.setUpdatedAt(now);

    when(accountRepository.findById(id)).thenReturn(Optional.of(account));

    AccountResponse response = accountService.get(id);
    assertThat(response.id()).isEqualTo(id);
    assertThat(response.accountNumber()).isEqualTo("ACCUT003");
    assertThat(response.balance()).isEqualByComparingTo("250.25");
  }
}
