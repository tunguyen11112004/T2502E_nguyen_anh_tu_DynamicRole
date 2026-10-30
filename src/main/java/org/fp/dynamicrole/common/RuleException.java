package org.fp.dynamicrole.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RuleException extends ResponseStatusException {

    public RuleException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
