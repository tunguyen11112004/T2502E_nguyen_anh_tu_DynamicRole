package org.fp.dynamicrole;

import org.fp.dynamicrole.task.Task;
import org.fp.dynamicrole.task.TaskForm;
import org.fp.dynamicrole.task.TaskRepository;
import org.fp.dynamicrole.task.TaskService;
import org.fp.dynamicrole.task.TaskStatus;
import org.fp.dynamicrole.user.AppUser;
import org.fp.dynamicrole.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TaskSecurityTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    UserRepository users;
    @Autowired
    TaskRepository tasks;
    @Autowired
    TaskService taskService;

    Long aliceTaskId;

    @BeforeEach
    void setup() {
        var alice = users.saveAndFlush(new AppUser("task_alice", "unused"));
        users.saveAndFlush(new AppUser("task_bob", "unused"));
        TaskForm f = new TaskForm();
        f.setTitle("Alice private task");
        f.setDueDate(LocalDate.now().plusDays(1));
        aliceTaskId = tasks.saveAndFlush(new Task(alice, f)).getId();
    }

    @Test
    void anonymousMustLogin() throws Exception {
        mvc.perform(get("/tasks"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void bobCannotEditAliceTask() throws Exception {
        mvc.perform(get("/tasks/{id}/edit", aliceTaskId)
                        .with(user("task_bob").roles("USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void bobCannotUpdateAliceTask() throws Exception {
        mvc.perform(post("/tasks/{id}", aliceTaskId)
                        .with(user("task_bob").roles("USER")).with(csrf())
                        .param("title", "Hacked").param("description", "")
                        .param("dueDate", LocalDate.now().plusDays(2).toString())
                        .param("status", "DONE"))
                .andExpect(status().isNotFound());
        assertEquals("Alice private task",
                tasks.findById(aliceTaskId).orElseThrow().getTitle());
    }

    @Test
    void bobCannotDeleteAliceTask() throws Exception {
        mvc.perform(post("/tasks/{id}/delete", aliceTaskId)
                        .with(user("task_bob").roles("USER")).with(csrf()))
                .andExpect(status().isNotFound());
        assertTrue(tasks.existsById(aliceTaskId));
    }

    @Test
    void deleteWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/tasks/{id}/delete", aliceTaskId)
                        .with(user("task_alice").roles("USER")))
                .andExpect(status().isForbidden());
        assertTrue(tasks.existsById(aliceTaskId));
    }

    @Test
    void adminCannotUpdateAliceTask() throws Exception {
        mvc.perform(post("/tasks/{id}", aliceTaskId)
                        .with(user("task_admin").roles("ADMIN")).with(csrf())
                        .param("title", "Admin edit").param("description", "")
                        .param("dueDate", LocalDate.now().plusDays(2).toString())
                        .param("status", "DONE"))
                .andExpect(status().isNotFound());
        assertEquals("Alice private task",
                tasks.findById(aliceTaskId).orElseThrow().getTitle());
    }

    @Test
    void aliceCanUpdateOwnTask() throws Exception {
        mvc.perform(post("/tasks/{id}", aliceTaskId)
                        .with(user("task_alice").roles("USER")).with(csrf())
                        .param("title", "Updated").param("description", "Done")
                        .param("dueDate", LocalDate.now().plusDays(2).toString())
                        .param("status", "DONE"))
                .andExpect(redirectedUrl("/tasks"));
        assertEquals(TaskStatus.DONE,
                tasks.findById(aliceTaskId).orElseThrow().getStatus());
    }

    @Test
    void blankTitleIsNotSaved() throws Exception {
        long before = tasks.count();
        mvc.perform(post("/tasks")
                        .with(user("task_bob").roles("USER")).with(csrf())
                        .param("title", "   ").param("description", "")
                        .param("dueDate", LocalDate.now().plusDays(1).toString())
                        .param("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "title"));
        assertEquals(before, tasks.count());
    }

    @Test
    void cannotCreateTaskWithPastDueDate() throws Exception {
        long before = tasks.count();
        mvc.perform(post("/tasks")
                        .with(user("task_bob").roles("USER")).with(csrf())
                        .param("title", "Late").param("description", "")
                        .param("dueDate", LocalDate.now().minusDays(1).toString())
                        .param("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "dueDate"));
        assertEquals(before, tasks.count());
    }

    @Test
    void overdueTaskCanBeMarkedDoneKeepingDueDate() throws Exception {
        var alice = users.findByUsername("task_alice").orElseThrow();
        LocalDate past = LocalDate.now().minusDays(3);
        TaskForm f = new TaskForm();
        f.setTitle("Overdue");
        f.setDueDate(past);
        Long id = tasks.saveAndFlush(new Task(alice, f)).getId();

        mvc.perform(post("/tasks/{id}", id)
                        .with(user("task_alice").roles("USER")).with(csrf())
                        .param("title", "Overdue").param("description", "")
                        .param("dueDate", past.toString())
                        .param("status", "DONE"))
                .andExpect(redirectedUrl("/tasks"));
        assertEquals(TaskStatus.DONE,
                tasks.findById(id).orElseThrow().getStatus());
    }

    @Test
    void changingDueDateToThePastIsRejected() throws Exception {
        mvc.perform(post("/tasks/{id}", aliceTaskId)
                        .with(user("task_alice").roles("USER")).with(csrf())
                        .param("title", "Updated").param("description", "")
                        .param("dueDate", LocalDate.now().minusDays(1).toString())
                        .param("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "dueDate"));
        assertEquals("Alice private task",
                tasks.findById(aliceTaskId).orElseThrow().getTitle());
    }

    @Test
    void userCannotOpenAdmin() throws Exception {
        mvc.perform(get("/admin/tasks")
                        .with(user("task_bob").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanOpenAdmin() throws Exception {
        mvc.perform(get("/admin/tasks")
                        .with(user("task_admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/tasks"));
    }

    @Test
    @WithMockUser(username = "task_bob", roles = "USER")
    void userCannotCallAdminServiceMethod() {
        assertThrows(AccessDeniedException.class, () -> taskService.all(0));
    }
}
