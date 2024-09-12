package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "review_question_instance")
@Data
@EqualsAndHashCode(callSuper = false)
@JsonPropertyOrder({"id", "quiz_question1", "notebook"})
public class ReviewQuestionInstance extends EntityIdentifiedByIdOnly {
  @OneToOne
  @JoinColumn(name = "predefined_question_id", referencedColumnName = "id")
  @NotNull
  @JsonIgnore
  private PredefinedQuestion predefinedQuestion;

  @NotNull
  public QuizQuestion1 getQuizQuestion1() {
    return predefinedQuestion.getQuizQuestion1();
  }

  public Notebook getNotebook() {
    return predefinedQuestion.getNote().getNotebook();
  }
}
