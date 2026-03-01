Feature: Account API
  As a banking platform user
  I want to create and fetch an account
  So that account data is stored and retrievable

  Scenario: Create account and fetch it by ID
    Given a customer id "d290f1ee-6c54-4b01-90e6-d701748f0851"
    And account number "ACCBDD001"
    And currency "USD"
    And opening balance 1000.00
    When the client creates the account
    Then the create status should be 201
    And a new account id is returned
    When the client fetches the account by returned id
    Then the fetch status should be 200
    And fetched account number should be "ACCBDD001"
