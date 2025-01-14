package com.odde.doughnut.testability;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionServant;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.services.LinkQuestionType;
import com.odde.doughnut.testability.builders.*;
import java.sql.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MakeMe extends MakeMeWithoutDB {
  @Autowired public ModelFactoryService modelFactoryService;

  private MakeMe() {}

  public static MakeMe makeMeWithoutFactoryService() {
    return new MakeMe();
  }

  public UserBuilder aUser() {
    return new UserBuilder(this);
  }

  public UserBuilder anAdmin() {
    return new UserBuilder(this, "admin");
  }

  public NoteBuilder aNote() {
    return new NoteBuilder(this);
  }

  public NoteBuilder aNote(String title) {
    return aNote().titleConstructor(title);
  }

  public NoteBuilder aHeadNote(String title) {
    return aNote().titleConstructor(title);
  }

  public NotebookBuilder aNotebook() {
    return new NotebookBuilder(null, this);
  }

  public NotebookBuilder theNotebook(Notebook notebook) {
    return new NotebookBuilder(notebook, this);
  }

  public NoteBuilder aNote(String title, String details) {
    return aNote().titleConstructor(title).details(details);
  }

  public NoteBuilder theNote(Note note) {
    return new NoteBuilder(note, this);
  }

  public BazaarNotebookBuilder aBazaarNotebook(Notebook notebook) {
    return new BazaarNotebookBuilder(this, notebook);
  }

  public CertificateBuilder aCertificate(Notebook notebook, UserModel user, Timestamp startDate) {

    return new CertificateBuilder(notebook, user, startDate, this);
  }

  public AssessmentAttemptBuilder anAssessmentAttempt(User currentUser) {
    AssessmentAttempt assessmentAttempt = new AssessmentAttempt();

    assessmentAttempt.setUser(currentUser);
    assessmentAttempt.setSubmittedAt(aTimestamp().please());
    assessmentAttempt.setTotalQuestionCount(2);
    assessmentAttempt.setAnswersCorrect(2);
    return new AssessmentAttemptBuilder(this, assessmentAttempt);
  }

  public <T> T refresh(T object) {
    modelFactoryService.entityManager.flush();
    modelFactoryService.entityManager.refresh(object);
    return object;
  }

  public ReviewPointBuilder aReviewPointFor(Note note) {
    ReviewPoint reviewPoint = ReviewPoint.buildReviewPointForNote(note);
    ReviewPointBuilder reviewPointBuilder = new ReviewPointBuilder(reviewPoint, this);
    reviewPointBuilder.entity.setNote(note);
    return reviewPointBuilder;
  }

  public ReviewPointBuilder aReviewPointBy(UserModel user) {
    Note note = aNote().please();
    return aReviewPointFor(note).by(user);
  }

  public CircleBuilder aCircle() {
    return new CircleBuilder(null, this);
  }

  public CircleBuilder theCircle(CircleModel circleModel) {
    return new CircleBuilder(circleModel, this);
  }

  public ImageBuilder anImage() {
    return new ImageBuilder(new Image(), this);
  }

  public SubscriptionBuilder aSubscription() {
    return new SubscriptionBuilder(this, new Subscription());
  }

  public LinkBuilder aLink() {
    return new LinkBuilder(this);
  }

  public UserModel aNullUserModelPlease() {
    return modelFactoryService.toUserModel(null);
  }

  public PredefinedQuestionBuilder aPredefinedQuestion() {
    return new PredefinedQuestionBuilder(this);
  }

  public ReviewQuestionInstanceBuilder aReviewQuestionInstance() {
    return new ReviewQuestionInstanceBuilder(this, null);
  }

  public ReviewQuestionInstanceBuilder theReviewQuestionInstance(
      ReviewQuestionInstance reviewQuestionInstance) {
    return new ReviewQuestionInstanceBuilder(this, reviewQuestionInstance);
  }

  public PredefinedQuestion buildAQuestionForLinkingNote(
      LinkQuestionType linkQuestionType, Note linkingNote, User user) {
    PredefinedQuestionServant servant =
        new PredefinedQuestionServant(user, new NonRandomizer(), modelFactoryService);
    try {
      return linkQuestionType
          .factoryForLinkingNote
          .apply(linkingNote, servant)
          .buildValidPredefinedQuestion();
    } catch (PredefinedQuestionNotPossibleException e) {
      return null;
    }
  }

  public FailureReportBuilder aFailureReport() {
    return new FailureReportBuilder(this);
  }

  public SuggestedQuestionForFineTuningBuilder aQuestionSuggestionForFineTunining() {
    return new SuggestedQuestionForFineTuningBuilder(this);
  }

  public AudioBuilder anAudio() {
    return new AudioBuilder(new Audio(), this);
  }

  public UserAssistantThreadBuilder aUserAssistantThread(String threadId) {
    return new UserAssistantThreadBuilder(this, threadId);
  }

  public ConversationBuilder aConversation() {
    return new ConversationBuilder(this);
  }
}
