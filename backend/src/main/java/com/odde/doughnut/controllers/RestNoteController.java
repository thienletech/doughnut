package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.json.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.DuplicateWikidataIdException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.SearchTermModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.NoteConstructionService;
import com.odde.doughnut.services.WikidataService;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import com.odde.doughnut.services.wikidataApis.WikidataIdWithApi;
import com.odde.doughnut.testability.TestabilitySettings;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/notes")
class RestNoteController {
  private final ModelFactoryService modelFactoryService;
  private UserModel currentUser;
  private final WikidataService wikidataService;
  private final TestabilitySettings testabilitySettings;

  public RestNoteController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      HttpClientAdapter httpClientAdapter,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
    this.wikidataService =
        new WikidataService(httpClientAdapter, testabilitySettings.getWikidataServiceUrl());
  }

  @PostMapping(value = "/{note}/updateWikidataId")
  @Transactional
  public NoteRealm updateWikidataId(
      @PathVariable(name = "note") Note note,
      @RequestBody WikidataAssociationCreation wikidataAssociationCreation)
      throws BindException, UnexpectedNoAccessRightException, IOException, InterruptedException {
    currentUser.assertAuthorization(note);
    WikidataIdWithApi wikidataIdWithApi =
        wikidataService.wrapWikidataIdWithApi(wikidataAssociationCreation.wikidataId);
    try {
      wikidataIdWithApi.associateNoteToWikidata(note, modelFactoryService);
    } catch (DuplicateWikidataIdException e) {
      BindingResult bindingResult =
          new BeanPropertyBindingResult(wikidataAssociationCreation, "wikidataAssociationCreation");
      bindingResult.rejectValue("wikidataId", "duplicate", "Duplicate Wikidata ID Detected.");
      throw new BindException(bindingResult);
    }
    modelFactoryService.save(note);
    return new NoteViewer(currentUser.getEntity(), note).toJsonObject();
  }

  @PostMapping(value = "/{parentNote}/create")
  @Transactional
  public NoteRealm createNote(
      @PathVariable(name = "parentNote") Note parentNote,
      @Valid @ModelAttribute NoteCreationDTO noteCreation)
      throws UnexpectedNoAccessRightException, InterruptedException, IOException, BindException {
    currentUser.assertAuthorization(parentNote);
    User user = currentUser.getEntity();

    try {
      Note note =
          getNoteConstructionService(user)
              .createNoteWithWikidataInfo(
                  parentNote,
                  wikidataService.wrapWikidataIdWithApi(noteCreation.wikidataId),
                  noteCreation.getLinkTypeToParent(),
                  noteCreation.getTopicConstructor());
      return new NoteViewer(user, note).toJsonObject();
    } catch (DuplicateWikidataIdException e) {
      BindingResult bindingResult = new BeanPropertyBindingResult(noteCreation, "noteCreation");
      bindingResult.rejectValue("wikidataId", "duplicate", "Duplicate Wikidata ID Detected.");
      throw new BindException(bindingResult);
    }
  }

  private NoteConstructionService getNoteConstructionService(User user) {
    return new NoteConstructionService(
        user, testabilitySettings.getCurrentUTCTimestamp(), modelFactoryService);
  }

  @GetMapping("/{note}")
  public NoteRealm show(@PathVariable("note") Note note) throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(note);
    User user = currentUser.getEntity();
    return new NoteViewer(user, note).toJsonObject();
  }

  @PatchMapping(path = "/{note}")
  @Transactional
  public NoteRealm updateNote(
      @PathVariable(name = "note") Note note,
      @Valid @ModelAttribute NoteAccessories noteAccessories)
      throws UnexpectedNoAccessRightException, IOException {
    currentUser.assertAuthorization(note);

    final User user = currentUser.getEntity();
    noteAccessories.fetchUploadedPicture(user);
    note.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
    note.updateNoteContent(noteAccessories);
    modelFactoryService.save(note);
    return new NoteViewer(user, note).toJsonObject();
  }

  @GetMapping("/{note}/note-info")
  public NoteInfo getNoteInfo(@PathVariable("note") Note note)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(note);
    NoteInfo noteInfo = new NoteInfo();
    noteInfo.setReviewPoint(currentUser.getReviewPointFor(note));
    noteInfo.setNote(new NoteViewer(currentUser.getEntity(), note).toJsonObject());
    noteInfo.setCreatedAt(note.getThing().getCreatedAt());
    noteInfo.setReviewSetting(note.getReviewSetting());
    return noteInfo;
  }

  @PostMapping("/search")
  @Transactional
  public List<Note> searchForLinkTarget(@Valid @RequestBody SearchTerm searchTerm) {
    SearchTermModel searchTermModel =
        modelFactoryService.toSearchTermModel(currentUser.getEntity(), searchTerm);
    return searchTermModel.searchForNotes();
  }

  @PostMapping(value = "/{note}/delete")
  @Transactional
  public List<NoteRealm> deleteNote(@PathVariable("note") Note note)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    modelFactoryService.toNoteModel(note).destroy(testabilitySettings.getCurrentUTCTimestamp());
    modelFactoryService.entityManager.flush();
    Note parentNote = note.getParent();
    if (parentNote != null) {
      return List.of(new NoteViewer(currentUser.getEntity(), parentNote).toJsonObject());
    }
    return List.of();
  }

  @PatchMapping(value = "/{note}/undo-delete")
  @Transactional
  public NoteRealm undoDeleteNote(@PathVariable("note") Note note)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    modelFactoryService.toNoteModel(note).restore();
    modelFactoryService.entityManager.flush();

    return new NoteViewer(currentUser.getEntity(), note).toJsonObject();
  }

  @GetMapping("/{note}/position")
  public NotePositionViewedByUser getPosition(Note note) throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    return new NoteViewer(currentUser.getEntity(), note).jsonNotePosition();
  }

  @PostMapping(value = "/{note}/review-setting")
  @Transactional
  public RedirectToNoteResponse updateReviewSetting(
      @PathVariable("note") Note note, @Valid @RequestBody ReviewSetting reviewSetting)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    note.mergeMasterReviewSetting(reviewSetting);
    modelFactoryService.save(note);
    return new RedirectToNoteResponse(note.getId());
  }
}
