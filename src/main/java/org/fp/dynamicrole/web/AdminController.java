package org.fp.dynamicrole.web;

import org.fp.dynamicrole.common.RuleException;
import org.fp.dynamicrole.course.AssignmentService;
import org.fp.dynamicrole.course.AssignmentStatus;
import org.fp.dynamicrole.user.AssignableRole;
import org.fp.dynamicrole.user.RoleAssignmentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AssignmentService assignments;
    private final RoleAssignmentService roles;

    public AdminController(AssignmentService assignments, RoleAssignmentService roles) {
        this.assignments = assignments;
        this.roles = roles;
    }

    @GetMapping
    public String stats(Model model) {
        model.addAttribute("stats", assignments.stats());
        return "admin/stats";
    }

    @GetMapping("/assignments")
    public String assignments(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(required = false) AssignmentStatus status,
                              Model model) {
        model.addAttribute("assignments", assignments.all(page, status));
        model.addAttribute("status", status);
        model.addAttribute("statuses", AssignmentStatus.values());
        return "admin/assignments";
    }

    @GetMapping("/users")
    public String users(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("users", roles.list(page));
        model.addAttribute("roleChoices", AssignableRole.values());
        return "admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String assignRole(@PathVariable Long id,
                             @RequestParam AssignableRole role,
                             RedirectAttributes redirect) {
        try {
            roles.assign(id, role);
        } catch (RuleException ex) {
            redirect.addFlashAttribute("error", ex.getReason());
        }
        return "redirect:/admin/users";
    }
}
