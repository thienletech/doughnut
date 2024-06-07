Feature: Bazaar generate
  As a trainer, I want to generate to assessment questions in the Bazaar so that I can
  print the questions for notebook.

  Background:
    Given there are some notes for existing user "old_learner"
      | topicConstructor | testingParent | skipReview |
      | LeSS in Action   |               | true       |

  Scenario: open pop up for log in if the user is not logged in and generate assessment
    Given I haven't login
    And notebook "LeSS in Action" is shared to the Bazaar
    When I go to the bazaar
    And I generate assessment questions on notebook "LeSS in Action"
    Then I should see message that says "Please login first"

  @ignore
  @usingMockedOpenAiService
  Scenario Outline: display assessment questions from notebook
    Given I am logged in as an existing user
    And There are <notes count> notes belonging to "LeSS in Action"
    And notebook "LeSS in Action" is shared to the Bazaar
    When I go to the bazaar
    And I generate assessment questions on notebook "LeSS in Action"
    Then I should see message that says "<message>"
    And I should see <question count> questions

    Examples:
      | notes count | message                                  | question count |
      | 5           | Assessment For LeSS in Action            | 5              |
      | 6           | Assessment For LeSS in Action            | 5              |
      | 4           | Insufficient notes to create assessment! | 0              |

  @ignore
  Scenario: generate questions from notebook
    Given I am logged in as an existing user
    When I click on generate assessment questions button
    Then generate "5" questions


