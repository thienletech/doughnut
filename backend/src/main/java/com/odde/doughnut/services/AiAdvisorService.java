package com.odde.doughnut.services;

import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.services.openAiApis.OpenAiApis;
import com.theokanning.openai.OpenAiApi;
import java.util.List;
import reactor.core.publisher.Flux;

public class AiAdvisorService {
  private final OpenAiApis openAiApis;

  public AiAdvisorService(OpenAiApi openAiApi) {
    openAiApis = new OpenAiApis(openAiApi);
  }

  public Flux<AiSuggestion> getAiSuggestion(String prompt) {
    AiSuggestion aiSuggestion = new AiSuggestion(openAiApis.getOpenAiCompletion(prompt));
    return Flux.just(aiSuggestion);
  }

  public AiEngagingStory getEngagingStory(List<String> items) {
    final String topics = String.join(" and ", items);
    final String prompt = String.format("Tell me an engaging story to learn about %s.", topics);

    return new AiEngagingStory(openAiApis.getOpenAiCompletion(prompt));
  }
}
