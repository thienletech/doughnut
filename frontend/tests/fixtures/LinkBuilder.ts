import { Note, Thing } from "@/generated/backend";
import Builder from "./Builder";
import NoteBuilder from "./NoteBuilder";

class LinkBuilder extends Builder<Thing> {
  sourceNoteBuilder = new NoteBuilder();

  targetNoteBuilder = new NoteBuilder();

  internalType = Note.linkType.RELATED_TO;

  from(note: Note): LinkBuilder {
    this.sourceNoteBuilder.data = note;
    return this;
  }

  to(note: Note): LinkBuilder {
    this.targetNoteBuilder.data = note;
    return this;
  }

  type(t: Note.linkType): LinkBuilder {
    this.internalType = t;
    return this;
  }

  do(): Thing {
    return {
      note: new NoteBuilder()
        .linkType(this.internalType)
        .target(this.targetNoteBuilder.do())
        .do(),
      sourceNote: this.sourceNoteBuilder.do(),
    };
  }
}

export default LinkBuilder;
