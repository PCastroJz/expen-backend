package com.expen.expenses.controllers;

import com.expen.expenses.dtos.TransactionDTO;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionNotificationController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void notifyNewTransaction(TransactionDTO transactionDTO, String action) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", action);
        payload.put("accountId", transactionDTO.getAccountId());
        payload.put("id", transactionDTO.getId());
        payload.put("amount", transactionDTO.getAmount());
        payload.put("date", transactionDTO.getDate());
        payload.put("type", transactionDTO.getType());
        payload.put("category", transactionDTO.getCategory());
        payload.put("description", transactionDTO.getDescription());
        payload.put("paymentMethod", transactionDTO.getPaymentMethod());
        payload.put("schedule", transactionDTO.isSchedule());
        payload.put("pay", transactionDTO.isPay());

        messagingTemplate.convertAndSend("/topic/new-transactions", payload);
    }
}
