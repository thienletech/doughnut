package com.odde.doughnut.services;

import com.odde.doughnut.entities.AssessmentQuestionInstance;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ConversationService {

  private final ModelFactoryService modelFactoryService;

  public Conversation startConversation(
      AssessmentQuestionInstance assessmentQuestionInstance, User initiator, String feedback) {
    Conversation conversation = new Conversation();
    conversation.setAssessmentQuestionInstance(assessmentQuestionInstance);
    conversation.setConversationInitiator(initiator);
    conversation.setMessage(feedback);

    return modelFactoryService.conversationRepository.save(conversation);
  }

  public List<Conversation> conversationRelatedToUser(User user) {
    return modelFactoryService.conversationRepository
        .findByUserInSubjectOwnershipOrConversationInitiator(user);
  }
}
