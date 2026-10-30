package org.fp.dynamicrole.user;

import org.fp.dynamicrole.common.NotFound;
import org.fp.dynamicrole.common.Pages;
import org.fp.dynamicrole.common.RuleException;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleAssignmentService {

    private final UserRepository users;
    private final RoleRepository roles;

    public RoleAssignmentService(UserRepository users, RoleRepository roles) {
        this.users = users;
        this.roles = roles;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserSummary> list(int page) {
        return users.findAll(Pages.users(page)).map(UserSummary::from);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void assign(Long userId, AssignableRole role) {
        var user = users.findWithRolesById(userId).orElseThrow(NotFound::user);
        boolean targetIsAdmin = user.getRoles().stream().anyMatch(item -> "ROLE_ADMIN".equals(item.getName()));
        if (targetIsAdmin && role != AssignableRole.ADMIN && users.countByRoles_Name("ROLE_ADMIN") <= 1) {
            throw new RuleException("Không thể gỡ quyền quản trị cuối cùng");
        }
        var stored = roles.findByName(role.authority())
                .orElseThrow(() -> new IllegalStateException("Thiếu " + role.authority() + " trong database"));
        user.replaceRoles(Set.of(stored));
    }

    public record UserSummary(Long id, String username, String roles) {
        static UserSummary from(AppUser user) {
            var names = user.getRoles().stream()
                    .map(Role::getName)
                    .sorted()
                    .collect(Collectors.joining(", "));
            return new UserSummary(user.getId(), user.getUsername(), names);
        }
    }
}
