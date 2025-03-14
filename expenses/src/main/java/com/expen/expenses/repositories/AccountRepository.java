package com.expen.expenses.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.expen.expenses.models.Account;
import java.util.List;
import java.util.Optional;


public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserId(Long userId);
    Optional<Account> findById(Long id);
}
