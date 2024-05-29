package com.odde.doughnut.entities;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.algorithms.ClozedString;
import com.odde.doughnut.algorithms.HtmlOrMarkdown;
import com.odde.doughnut.algorithms.NoteTitle;
import com.odde.doughnut.algorithms.SiblingOrder;
import com.odde.doughnut.controllers.dto.TargetNoteForTopic;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.models.NoteViewer;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "note")
@JsonPropertyOrder({"topic", "topicConstructor", "details", "parentId", "linkType", "updatedAt"})
public abstract class Note extends EntityIdentifiedByIdOnly {
  public static final int MAX_TITLE_LENGTH = 150;

  @OneToOne
  @JoinColumn(name = "creator_id")
  @JsonIgnore
  @Getter
  @Setter
  private User creator;

  @ManyToOne
  @JoinColumn(name = "notebook_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private Notebook notebook;

  @OneToOne(mappedBy = "note", cascade = CascadeType.ALL)
  @JsonIgnore
  @Getter
  private NoteAccessory noteAccessory;

  @Column(name = "description")
  @Getter
  @Setter
  @JsonPropertyDescription("The details of the note is in markdown format.")
  private String details;

  @Size(min = 1, max = Note.MAX_TITLE_LENGTH)
  @Getter
  @Setter
  @Column(name = "topic_constructor")
  @NotNull
  private String topicConstructor = "";

  @Column(name = "created_at")
  @Setter
  @Getter
  @NotNull
  private Timestamp createdAt;

  @Setter
  @Column(name = "deleted_at")
  @Getter
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Timestamp deletedAt;

  @OneToMany(mappedBy = "targetNote")
  private List<LinkingNote> refers = new ArrayList<>();

  @OneToMany(mappedBy = "parent", cascade = CascadeType.DETACH)
  @OrderBy("siblingOrder")
  private final List<LinkingNote> links = new ArrayList<>();

  @OneToMany(mappedBy = "parent", cascade = CascadeType.DETACH)
  @JsonIgnore
  @OrderBy("siblingOrder")
  private final List<HierarchicalNote> hierarchicalChildren = new ArrayList<>();

  @OneToMany(mappedBy = "parent", cascade = CascadeType.DETACH)
  @JsonIgnore
  @OrderBy("siblingOrder")
  private final List<Note> children = new ArrayList<>();

  @OneToMany(mappedBy = "note")
  @JsonIgnore
  private Set<ReviewPoint> reviewPoints;

  @Column(name = "updated_at")
  @Getter
  @Setter
  @NotNull
  private Timestamp updatedAt;

  @Column(name = "wikidata_id")
  @Getter
  @Setter
  private String wikidataId;

  @Column(name = "sibling_order")
  @JsonIgnore
  @Getter
  private Long siblingOrder = SiblingOrder.getGoodEnoughOrderNumber();

  @ManyToOne
  @JoinColumn(name = "target_note_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private Note targetNote;

  @OneToOne
  @JoinColumn(name = "parent_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  private Note parent;

  @Embedded @JsonIgnore @Getter private ReviewSetting reviewSetting = new ReviewSetting();

  @JsonIgnore
  public List<HierarchicalNote> getHierarchicalChildren() {
    return filterDeleted(hierarchicalChildren);
  }

  @JsonIgnore
  public List<Note> getChildren() {
    return filterDeleted(children);
  }

  @JsonIgnore
  public List<LinkingNote> getLinks() {
    return filterDeleted(links);
  }

  @JsonIgnore
  public List<LinkingNote> getRefers() {
    return filterDeleted(refers);
  }

  private static <T extends Note> List<T> filterDeleted(List<T> notes) {
    return notes.stream().filter(n -> n.getDeletedAt() == null).toList();
  }

  @JsonIgnore
  public boolean targetVisibleAsSourceOrTo(User viewer) {
    if (getParent().getNotebook() == getTargetNote().getNotebook()) return true;
    if (viewer == null) return false;
    return viewer.canReferTo(getTargetNote().getNotebook());
  }

  @JsonIgnore
  public NoteTitle getNoteTitle() {
    return new NoteTitle(getTopicConstructor());
  }

  @JsonIgnore
  public List<HierarchicalNote> getHierarchicalSiblings() {
    if (getParent() == null) {
      return new ArrayList<>();
    }
    return getParent().getHierarchicalChildren();
  }

  @JsonIgnore
  public List<Note> getSiblings() {
    if (getParent() == null) {
      return new ArrayList<>();
    }
    return getParent().getChildren();
  }

  @JsonIgnore
  public ClozedString getClozeDescription() {
    if (isDetailsBlankHtml()) return new ClozedString(null, "");

    return ClozedString.htmlClozedString(getDetails()).hide(getNoteTitle());
  }

  @JsonIgnore
  public boolean isDetailsBlankHtml() {
    return new HtmlOrMarkdown(getDetails()).isBlank();
  }

  public LinkType getLinkType() {
    if (!getTopicConstructor().startsWith(":")) return null;
    return LinkType.fromLabel(getTopicConstructor().substring(1));
  }

  public void setLinkType(LinkType linkType) {
    setTopicConstructor(":" + linkType.label);
  }

  protected String getLinkConstructor() {
    return getTopicConstructor();
  }

  @JsonIgnore
  public List<Note> getAncestors() {
    List<Note> result = new ArrayList<>();
    Note p = getParent();
    while (p != null) {
      result.add(0, p);
      p = p.getParent();
    }
    return result;
  }

  @NotNull
  public String getTopic() {
    String constructor = getLinkConstructor();
    if (!constructor.contains("%P")) return constructor;
    Note parent = getParent();
    if (parent == null) return constructor;
    String target =
        getTargetNote() == null ? "missing target" : getTargetNote().getTopicConstructor();
    return constructor
        .replace("%P", "[" + parent.getTopicConstructor() + "]")
        .replace("%T", "[" + target + "]");
  }

  @Override
  public String toString() {
    return "Note{" + "id=" + id + ", title='" + getTopicConstructor() + '\'' + '}';
  }

  @JsonIgnore
  public void setParentNote(Note parentNote) {
    if (parentNote == null) return;
    setNotebook(parentNote.getNotebook());
    parent = parentNote;
  }

  public void updateSiblingOrder(Note relativeToNote, boolean asFirstChildOfNote) {
    if (!asFirstChildOfNote) {
      this.siblingOrder =
          relativeToNote
              .nextSibling()
              .map(x -> (relativeToNote.siblingOrder + x.getSiblingOrder()) / 2)
              .orElse(relativeToNote.siblingOrder + SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT);
      return;
    }
    relativeToNote.getChildren().stream()
        .findFirst()
        .ifPresent(
            firstChild ->
                this.siblingOrder =
                    firstChild.getSiblingOrder() - SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT);
  }

  private Optional<Note> nextSibling() {
    return getSiblings().stream().filter(nc -> nc.getSiblingOrder() > siblingOrder).findFirst();
  }

  public Optional<Integer> getParentId() {
    Note parent = getParent();
    if (parent == null) return Optional.empty();
    return Optional.ofNullable(parent.id);
  }

  @JsonIgnore
  public ImageWithMask getImageWithMask() {
    if (this.noteAccessory == null) return null;

    return noteAccessory.getImageWithMask();
  }

  public void prependDescription(String addition) {
    String prevDesc = getDetails() != null ? getDetails() : "";
    String desc = prevDesc.isEmpty() ? addition : addition + "\n" + prevDesc;
    setDetails(desc);
  }

  @JsonIgnore
  public String getContextPathString() {
    return getAncestors().stream()
        .map(Note::getTopicConstructor)
        .collect(Collectors.joining(" › "));
  }

  @JsonIgnore
  public boolean matchAnswer(String spellingAnswer) {
    return getNoteTitle().matches(spellingAnswer);
  }

  @JsonIgnore
  public Stream<Note> getDescendants() {
    return getHierarchicalChildren().stream()
        .flatMap(c -> Stream.concat(Stream.of(c), c.getDescendants()));
  }

  @JsonIgnore
  public Stream<Note> getLinksAndRefers() {
    return Stream.concat(getLinks().stream(), getRefers().stream());
  }

  @JsonIgnore
  public abstract List<QuizQuestionFactory> getQuizQuestionFactories();

  @JsonIgnore
  public NoteAccessory getOrInitializeNoteAccessory() {
    if (noteAccessory == null) {
      noteAccessory = new NoteAccessory();
      noteAccessory.setNote(this);
    }
    return noteAccessory;
  }

  public TargetNoteForTopic getTargetForTopic() {
    if (getTargetNote() == null) return null;
    return getTargetNote().asTargetNoteForTopic();
  }

  @JsonIgnore
  private TargetNoteForTopic asTargetNoteForTopic() {
    TargetNoteForTopic targetNoteForTopic = new TargetNoteForTopic();
    targetNoteForTopic.setId(getId());
    targetNoteForTopic.setTopicConstructor(getTopicConstructor());
    targetNoteForTopic.setTargetNoteForTopic(getTargetForTopic());
    return targetNoteForTopic;
  }

  public static class NoteBrief {
    public String contextPath;
    public String topic;
    public String details;
  }

  @JsonIgnore
  public String getNoteDescription() {
    Note.NoteBrief noteBrief = new Note.NoteBrief();
    noteBrief.contextPath = getContextPathString();
    noteBrief.topic = getTopicConstructor();
    noteBrief.details = getDetails();
    return """
The note of current focus (in JSON format):
%s
"""
        .formatted(defaultObjectMapper().valueToTree(noteBrief).toPrettyString());
  }

  protected void initialize(
      User user, Note parentNote, Timestamp currentUTCTimestamp, String topicConstructor) {
    setParentNote(parentNote);
    setUpdatedAt(currentUTCTimestamp);
    setTopicConstructor(topicConstructor);
    setCreatedAt(currentUTCTimestamp);
    setUpdatedAt(currentUTCTimestamp);
    setCreator(user);
  }

  public void buildNotebookForHeadNote(Ownership ownership, User creator) {
    final Notebook notebook = new Notebook();
    notebook.setCreatorEntity(creator);
    notebook.setOwnership(ownership);
    notebook.setHeadNote(this);
    setNotebook(notebook);
  }

  public NoteViewer targetNoteViewer(User user) {
    return new NoteViewer(user, getTargetNote());
  }

  public Thing buildNoteThing() {
    Thing result = new Thing();
    result.setNote(this);
    return result;
  }
}
