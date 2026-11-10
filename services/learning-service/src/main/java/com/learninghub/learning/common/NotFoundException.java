package com.learninghub.learning.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class NotFoundException extends ResponseStatusException {
    public NotFoundException(String what, Object id) {
        super(HttpStatus.NOT_FOUND, what + " " + id + " not found");
    }
}
