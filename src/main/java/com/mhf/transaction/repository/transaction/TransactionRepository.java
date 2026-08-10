package com.mhf.transaction.repository.transaction;

import com.mhf.transaction.model.transaction.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
