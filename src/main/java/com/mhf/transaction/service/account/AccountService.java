package com.mhf.transaction.service.account;

import com.mhf.transaction.dto.account.AccountResponse;
import com.mhf.transaction.dto.account.CreateAccountRequest;
import com.mhf.transaction.mapper.account.AccountMapper;
import com.mhf.transaction.model.account.Account;
import com.mhf.transaction.repository.account.AccountRepository;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    public AccountService(AccountRepository accountRepository,
                          AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
    }

    public AccountResponse createAccount(CreateAccountRequest request) {

        Account account = accountMapper.toEntity(request);

        Account savedAccount = accountRepository.save(account);

        return accountMapper.toResponse(savedAccount);

    }

}
