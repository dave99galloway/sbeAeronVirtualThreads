Feature: Aeron Messaging with Different Serialization Formats
  As a system architect
  I want to send messages using Aeron with different serialization formats
  So that I can compare their performance and resource usage

  Scenario: Send and receive a trade message using SBE
    Given an Aeron publisher and subscriber are connected
    When I send a trade message using SBE format
      | tradeId | symbol | price  | quantity | side | counterparty   |
      |    1001 | AAPL   | 150.25 |      100 | B    | COUNTERPARTY_A |
    Then the message should be received successfully
    And the received trade should match the sent trade

  Scenario: Send and receive a trade message using Protobuf
    Given an Aeron publisher and subscriber are connected
    When I send a trade message using Protobuf format
      | tradeId | symbol | price  | quantity | side | counterparty   |
      |    1002 | GOOGL  | 2800.5 |       50 | S    | COUNTERPARTY_B |
    Then the message should be received successfully
    And the received trade should match the sent trade

  Scenario: Send and receive a trade message using JSON
    Given an Aeron publisher and subscriber are connected
    When I send a trade message using JSON format
      | tradeId | symbol | price  | quantity | side | counterparty   |
      |    1003 | MSFT   | 380.75 |      200 | B    | COUNTERPARTY_C |
    Then the message should be received successfully
    And the received trade should match the sent trade

  Scenario Outline: Compare message sizes across serialization formats
    Given an Aeron publisher and subscriber are connected
    When I serialize a trade message using <format> format
      | tradeId | symbol | price  | quantity | side | counterparty   |
      |    2001 | AAPL   | 150.25 |      100 | B    | COUNTERPARTY_A |
    Then the serialized message size should be recorded
    And the <format> format should produce a message of expected size range

    Examples:
      | format   |
      | SBE      |
      | Protobuf |
      | JSON     |

  Scenario: Send multiple messages and verify throughput
    Given an Aeron publisher and subscriber are connected
    When I send 1000 trade messages using SBE format
    Then all messages should be received successfully
    And the throughput should be measured

  Scenario: Use virtual threads for message subscription
    Given an Aeron publisher is connected
    And an Aeron subscriber using virtual threads is connected
    When I send 100 trade messages using SBE format
    Then all messages should be received by the virtual thread subscriber
    And no thread contention should be observed
