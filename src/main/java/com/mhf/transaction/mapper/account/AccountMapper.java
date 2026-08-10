package com.mhf.transaction.mapper.account;

import com.mhf.transaction.dto.account.AccountResponse;
import com.mhf.transaction.dto.account.CreateAccountRequest;
import com.mhf.transaction.model.account.Account;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    Account toEntity(CreateAccountRequest request);

    AccountResponse toResponse(Account account);

}
