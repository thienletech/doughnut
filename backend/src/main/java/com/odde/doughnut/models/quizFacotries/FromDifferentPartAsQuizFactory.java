package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.UserModel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FromDifferentPartAsQuizFactory extends AbstractCategoryQuizFactory
    implements QuizQuestionFactory, QuestionOptionsFactory {
  private List<Note> cachedFillingOptions = null;
  private final User user;
  private final Link link;
  private final QuizQuestionServant servant;

  public FromDifferentPartAsQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    super(servant, reviewPoint.getUser(), reviewPoint.getLink());
    user = reviewPoint.getUser();
    link = reviewPoint.getLink();
    this.servant = servant;
  }

  @Override
  public List<Note> allWrongAnswers() {
    List<Note> result = new ArrayList<>(link.getCousinsOfSameLinkType(user));
    result.add(link.getSourceNote());
    return result;
  }

  @Override
  public List<Note> generateFillingOptions() {
    if (cachedFillingOptions == null) {
      Stream<Link> cousinLinks = getCousinLinksFromSameCategoriesOfSameLinkType();
      cachedFillingOptions =
          servant
              .chooseFillingOptionsRandomly(cousinLinks)
              .map(Link::getSourceNote)
              .collect(Collectors.toList());
    }
    return cachedFillingOptions;
  }

  @Override
  public Link getCategoryLink() {
    return getCategoryLink1();
  }

  @Override
  public Note generateAnswerNote() {
    return servant
        .randomizer
        .chooseOneRandomly(getReverseLinksOfCousins(user))
        .map(Link::getSourceNote)
        .orElse(null);
  }

  @Override
  public List<ReviewPoint> getViceReviewPoints(UserModel userModel) {
    return getCategoryReviewPoints(userModel);
  }
}
