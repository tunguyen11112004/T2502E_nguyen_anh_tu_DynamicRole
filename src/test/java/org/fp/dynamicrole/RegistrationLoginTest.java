package org.fp.dynamicrole;

import org.fp.dynamicrole.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistrationLoginTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @Test
    void registerIgnoresRequestedRoleAndLogsIn() throws Exception {
        var result = mvc.perform(post("/register").with(csrf())
                        .param("username", "alice")
                        .param("password", "password1")
                        .param("passwordConfirm", "password1")
                        .param("roles", "ROLE_ADMIN"))
                .andExpect(redirectedUrl("/"))
                .andReturn();

        var saved = users.findByUsername("alice").orElseThrow();
        assertTrue(saved.getPassword().startsWith("{bcrypt}"));
        assertEquals(1, saved.getRoles().size());
        assertEquals("ROLE_STUDENT", saved.getRoles().iterator().next().getName());

        var session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("alice")));
    }

    @Test
    void duplicateUsernameIsRejected() throws Exception {
        register("alice");
        long before = users.count();
        mvc.perform(post("/register").with(csrf())
                        .param("username", "alice")
                        .param("password", "password1")
                        .param("passwordConfirm", "password1"))
                .andExpect(status().isOk());
        assertEquals(before, users.count());
    }

    @Test
    void passwordMismatchIsRejected() throws Exception {
        long before = users.count();
        mvc.perform(post("/register").with(csrf())
                        .param("username", "bob")
                        .param("password", "password1")
                        .param("passwordConfirm", "password2"))
                .andExpect(status().isOk());
        assertEquals(before, users.count());
    }

    @Test
    void registerWithoutCsrfIsRejected() throws Exception {
        long before = users.count();
        mvc.perform(post("/register")
                        .param("username", "carol")
                        .param("password", "password1")
                        .param("passwordConfirm", "password1"))
                .andExpect(status().isForbidden());
        assertEquals(before, users.count());
    }

    @Test
    void loginThenLogout() throws Exception {
        register("dave");
        var login = mvc.perform(post("/login").with(csrf())
                        .param("username", "dave")
                        .param("password", "password1"))
                .andExpect(redirectedUrl("/"))
                .andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(post("/logout").with(csrf()).session(session))
                .andExpect(redirectedUrl("/login?logout"));
    }

    private void register(String username) throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("username", username)
                        .param("password", "password1")
                        .param("passwordConfirm", "password1"))
                .andExpect(redirectedUrl("/"));
    }
}
