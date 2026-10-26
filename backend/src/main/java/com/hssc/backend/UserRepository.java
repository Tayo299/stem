package com.hssc.backend;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface UserRepository extends JpaRepository<UserAccount,UUID> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    List<UserAccount> findByRoleAndStatus(Role role,AccountStatus status);
    List<UserAccount> findByStatusOrderByCreatedAtAsc(AccountStatus status);
    long countByStatus(AccountStatus status);
    long countByRoleAndStatus(Role role,AccountStatus status);
}
