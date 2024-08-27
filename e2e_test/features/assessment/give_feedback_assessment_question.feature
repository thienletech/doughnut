@ignore
Feature: Learner gives feedback on an assessment question
    As a learner, I want to give feedback on an assessment question

    Background:
        Given I am logged in as "old_learner"
        And there is a certified notebook "Just say 'Yes'" by "a_trainer" with 2 questions, shared to the Bazaar

    Scenario: Starts an assessment and answers wrongly, then gives feedback on the question
        When I start an assessment on the note "Just say 'Yes'"
        And I answer the question "Is 0 * 0 = 0?" wrongly
        Then I see an option to give feedback on the question

    Scenario: I have the option to give feedback
        When I start an assessment on the note "Just say 'Yes'"
        And I answer the question "Is 0 * 0 = 0?" wrongly
        And I submit my feedback
        Then my feedback is saved and I see a confirmation message
