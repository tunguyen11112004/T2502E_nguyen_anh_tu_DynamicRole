package org.fp.dynamicrole.web;

import org.fp.dynamicrole.common.RuleException;
import org.fp.dynamicrole.course.AssignmentService;
import org.fp.dynamicrole.course.AssignmentStatus;
import org.fp.dynamicrole.submission.SubmissionForm;
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
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class StudentAssignmentController {

    private final AssignmentService assignments;
    private final SubmissionService submissions;

    public StudentAssignmentController(AssignmentService assignments, SubmissionService submissions) {
        this.assignments = assignments;
        this.submissions = submissions;
    }

    @InitBinder("form")
    void initBinder(WebDataBinder binder) {
        binder.setAllowedFields("content");
    }

    @GetMapping("/student/assignments")
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) AssignmentStatus status,
                       Model model) {
        var visible = status == AssignmentStatus.CLOSED ? AssignmentStatus.CLOSED : AssignmentStatus.OPEN;
        model.addAttribute("assignments", assignments.assigned(page, visible));
        model.addAttribute("status", visible);
        return "student/list";
    }

    @GetMapping("/student/assignments/{id}")
    public String detail(@PathVariable Long id, Model model) {
        fillDetail(id, model, new SubmissionForm(), true);
        return "student/detail";
    }

    @PostMapping("/student/assignments/{id}/submissions")
    public String submit(@PathVariable Long id,
                         @Valid @ModelAttribute("form") SubmissionForm form,
                         BindingResult errors,
                         Model model) {
        if (!errors.hasErrors()) {
            try {
                submissions.submit(id, form);
                return "redirect:/student/assignments/" + id;
            } catch (RuleException ex) {
                errors.reject("business", ex.getReason());
                model.addAttribute("error", ex.getReason());
            }
        }
        fillDetail(id, model, form, false);
        return "student/detail";
    }

    @GetMapping("/student/submissions/{id}")
    public String submission(@PathVariable Long id, Model model) {
        model.addAttribute("submission", submissions.getMine(id));
        return "student/submission";
    }

    private void fillDetail(Long id, Model model, SubmissionForm form, boolean prefill) {
        var assignment = assignments.getAssigned(id);
        var mine = submissions.mineForAssignment(id);
        if (prefill) {
            mine.ifPresent(view -> form.setContent(view.content()));
        }
        model.addAttribute("assignment", assignment);
        model.addAttribute("submission", mine.orElse(null));
        model.addAttribute("canSubmit", assignments.canSubmit(id));
        model.addAttribute("form", form);
    }
}
