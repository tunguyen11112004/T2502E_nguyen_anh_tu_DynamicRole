package org.fp.dynamicrole.task;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @InitBinder("form")
    void initBinder(WebDataBinder binder) {
        binder.setAllowedFields("title", "description", "dueDate", "status");
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model m) {
        m.addAttribute("tasks", service.mine(page));
        return "tasks/list";
    }

    private String renderForm(Model m, TaskForm form, Long id) {
        m.addAttribute("form", form);
        m.addAttribute("taskId", id);
        m.addAttribute("actionUrl", id == null ? "/tasks" : "/tasks/" + id);
        m.addAttribute("statuses", TaskStatus.values());
        return "tasks/form";
    }

    // Hạn mới không được trước hôm nay; giữ nguyên hạn cũ thì hợp lệ.
    private void checkDueDate(TaskForm form, LocalDate current, BindingResult errors) {
        if (!errors.hasFieldErrors("dueDate")
                && !form.isDueDateAcceptable(current, LocalDate.now())) {
            errors.rejectValue("dueDate", "task.dueDate.past",
                    "Hạn không được trước hôm nay");
        }
    }

    @GetMapping("/new")
    public String newForm(Model m) {
        return renderForm(m, new TaskForm(), null);
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("form") TaskForm form,
            BindingResult errors,
            Model m) {
        checkDueDate(form, null, errors);
        if (errors.hasErrors()) {
            return renderForm(m, form, null);
        }
        service.create(form);
        return "redirect:/tasks";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model m) {
        return renderForm(m, TaskForm.from(service.getMine(id)), id);
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") TaskForm form,
            BindingResult errors,
            Model m) {
        // Kiểm tra quyền sở hữu trước, kể cả khi form có lỗi (người lạ nhận 404)
        TaskView existing = service.getMine(id);
        checkDueDate(form, existing.dueDate(), errors);
        if (errors.hasErrors()) {
            return renderForm(m, form, id);
        }
        service.update(id, form);
        return "redirect:/tasks";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/tasks";
    }
}

