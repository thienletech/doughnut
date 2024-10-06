package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.QuestionSuggestionCreationParams;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.PredefinedQuestionService;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.client.OpenAiApi;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/review-questions")
class RestPredefinedQuestionController {
  private final ModelFactoryService modelFactoryService;
  private final PredefinedQuestionService predefinedQuestionService;

  private final UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestPredefinedQuestionController(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
    this.predefinedQuestionService =
        new PredefinedQuestionService(
            openAiApi, modelFactoryService, testabilitySettings.getRandomizer());
  }

  @PostMapping("/generate-question-without-save")
  public PredefinedQuestion generateQuestionWithoutSave(
      @RequestParam(value = "note") @Schema(type = "integer") Note note) {
    currentUser.assertLoggedIn();
    return predefinedQuestionService.generateAQuestionOfRandomTypeWithoutSaving(
        note, currentUser.getEntity());
  }

  @PostMapping("/{predefinedQuestion}/suggest-fine-tuning")
  @Transactional
  public SuggestedQuestionForFineTuning suggestQuestionForFineTuning(
      @PathVariable("predefinedQuestion") @Schema(type = "integer")
          PredefinedQuestion predefinedQuestion,
      @Valid @RequestBody QuestionSuggestionCreationParams suggestion) {
    SuggestedQuestionForFineTuning sqft = new SuggestedQuestionForFineTuning();
    var suggestedQuestionForFineTuningService =
        modelFactoryService.toSuggestedQuestionForFineTuningService(sqft);
    return suggestedQuestionForFineTuningService.suggestQuestionForFineTuning(
        predefinedQuestion,
        suggestion,
        currentUser.getEntity(),
        testabilitySettings.getCurrentUTCTimestamp());
  }

  @GetMapping("/{note}/note-questions")
  public List<PredefinedQuestion> getAllQuestionByNote(
      @PathVariable("note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    return note.getPredefinedQuestions().stream().toList();
  }

  @PostMapping("/{note}/note-questions")
  @Transactional
  public PredefinedQuestion addQuestionManually(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @Valid @RequestBody PredefinedQuestion predefinedQuestion)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    return predefinedQuestionService.addQuestion(note, predefinedQuestion);
  }

  @PatchMapping("/{note}/note-questions/{predefinedQuestion}")
  @Transactional
  public PredefinedQuestion updateQuestion(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @PathVariable("predefinedQuestion") @Schema(type = "integer")
          PredefinedQuestion predefinedQuestion,
      @Valid @RequestBody PredefinedQuestion predefinedQuestionUpdateRequest)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    predefinedQuestion.setBareQuestion(predefinedQuestionUpdateRequest.getBareQuestion());
    predefinedQuestion.setCorrectAnswerIndex(
        predefinedQuestionUpdateRequest.getCorrectAnswerIndex());
    predefinedQuestion.setApproved(predefinedQuestionUpdateRequest.isApproved());
    return predefinedQuestionService.updateQuestion(predefinedQuestion);
  }

  @DeleteMapping("/{note}/note-questions/{predefinedQuestion}")
  @Transactional
  public void removeQuestion(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @PathVariable("predefinedQuestion") @Schema(type = "integer")
          PredefinedQuestion predefinedQuestion)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    predefinedQuestionService.removeQuestion(predefinedQuestion);
  }

  @PostMapping("/{note}/refine-question")
  @Transactional
  public PredefinedQuestion refineQuestion(
      @PathVariable("note") @Schema(type = "integer") Note note,
      @RequestBody PredefinedQuestion predefinedQuestion)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    return predefinedQuestionService.refineAIQuestion(note, predefinedQuestion);
  }

  @PostMapping("/{reviewQuestionInstance}/toggle-approval")
  @Transactional
  public PredefinedQuestion toggleApproval(
      @PathVariable("reviewQuestionInstance") @Schema(type = "integer")
          PredefinedQuestion predefinedQuestion)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(predefinedQuestion.getNote());
    return predefinedQuestionService.toggleApproval(predefinedQuestion);
  }
}
