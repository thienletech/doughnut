package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionFromDifferentPartAs;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;
import java.util.stream.Collectors;

public class FromDifferentPartAsQuizFactory
    implements QuizQuestionFactory, QuestionOptionsFactory, SecondaryReviewPointsFactory {

  private final LinkingNote parentGrandLink;
  private final LinkingNote link;
  private final QuizQuestionServant servant;

  public FromDifferentPartAsQuizFactory(LinkingNote note, QuizQuestionServant servant) {
    link = note;
    this.servant = servant;
    parentGrandLink = servant.getParentGrandLink(link);
  }

  @Override
  public int minimumOptionCount() {
    return 3;
  }

  @Override
  public List<Note> generateFillingOptions() {
    if (getCategoryLink() == null) {
      return null;
    }
    List<Note> cousinLinks =
        servant.getSiblingLinksOfSameLinkTypeHavingReviewPoint(link).collect(Collectors.toList());
    return servant.chooseFillingOptionsRandomly(cousinLinks).stream()
        .map(Note::getParent)
        .collect(Collectors.toList());
  }

  @Override
  public LinkingNote getCategoryLink() {
    return parentGrandLink;
  }

  @Override
  public Note generateAnswer() {
    return servant
        .randomizer
        .chooseOneRandomly(servant.getCousinLinksAvoidingSiblings(link, parentGrandLink))
        .map(Note::getParent)
        .orElse(null);
  }

  @Override
  public QuizQuestionEntity buildQuizQuestion() {
    return new QuizQuestionFromDifferentPartAs();
  }
}
