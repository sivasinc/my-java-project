package com.bank.account.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.account.entity.Account;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AccountRepositoryTest {

  @Autowired
  private AccountRepository accountRepository;

  @Test
  void findByAccountNumberShouldReturnSavedAccount() {
    Account account = new Account();
    account.setCustomerId(UUID.randomUUID());
    account.setAccountNumber("ACCREPO001");
    account.setCurrency("USD");
    account.setBalance(BigDecimal.valueOf(120.50));
    account.setStatus("ACTIVE");
    account.setCreatedAt(OffsetDateTime.now());
    account.setUpdatedAt(OffsetDateTime.now());
    accountRepository.save(account);

    var found = accountRepository.findByAccountNumber("ACCREPO001");
    assertThat(found).isPresent();
    assertThat(found.get().getCurrency()).isEqualTo("USD");
  }

  @Test
  void findByAccountNumberShouldReturnEmptyWhenMissing() {
    var found = accountRepository.findByAccountNumber("ACCREPO404");
    assertThat(found).isEmpty();
  }
}
