package com.expen.expenses.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.expen.expenses.models.AccountUser;

public interface AccountUserRepository extends JpaRepository<AccountUser, Long> {
    Optional<AccountUser> findByAccountIdAndUserId(Long accountId, Long userId);

    List<AccountUser> findByUserIdAndRole(Long userId, String role);

    @Query("SELECT au FROM AccountUser au WHERE au.account.id = :accountId")
    List<AccountUser> findByAccountId(@Param("accountId") Long accountId);
}
