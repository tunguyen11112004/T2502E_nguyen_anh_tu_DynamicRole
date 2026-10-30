package org.fp.dynamicrole.web;

import org.fp.dynamicrole.common.RuleException;
import org.fp.dynamicrole.course.AssignmentForm;
import org.fp.dynamicrole.course.AssignmentService;
import org.fp.dynamicrole.course.AssignmentStatus;
import org.fp.dynamicrole.submission.GradeForm;
import org.fp.dynamicrole.submission.SubmissionService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/lecturer/assignments")
public class LecturerAssignmentController {

    private final AssignmentService assignments;
    private final SubmissionService submissions;

    public LecturerAssignmentController(AssignmentService assignments, SubmissionService submissions) {
        this.assignments = assignments;
        this.submissions = submissions;
    }

    @InitBinder("form")
    void initAssignmentForm(WebDataBinder binder) {
        binder.setAllowedFields("title", "description", "deadline");
    }

    @InitBinder("grade")
    void initGradeForm(WebDataBinder binder) {
        binder.setAllowedFields("score", "feedback");
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) AssignmentStatus status,
                       Model model) {
        model.addAttribute("assignments", assignments.mine(page, status));
        model.addAttribute("status", status);
        model.addAttribute("statuses", AssignmentStatus.values());
        return "lecturer/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        return renderForm(model, new AssignmentForm(), null);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") AssignmentForm form, BindingResult errors, Model model) {
        if (errors.hasErrors()) {
            return renderForm(model, form, null);
        }
        try {
            var id = assignments.create(form);
            return "redirect:/lecturer/assignments/" + id + "/edit";
        } catch (RuleException ex) {
            errors.reject("business", ex.getReason());
            return renderForm(model, form, null);
        }
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        var assignment = assignments.getMine(id);
        model.addAttribute("assignment", assignment);
        return renderForm(model, assignments.formFrom(assignment), id);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") AssignmentForm form,
                         BindingResult errors,
                         Model model) {
        var assignment = assignments.getMine(id);
        model.addAttribute("assignment", assignment);
        if (errors.hasErrors()) {
            return renderForm(model, form, id);
        }
        try {
            assignments.update(id, form);
        } catch (RuleException ex) {
            errors.reject("business", ex.getReason());
            return renderForm(model, form, id);
        }
        return "redirect:/lecturer/assignments/" + id + "/edit";
    }

    @PostMapping("/{id}/open")
    public String open(@PathVariable Long id, RedirectAttributes redirect) {
        return act(id, redirect, () -> assignments.open(id));
    }

    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id, RedirectAttributes redirect) {
        return act(id, redirect, () -> assignments.close(id));
    }

    @GetMapping("/{id}/students")
    public String students(@PathVariable Long id, Model model) {
        model.addAttribute("assignment", assignments.getMine(id));
        model.addAttribute("students", assignments.students());
        return "lecturer/students";
    }

    @PostMapping("/{id}/students")
    public String assign(@PathVariable Long id,
                         @RequestParam(required = false) List<Long> studentIds,
                         RedirectAttributes redirect) {
        try {
            assignments.assign(id, studentIds);
        } catch (RuleException ex) {
            redirect.addFlashAttribute("error", ex.getReason());
        }
        return "redirect:/lecturer/assignments/" + id + "/students";
    }

    @GetMapping("/{id}/submissions")
    public String submissionList(@PathVariable Long id,
                                 @RequestParam(defaultValue = "0") int page,
                                 Model model) {
        model.addAttribute("assignment", assignments.getMine(id));
        model.addAttribute("submissions", submissions.forMyAssignment(id, page));
        return "lecturer/submissions";
    }

    @GetMapping("/{id}/submissions/{submissionId}/grade")
    public String gradeForm(@PathVariable Long id, @PathVariable Long submissionId, Model model) {
        model.addAttribute("assignment", assignments.getMine(id));
        model.addAttribute("submission", submissions.getForGrading(id, submissionId));
        model.addAttribute("grade", new GradeForm());
        return "lecturer/grade";
    }

    @PostMapping("/{id}/submissions/{submissionId}/grade")
    public String grade(@PathVariable Long id,
                        @PathVariable Long submissionId,
                        @Valid @ModelAttribute("grade") GradeForm grade,
                        BindingResult errors,
                        Model model) {
        if (!errors.hasErrors()) {
            try {
                submissions.grade(id, submissionId, grade);
                return "redirect:/lecturer/assignments/" + id + "/submissions";
            } catch (RuleException ex) {
                errors.reject("business", ex.getReason());
            }
        }
        model.addAttribute("assignment", assignments.getMine(id));
        model.addAttribute("submission", submissions.getForGrading(id, submissionId));
        return "lecturer/grade";
    }

    private String renderForm(Model model, AssignmentForm form, Long id) {
        model.addAttribute("form", form);
        model.addAttribute("assignmentId", id);
        model.addAttribute("actionUrl", id == null ? "/lecturer/assignments" : "/lecturer/assignments/" + id);
        if (id != null && !model.containsAttribute("assignment")) {
            model.addAttribute("assignment", assignments.getMine(id));
        }
        return "lecturer/form";
    }

    private String act(Long id, RedirectAttributes redirect, Runnable action) {
        try {
            action.run();
        } catch (RuleException ex) {
            redirect.addFlashAttribute("error", ex.getReason());
        }
        return "redirect:/lecturer/assignments/" + id + "/edit";
    }
}
