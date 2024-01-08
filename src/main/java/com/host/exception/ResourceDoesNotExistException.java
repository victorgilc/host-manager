package com.host.exception;

public class ResourceDoesNotExistException extends RuntimeException{
    public ResourceDoesNotExistException(final Long id){
        super(String.format("Resource with id \"%s\" doesn't exist", id));
    }
}
