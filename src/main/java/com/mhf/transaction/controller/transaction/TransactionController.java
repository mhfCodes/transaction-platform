package com.mhf.transaction.controller.transaction;

import com.mhf.transaction.dto.transaction.TransactionRequest;
import com.mhf.transaction.dto.transaction.TransactionResponse;
import com.mhf.transaction.service.transaction.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> transfer(@RequestBody TransactionRequest request) {

        TransactionResponse response = transactionService.transfer(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getById(@PathVariable Long id) {

        TransactionResponse response = transactionService.getById(id);

        return ResponseEntity.ok(response);
    }

}
