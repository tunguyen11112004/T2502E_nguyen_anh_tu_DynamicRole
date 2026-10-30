package org.fp.dynamicrole.user;

public enum AssignableRole {
    STUDENT("ROLE_STUDENT"),
    LECTURER("ROLE_LECTURER"),
    ADMIN("ROLE_ADMIN");

    private final String authority;

    AssignableRole(String authority) {
        this.authority = authority;
    }

    public String authority() {
        return authority;
    }
}
