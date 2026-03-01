package com.bank.account.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.bank.account.dto.AccountResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

public class AccountApiStepDefinitions {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private UUID customerId;
  private String accountNumber;
  private String currency;
  private BigDecimal openingBalance;
  private UUID createdAccountId;
  private MvcResult createResult;
  private MvcResult fetchResult;

  @Given("a customer id {string}")
  public void aCustomerId(String value) {
    customerId = UUID.fromString(value);
  }

  @And("account number {string}")
  public void accountNumber(String value) {
    accountNumber = value;
  }

  @And("currency {string}")
  public void currency(String value) {
    currency = value;
  }

  @And("opening balance {double}")
  public void openingBalance(double value) {
    openingBalance = BigDecimal.valueOf(value);
  }

  @When("the client creates the account")
  public void createAccount() throws Exception {
    String body = """
        {
          "customerId":"%s",
          "accountNumber":"%s",
          "currency":"%s",
          "openingBalance":%s
        }
        """.formatted(customerId, accountNumber, currency, openingBalance);

    createResult = mockMvc.perform(
            post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andReturn();

    if (createResult.getResponse().getStatus() == 201) {
      AccountResponse response =
          objectMapper.readValue(createResult.getResponse().getContentAsString(), AccountResponse.class);
      createdAccountId = response.id();
    }
  }

  @Then("the create status should be {int}")
  public void theCreateStatusShouldBe(int statusCode) {
    assertThat(createResult.getResponse().getStatus()).isEqualTo(statusCode);
  }

  @And("a new account id is returned")
  public void aNewAccountIdIsReturned() {
    assertThat(createdAccountId).isNotNull();
  }

  @When("the client fetches the account by returned id")
  public void fetchAccountByReturnedId() throws Exception {
    fetchResult = mockMvc.perform(get("/api/accounts/{id}", createdAccountId)).andReturn();
  }

  @Then("the fetch status should be {int}")
  public void theFetchStatusShouldBe(int statusCode) {
    assertThat(fetchResult.getResponse().getStatus()).isEqualTo(statusCode);
  }

  @And("fetched account number should be {string}")
  public void fetchedAccountNumberShouldBe(String expected) throws Exception {
    AccountResponse response =
        objectMapper.readValue(fetchResult.getResponse().getContentAsString(), AccountResponse.class);
    assertThat(response.accountNumber()).isEqualTo(expected);
  }
}
