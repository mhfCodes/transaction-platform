package com.mhf.transaction.repository.account;

import com.mhf.transaction.model.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}
