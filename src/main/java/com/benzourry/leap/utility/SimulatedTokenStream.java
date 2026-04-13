package com.benzourry.leap.utility;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
//import dev.langchain4j.model.output.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;

import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class SimulatedTokenStream implements TokenStream {
    private final String[] words;
    private final Executor executor;
    private final String fullText;

    private Consumer<String> partialHandler = s -> {};
    private Consumer<ChatResponse> completeHandler = r -> {};
    private Consumer<Throwable> errorHandler = t -> {};

    public SimulatedTokenStream(String text, Executor executor) {
        this.fullText = text;
        this.words = text.split(" ");
        this.executor = executor;
    }

    @Override
    public TokenStream onPartialResponse(Consumer<String> handler) {
        this.partialHandler = handler;
        return this;
    }

    @Override
    public TokenStream onRetrieved(Consumer<List<dev.langchain4j.rag.content.Content>> consumer) {
        return this;
    }

    @Override
    public TokenStream onToolExecuted(Consumer<ToolExecution> consumer) {
        return this;
    }

    @Override
    public void start() {
        emitRecursive(0);
    }

    private void emitRecursive(int index) {
        if (index < words.length) {
            try {
                partialHandler.accept(words[index] + " ");

                // Schedule the next word using the existing executor
                CompletableFuture.delayedExecutor(25, TimeUnit.MILLISECONDS, executor)
                        .execute(() -> emitRecursive(index + 1));
            } catch (Exception e) {
                errorHandler.accept(e);
            }
        } else {
            // Signal completion
            completeHandler.accept(ChatResponse.builder()
                    .aiMessage(AiMessage.from(fullText))
                    .build());
        }
    }

    // Minimal empty implementations for the rest
    @Override public TokenStream onCompleteResponse(Consumer<ChatResponse> handler) {
        this.completeHandler = handler;
        return this;
    }

    @Override public TokenStream onError(Consumer<Throwable> handler) {
        this.errorHandler = handler;
        return this;
    }

    @Override
    public TokenStream ignoreErrors() {
        return this; // Change from null to this
    }
}