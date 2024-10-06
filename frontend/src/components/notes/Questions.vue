<template>
  <div>
    <PopButton btn-class="btn btn-primary" title="Add Question">
      <template #default="{ closer }">
        <NotePredefinedQuestion
          v-bind="{ note }"
          @close-dialog="
            closer();
            questionAdded($event);
          "
        />
      </template>
    </PopButton>
    <table class="question-table mt-2" v-if="questions.length">
      <thead>
        <tr>
          <th>Delete</th>
          <th>Edit</th>
          <th>Approved</th>
          <th>Question Text</th>
          <th>A</th>
          <th>B</th>
          <th>C</th>
          <th>D</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(question, outerIndex) in questions"
          :key="question.bareQuestion.multipleChoicesQuestion.stem"
        >
          <td>
            <PopButton btn-class="btn btn-primary" title="Delete">
              <template #default="{ closer }">
                Do you want to delete this question?
                <button btn-class="btn" title="Yes" @click="() => {removeQuestion(note, question); closer();}">
                  Yes
                </button>
                <button btn-class="btn" title="No" @click="closer()">
                  No
                </button>
              </template>
            </PopButton>
          </td>
          <td>
          <PopButton btn-class="btn btn-primary" title="Edit">
            <template #default="{ closer }">
              <NotePredefinedQuestion
                v-bind="{ note, question}"
                @close-dialog="
                  closer();
                  questionEdited($event);
                "
              />
            </template>
          </PopButton>
          </td>
          <td>
            <input
              :id="'checkbox-' + outerIndex"
              type="checkbox"
              v-model="question.approved"
              @change="toggleApproval(question.id)"
            />
          </td>
          <td>
            <span @click="openedQuestion=question">
              {{ question.bareQuestion.multipleChoicesQuestion.stem }}
            </span>
          </td>
          <template
            v-if="question.bareQuestion.multipleChoicesQuestion.choices"
          >
            <td
              v-for="(choice, index) in question
                .bareQuestion
                .multipleChoicesQuestion.choices"
              :class="{
                'correct-choice': index === question.correctAnswerIndex,
              }"
              :key="index"
            >
              {{ choice }}
            </td>
          </template>
        </tr>
      </tbody>
    </table>
    <div v-else class="no-questions">
      <b >No questions</b>
    </div>
  </div>
  <Modal
    v-if="openedQuestion !== undefined"
    @close_request="openedQuestion = undefined"
  >
    <template #body>
      <QuestionManagement
        :predefinedQuestion="openedQuestion"
      />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { onMounted, ref } from "vue"
import type { Note, PredefinedQuestion } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import NotePredefinedQuestion from "./NotePredefinedQuestion.vue"
import QuestionManagement from "./QuestionManagement.vue"
import PopButton from "../commons/Popups/PopButton.vue"

const { managedApi } = useLoadingApi()
const props = defineProps({
  note: {
    type: Object as PropType<Note>,
    required: true,
  },
})
const questions = ref<PredefinedQuestion[]>([])
const openedQuestion = ref<PredefinedQuestion | undefined>()

const fetchQuestions = async () => {
  questions.value =
    await managedApi.restPredefinedQuestionController.getAllQuestionByNote(
      props.note.id
    )
}
const questionAdded = (newQuestion: PredefinedQuestion) => {
  if (newQuestion == null) {
    return
  }
  questions.value.push(newQuestion)
}
const toggleApproval = async (questionId?: number) => {
  if (questionId) {
    await managedApi.restPredefinedQuestionController.toggleApproval(questionId)
  }
}
const removeQuestion = async (note: Note, question: PredefinedQuestion) => {
  if (question == null) {
    return
  }
  await managedApi.restPredefinedQuestionController.removeQuestion(
    note.id,
    question.id
  )
  questions.value = questions.value.filter((q) => q.id !== question.id)
}
const questionEdited = (newQuestion: PredefinedQuestion) => {
  if (newQuestion == null) {
    return
  }
  questions.value = questions.value.map((question) =>
    question.id === newQuestion.id ? newQuestion : question
  )
}
onMounted(() => {
  fetchQuestions()
})
</script>

<style scoped>
.question-table {
  border-collapse: collapse;
  width: 100%;
}

.question-table th,
.question-table td {
  border: 1px solid #dddddd;
  text-align: left;
  padding: 8px;
}

.question-table th {
  background-color: #f2f2f2;
}

.correct-choice {
  background-color: #4caf50;
}
.no-questions {
  margin-top: 10px;
  width: 100%;
  text-align: center;
}
</style>
