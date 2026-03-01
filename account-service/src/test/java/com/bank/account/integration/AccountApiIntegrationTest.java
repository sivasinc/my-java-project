package com.bank.account.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.account.dto.AccountResponse;
import com.bank.account.dto.CreateAccountRequest;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountApiIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
      .withDatabaseName("accounts_db")
      .withUsername("bank_admin")
      .withPassword("bank_admin_pass");

  @DynamicPropertySource
  static void setProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("app.security.enabled", () -> "false");
    registry.add("management.tracing.enabled", () -> "false");
  }

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void createThenGetAccountShouldSucceed() {
    CreateAccountRequest request = new CreateAccountRequest(
        UUID.randomUUID(), "ACCINT001", "USD", BigDecimal.valueOf(900.00));

    ResponseEntity<AccountResponse> created = restTemplate.postForEntity("/api/accounts", request, AccountResponse.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody()).isNotNull();
    UUID id = created.getBody().id();
    assertThat(id).isNotNull();

    ResponseEntity<AccountResponse> fetched = restTemplate.getForEntity("/api/accounts/{id}", AccountResponse.class, id);
    assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(fetched.getBody()).isNotNull();
    assertThat(fetched.getBody().accountNumber()).isEqualTo("ACCINT001");
  }
}
