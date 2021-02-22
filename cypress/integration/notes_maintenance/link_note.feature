Feature: link note

    Background:
        Given there are some notes for the current user

    @clean_db @login_as_existing_user1
    Scenario: View all linkable notes when no links exist
        When I go to the notes page
        Then I should see 3 notes belonging to the user
            | note-title       |
            | Sedition        |
            | Sedation        |
            | Sedative        |
        When I click Create Link button on Sedition
        Then I should be navigated to the linking page
        And I should see the source note as Sedition
        And I should see below notes
        | note-title      |   note-description  |
        | Sedation        |   Put to sleep      |
        | Sedative        |   sleep medicine    |

    @clean_db @login_as_existing_user1
    Scenario: Search note for linking
        When I go to the notes page
        And I click Create Link button on Sedition
        Then I should be navigated to the linking page
        And  I should see below notes
            | note-title      |   note-description  |
            | Sedation        |   Put to sleep      |
            | Sedative        |   sleep medicine    |
        When I search for notes with title "Sedatio"
        Then I should see only "Sedation"

    @clean_db @login_as_existing_user1
    Scenario: Create link for note
        When I go to the notes page
        And I click Create Link button on Sedition
        Then I should be navigated to the linking page
        And I should be able to see the buttons for linking note
        When I select a Sedation note
        Then I should be redirected to review page
        And I should see the Sedition note linked to Sedation