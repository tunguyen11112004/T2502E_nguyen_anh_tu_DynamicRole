package org.fp.dynamicrole.web;

import org.fp.dynamicrole.user.RegistrationForm;
import org.fp.dynamicrole.user.RegistrationService;
import org.fp.dynamicrole.user.UsernameTakenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @InitBinder("form")
    void initBinder(WebDataBinder binder) {
        binder.setAllowedFields("username", "password", "passwordConfirm");
    }

    @GetMapping("/register")
    public String form(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("form") RegistrationForm form,
            BindingResult errors,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (!errors.hasErrors() && !form.getPassword().equals(form.getPasswordConfirm())) {
            errors.rejectValue("passwordConfirm", "mismatch", "Mật khẩu xác nhận không khớp");
        }
        if (errors.hasErrors()) {
            return "register";
        }
        try {
            registrationService.register(form, request, response);
        } catch (UsernameTakenException ex) {
            errors.rejectValue("username", "taken", "Tên đăng nhập đã tồn tại");
            return "register";
        }
        return "redirect:/";
    }
}
