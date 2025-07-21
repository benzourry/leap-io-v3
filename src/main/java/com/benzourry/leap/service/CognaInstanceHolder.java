package com.benzourry.leap.service;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CognaInstanceHolder {

    Long cognaId;

    EmbeddingModel embeddingModel;

    ChatModel chatModel;

    EmbeddingStore embeddingStore;

    ChatService.Assistant assistant;


    public CognaInstanceHolder(){}
}
