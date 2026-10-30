package org.fp.dynamicrole.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    @EntityGraph(attributePaths = "roles")
    Optional<AppUser> findByUsername(String username);

    @EntityGraph(attributePaths = "roles")
    @Query("select u from AppUser u where u.id = :id")
    Optional<AppUser> findWithRolesById(@Param("id") Long id);

    @EntityGraph(attributePaths = "roles")
    List<AppUser> findByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = "roles")
    List<AppUser> findByRoles_NameOrderByUsernameAsc(String roleName);

    @Override
    @EntityGraph(attributePaths = "roles")
    Page<AppUser> findAll(Pageable pageable);

    long countByRoles_Name(String roleName);
}
