package com.odde.doughnut.models;

import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.repositories.UserRepository;
import com.odde.doughnut.testability.DBCleaner;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
@Transactional

public class NoteTest {
    @Autowired private NoteRepository noteRepository;
    @Autowired EntityManager entityManager;


    MakeMe makeMe;

    @BeforeEach
    void setup() {
        makeMe = new MakeMe();
    }

    @Nested
    class GetAncestors {
        @BeforeEach
        void setup() {
        }
        @Test
        void topLevelNoteHaveEmptyAncestors() {
            Note topLevel = makeMe.aNote().please(noteRepository);
            List<Note> ancestors = topLevel.getAncestors();
            assertThat(ancestors, is(empty()));
        }
    }

}
