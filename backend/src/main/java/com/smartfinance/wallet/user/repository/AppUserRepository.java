package com.smartfinance.wallet.user.repository;

import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    List<AppUser> findAllByOrderByIdDesc();

    @Query("""
            select u from AppUser u
            where locate(lower(:search), lower(concat(concat(u.firstName, ' '), u.lastName))) > 0
               or locate(lower(:search), lower(u.email)) > 0
            order by u.id desc
            """)
    List<AppUser> searchAccounts(@Param("search") String search);

    long countByEnabledTrueAndBlockedFalse();

    long countByBlockedTrue();

    long countByRole(Role role);
}
