package com.bank.account.service;

import com.bank.account.dto.AccountResponse;
import com.bank.account.dto.CreateAccountRequest;
import com.bank.account.entity.Account;
import com.bank.account.repository.AccountRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

  private final AccountRepository accountRepository;

  public AccountService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  public AccountResponse create(CreateAccountRequest request) {
    accountRepository.findByAccountNumber(request.accountNumber()).ifPresent(existing -> {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Account number already exists");
    });

    OffsetDateTime now = OffsetDateTime.now();
    Account account = new Account();
    account.setCustomerId(request.customerId());
    account.setAccountNumber(request.accountNumber());
    account.setCurrency(request.currency());
    account.setBalance(request.openingBalance());
    account.setStatus("ACTIVE");
    account.setCreatedAt(now);
    account.setUpdatedAt(now);

    Account saved = accountRepository.save(account);
    return map(saved);
  }

  public AccountResponse get(UUID id) {
    return accountRepository.findById(id)
        .map(this::map)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
  }

  private AccountResponse map(Account account) {
    return new AccountResponse(
        account.getId(),
        account.getCustomerId(),
        account.getAccountNumber(),
        account.getCurrency(),
        account.getBalance(),
        account.getStatus(),
        account.getCreatedAt(),
        account.getUpdatedAt());
  }
}
