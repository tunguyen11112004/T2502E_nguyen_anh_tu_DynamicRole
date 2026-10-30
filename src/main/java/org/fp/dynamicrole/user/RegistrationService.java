package org.fp.dynamicrole.user;

import org.fp.dynamicrole.security.AutoLogin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final AutoLogin autoLogin;

    public RegistrationService(
            UserRepository users,
            RoleRepository roles,
            PasswordEncoder passwordEncoder,
            AutoLogin autoLogin) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.autoLogin = autoLogin;
    }

    @Transactional
    public void register(RegistrationForm form, HttpServletRequest request, HttpServletResponse response) {
        String username = form.getUsername().strip();
        if (users.findByUsername(username).isPresent()) {
            throw new UsernameTakenException();
        }
        var userRole = roles.findByName("ROLE_STUDENT")
                .orElseThrow(() -> new IllegalStateException("Thiếu ROLE_STUDENT trong database"));
        var user = new AppUser(username, passwordEncoder.encode(form.getPassword()));
        user.addRole(userRole);
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new UsernameTakenException();
        }
        autoLogin.login(username, form.getPassword(), request, response);
    }
}
