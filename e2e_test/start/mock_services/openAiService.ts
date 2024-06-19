import ServiceMocker from "../../support/ServiceMocker"
import testability from "../testability"
import createOpenAiChatCompletionMock from "./createOpenAiChatCompletionMock"

const openAiService = () => {
  const serviceMocker = new ServiceMocker("openAi", 5001)
  return {
    mock() {
      testability().mockService(serviceMocker)
    },
    restore() {
      testability().restoreMockedService(serviceMocker)
    },

    restartImposter() {
      return serviceMocker.install()
    },

    chatCompletion() {
      return createOpenAiChatCompletionMock(serviceMocker)
    },

    stubCreateImage() {
      return serviceMocker.stubPoster(`/images/generations`, {
        created: 1589478378,
        data: [
          {
            url: "https://moon",
            b64_json:
              "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
          },
        ],
      })
    },

    stubOpenAiCompletionWithErrorResponse() {
      return serviceMocker.stubGetterWithError500Response(`/*`, {})
    },

    async alwaysResponseAsUnauthorized() {
      await serviceMocker.install()
      await serviceMocker.stubPosterUnauthorized(`/*`, {
        status: "BAD_REQUEST",
        message: "nah nah nah, you need a valid token",
        error: {
          "OpenAi Error": "BAD_REQUEST",
        },
      })
    },

    stubOpenAiUploadResponse(shouldSuccess: boolean) {
      if (shouldSuccess) {
        return serviceMocker.stubPoster(`/files`, {
          id: "file-abc123",
          object: "file",
          bytes: 175,
          created_at: 1613677385,
          filename: "Question-%s.jsonl",
          purpose: "fine-tune",
        })
      } else {
        return serviceMocker.stubPosterWithError500Response("/v1/files", {})
      }
    },

    async stubCreateAssistant(newId: string, _nameOfAssistant: string, modelName: string) {
      return await serviceMocker.mockMatchsAndNotMatches(
        `/assistants`,
        {
          name: _nameOfAssistant,
          model: modelName,
        },
        undefined,
        {
          id: newId,
        },
      )
    },

    thread(threadId: string) {
      return {
        async stubCreateThreadRunAndSubmitOutput() {
          await serviceMocker.stubPoster(`/threads`, {
            id: threadId,
          })
          // for creating a message
          await serviceMocker.stubPoster(`/threads/${threadId}/messages`, {
            id: "msg-abc123",
          })
          await serviceMocker.stubPoster(`/threads/${threadId}/runs`, {
            id: "run-abc123",
            status: "queued",
          })
          return await serviceMocker.stubPoster(
            `/threads/${threadId}/runs/run-abc123/submit_tool_outputs`,
            {
              id: "run-abc123",
              status: "queued",
            },
          )
        },

        async stubRetrieveRunsThatReplyWithMessage(msg: string) {
          const responses = [
            {
              id: "run-abc123",
              status: "completed",
            },
          ]
          await serviceMocker.stubGetterWithMutipleResponses(
            `/threads/${threadId}/runs/run-abc123`,
            {},
            responses,
          )
          return await serviceMocker.stubGetter(
            `/threads/${threadId}/messages`,
            {},
            {
              object: "list",
              data: [
                {
                  object: "thread.message",
                  content: [
                    {
                      type: "text",
                      text: {
                        value: msg,
                      },
                    },
                  ],
                },
              ],
            },
          )
        },
        async stubRetrieveRunsThatRequireAction(hashes: Record<string, string>[]) {
          const createRequiresActionRun = (functionName: string, argumentsObj: unknown) => {
            return {
              id: "run-abc123",
              status: "requires_action",
              required_action: {
                type: "submit_tool_outputs",
                submit_tool_outputs: {
                  tool_calls: [
                    {
                      type: "function",
                      function: {
                        name: functionName,
                        arguments: JSON.stringify(argumentsObj),
                      },
                    },
                  ],
                },
              },
            }
          }

          const responses = hashes.map((hash) => {
            switch (hash["response"]) {
              case "ask clarification question":
                return createRequiresActionRun("ask_clarification_question", {
                  question: hash["arguments"],
                })
              case "complete note details":
                return createRequiresActionRun("complete_note_details", {
                  completion: hash["arguments"]?.match(/"(.*)"/)?.[1],
                })
              default:
                throw new Error(`Unknown response: ${hash["response"]}`)
            }
          })

          return await serviceMocker.stubGetterWithMutipleResponses(
            `/threads/${threadId}/runs/run-abc123`,
            {},
            responses,
          )
        },
      }
    },

    async stubFineTuningStatus(successful: boolean) {
      return await serviceMocker.stubPoster(`/fine_tuning/jobs`, {
        object: "fine_tuning.job",
        id: "ftjob-abc123",
        model: "gpt-3.5-turbo-0613",
        created_at: 1614807352,
        fine_tuned_model: null,
        organization_id: "org-123",
        result_files: [],
        status: successful ? "queued" : "failed",
        validation_file: null,
        training_file: "file-abc123",
      })
    },

    async stubGetModels(modelNames: string) {
      return await serviceMocker.stubGetter(`/models`, undefined, {
        object: "list",
        data: modelNames.split(",").map((modelName) => {
          return {
            id: modelName.trim(),
            object: "model",
            created: 1614807352,
            owned_by: "openai",
          }
        }),
      })
    },
  }
}

export default openAiService
