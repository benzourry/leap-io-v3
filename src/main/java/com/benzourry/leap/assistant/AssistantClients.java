package com.benzourry.leap.assistant;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.TokenStream;

import java.util.List;

/** LangChain4j interfaces used only by the standalone app-builder assistant. */
public final class AssistantClients {

    private AssistantClients() {
    }

    public interface Assistant {

        @SystemMessage({
                "{{systemMessage}}",
                "Today is {{current_date}}."
        })
        @Agent("{{systemMessage}}. Today is {{current_date}}.")
        String chat(@UserMessage("userMessage") String userMessage,
                    @UserMessage("contentList") List<Content> contentList,
                    @V("systemMessage") String systemMessage);
    }

    public interface StreamingAssistant {

        @SystemMessage({
                "{{systemMessage}}",
                "Today is {{current_date}}."
        })
        TokenStream chat(@UserMessage("userMessage") String userMessage,
                         @UserMessage("contentList") List<Content> contentList,
                         @V("systemMessage") String systemMessage);
    }
}
