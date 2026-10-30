package org.fp.dynamicrole.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public String username() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
