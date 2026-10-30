package org.fp.dynamicrole.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Pages {

    private Pages() {
    }

    public static Pageable assignments(int page) {
        return PageRequest.of(Math.max(page, 0), 10,
                Sort.by(Sort.Order.asc("deadline"), Sort.Order.desc("id")));
    }

    public static Pageable submissions(int page) {
        return PageRequest.of(Math.max(page, 0), 10,
                Sort.by(Sort.Order.desc("submittedAt"), Sort.Order.desc("id")));
    }

    public static Pageable users(int page) {
        return PageRequest.of(Math.max(page, 0), 10, Sort.by("username").ascending());
    }
}
