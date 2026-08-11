package com.mhf.transaction.mapper.transaction;

import com.mhf.transaction.dto.transaction.TransferResponse;
import com.mhf.transaction.model.transaction.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(source = "id", target = "transactionId")
    @Mapping(source = "sourceAccount.id", target = "sourceAccountId")
    @Mapping(source = "destinationAccount.id", target = "destinationAccountId")
    TransferResponse toTransferResponse(Transaction transaction);

}
