package com.odde.doughnut.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Conversation extends EntityIdentifiedByIdOnly {

  @ManyToOne
  @JoinColumn(name = "quiz_question_and_answer_id", referencedColumnName = "id")
  QuizQuestionAndAnswer quizQuestionAndAnswer;

  @ManyToOne
  @JoinColumn(name = "note_creator_id", referencedColumnName = "id")
  User noteCreator;

  @ManyToOne
  @JoinColumn(name = "conversation_initiator_id", referencedColumnName = "id")
  User conversationInitiator;
}