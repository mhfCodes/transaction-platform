package com.mhf.transaction.controller.transaction;

import com.mhf.transaction.dto.transaction.TransferRequest;
import com.mhf.transaction.dto.transaction.TransferResponse;
import com.mhf.transaction.service.transaction.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(@RequestBody TransferRequest request) {

        TransferResponse response = transactionService.transfer(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }



}
