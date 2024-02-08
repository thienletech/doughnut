package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.factoryServices.quizFacotries.*;
import com.odde.doughnut.factoryServices.quizFacotries.factories.*;
import com.odde.doughnut.factoryServices.quizFacotries.presenters.*;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;

//
// The class name has Entity suffix so that it can be distinguished from the QuizQuestion class,
// which is used in the frontend from the data consumer's perspective.
//
@Entity
@Table(name = "quiz_question")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "question_type", discriminatorType = DiscriminatorType.INTEGER)
public class QuizQuestionEntity extends EntityIdentifiedByIdOnly {

  public enum QuestionType {
    CLOZE_SELECTION(
        1, ClozeTitleSelectionQuizFactory::new, null, ClozeTitleSelectionQuizPresenter::new),
    SPELLING(2, SpellingQuizFactory::new, null, SpellingQuizPresenter::new),
    PICTURE_TITLE(
        3, PictureTitleSelectionQuizFactory::new, null, PictureTitleSelectionQuizPresenter::new),
    PICTURE_SELECTION(
        4, PictureSelectionQuizFactory::new, null, PictureSelectionQuizPresenter::new),
    LINK_TARGET(5, null, LinkTargetQuizFactory::new, LinkTargetQuizPresenter::new),
    LINK_SOURCE(6, null, LinkSourceQuizFactory::new, LinkSourceQuizPresenter::new),
    LINK_SOURCE_WITHIN_SAME_LINK_TYPE(
        14,
        null,
        LinkSourceWithinSameLinkTypeQuizFactory::new,
        LinkSourceWithinSameLinkTypeQuizPresenter::new),
    CLOZE_LINK_TARGET(7, null, ClozeLinkTargetQuizFactory::new, ClozeLinkTargetQuizPresenter::new),
    DESCRIPTION_LINK_TARGET(
        8, null, DescriptionLinkTargetQuizFactory::new, DescriptionLinkTargetQuizPresenter::new),
    WHICH_SPEC_HAS_INSTANCE(
        9, null, WhichSpecHasInstanceQuizFactory::new, WhichSpecHasInstanceQuizPresenter::new),
    FROM_SAME_PART_AS(10, null, FromSamePartAsQuizFactory::new, FromSamePartAsQuizPresenter::new),
    FROM_DIFFERENT_PART_AS(
        11, null, FromDifferentPartAsQuizFactory::new, FromDifferentPartAsQuizPresenter::new),
    AI_QUESTION(12, AiQuestionFactory::new, null, AiQuestionPresenter::new);

    public final Integer id;
    public final BiFunction<Note, QuizQuestionServant, QuizQuestionFactory> factory;
    public final BiFunction<LinkingNote, QuizQuestionServant, QuizQuestionFactory>
        factoryForLinkingNote;
    public final Function<QuizQuestionEntity, QuizQuestionPresenter> presenter;

    QuestionType(
        Integer id,
        BiFunction<Note, QuizQuestionServant, QuizQuestionFactory> factory,
        BiFunction<LinkingNote, QuizQuestionServant, QuizQuestionFactory> factoryForLinkingNote,
        Function<QuizQuestionEntity, QuizQuestionPresenter> presenter) {
      this.id = id;
      this.factory = factory;
      this.factoryForLinkingNote = factoryForLinkingNote;
      this.presenter = presenter;
    }

    private static final Map<Integer, QuestionType> idMap =
        Collections.unmodifiableMap(
            Arrays.stream(values()).collect(Collectors.toMap(x -> x.id, x -> x)));

    public static QuestionType fromId(Integer id) {
      return idMap.getOrDefault(id, null);
    }

    public QuizQuestionFactory getQuizQuestionFactory(Note note, QuizQuestionServant servant) {
      if (note instanceof LinkingNote ln) return factoryForLinkingNote.apply(ln, servant);
      return factory.apply(note, servant);
    }
  }

  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Note note;

  @Column(name = "question_type", insertable = false, updatable = false)
  @Getter
  @Setter
  private Integer questionTypeId;

  @Column(name = "raw_json_question")
  @Getter
  @Setter
  private String rawJsonQuestion;

  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "category_link_id")
  @Getter
  @Setter
  private LinkingNote categoryLink;

  @Column(name = "option_thing_ids")
  @Getter
  @Setter
  private String optionNoteIds = "";

  @Column(name = "correct_answer_index")
  @Getter
  @Setter
  private Integer correctAnswerIndex;

  @Column(name = "created_at")
  @Getter
  @Setter
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  public void setQuestionType(QuestionType questionType) {
    this.questionTypeId = questionType.id;
  }

  @JsonIgnore
  public QuestionType getQuestionType() {
    return QuestionType.fromId(questionTypeId);
  }

  @JsonIgnore
  public QuizQuestionPresenter buildPresenter() {
    return getQuestionType().presenter.apply(this);
  }

  public void setChoicesAndRightAnswer(
      Note answerNote, List<? extends Note> options, Randomizer randomizer) {
    List<Note> optionsEntities = new ArrayList<>(options);
    optionsEntities.add(answerNote);
    List<Note> shuffled = randomizer.shuffle(optionsEntities);
    setCorrectAnswerIndex(shuffled.indexOf(answerNote));
    setOptionNoteIds(
        shuffled.stream().map(Note::getId).map(Object::toString).collect(Collectors.joining(",")));
  }

  @JsonIgnore
  public List<Integer> getChoiceNoteIds() {
    if (Strings.isBlank(optionNoteIds)) return List.of();
    return Arrays.stream(optionNoteIds.split(","))
        .map(Integer::parseInt)
        .collect(Collectors.toList());
  }

  @JsonIgnore
  public ReviewPoint getReviewPointFor(UserModel userModel) {
    return userModel.getReviewPointFor(getNote());
  }

  public MCQWithAnswer getMcqWithAnswer() {
    try {
      return new ObjectMapper().readValue(getRawJsonQuestion(), MCQWithAnswer.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean checkAnswer(Answer answer) {
    return Objects.equals(answer.getChoiceIndex(), getCorrectAnswerIndex());
  }
}
