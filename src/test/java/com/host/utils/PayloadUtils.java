package com.host.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.SneakyThrows;

import java.io.IOException;

@ApplicationScoped
public class PayloadUtils {

    @Inject
    ObjectMapper objectMapper;
    @SneakyThrows
    public JsonNode getPayload(final String path){
        return objectMapper.readValue( this.getClass().getResource(path), JsonNode.class);
    }
}
