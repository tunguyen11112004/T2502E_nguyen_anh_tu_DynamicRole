package org.fp.dynamicrole.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class NotFound {

    private NotFound() {
    }

    public static ResponseStatusException assignment() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài tập");
    }

    public static ResponseStatusException submission() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài nộp");
    }

    public static ResponseStatusException user() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản");
    }
}
