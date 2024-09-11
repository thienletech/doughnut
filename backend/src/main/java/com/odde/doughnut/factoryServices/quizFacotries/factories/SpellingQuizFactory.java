package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionAndAnswer;
import com.odde.doughnut.entities.ReviewSetting;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;

public class SpellingQuizFactory extends QuizQuestionFactory {

  protected final Note answerNote;

  public SpellingQuizFactory(Note note) {
    this.answerNote = note;
  }

  @Override
  public QuestionAndAnswer buildValidQuizQuestion() throws QuizQuestionNotPossibleException {
    if (!needSpellingQuiz()) {
      throw new QuizQuestionNotPossibleException();
    }
    return buildSpellingQuestion();
  }

  private boolean needSpellingQuiz() {
    if (answerNote.isDetailsBlankHtml()) {
      return false;
    }
    ReviewSetting reviewSetting = answerNote.getReviewSetting();
    return reviewSetting != null && reviewSetting.getRememberSpelling();
  }

  private String getStem() {
    return answerNote.getClozeDescription().clozeDetails();
  }

  public QuestionAndAnswer buildSpellingQuestion() {
    QuestionAndAnswer questionAndAnswerSpelling = new QuestionAndAnswer();
    questionAndAnswerSpelling.setNote(answerNote);
    questionAndAnswerSpelling.setApproved(true);
    questionAndAnswerSpelling.setCheckSpell(true);
    MultipleChoicesQuestion mcq = new MultipleChoicesQuestion();
    mcq.setStem(getStem());
    questionAndAnswerSpelling.setMultipleChoicesQuestion(mcq);
    // for in memory consistency
    questionAndAnswerSpelling.getQuizQuestion().setQuestionAndAnswer(questionAndAnswerSpelling);
    return questionAndAnswerSpelling;
  }
}
