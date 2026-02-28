package com.bank.account.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.account.dto.AccountResponse;
import com.bank.account.service.AccountService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "app.security.enabled=false",
    "management.tracing.enabled=false"
})
class AccountControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private AccountService accountService;

  @Test
  void createShouldReturnCreated() throws Exception {
    UUID id = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    AccountResponse response = new AccountResponse(
        id, customerId, "ACCWEB001", "USD", BigDecimal.valueOf(500.00), "ACTIVE",
        OffsetDateTime.now(), OffsetDateTime.now());

    when(accountService.create(any())).thenReturn(response);

    mockMvc.perform(post("/api/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "customerId":"%s",
                  "accountNumber":"ACCWEB001",
                  "currency":"USD",
                  "openingBalance":500.00
                }
                """.formatted(customerId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accountNumber").value("ACCWEB001"));
  }

  @Test
  void createShouldReturnBadRequestForInvalidPayload() throws Exception {
    mockMvc.perform(post("/api/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "customerId":"not-a-uuid",
                  "accountNumber":"",
                  "currency":"US",
                  "openingBalance":-1
                }
                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getShouldReturnNotFound() throws Exception {
    UUID id = UUID.randomUUID();
    when(accountService.get(eq(id))).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

    mockMvc.perform(get("/api/accounts/{id}", id))
        .andExpect(status().isNotFound());
  }
}
