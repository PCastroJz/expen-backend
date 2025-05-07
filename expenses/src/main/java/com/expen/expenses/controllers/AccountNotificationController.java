package com.expen.expenses.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import com.expen.expenses.dtos.AccountDTO;

@RestController
public class AccountNotificationController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void notifyAccountUpdate(AccountDTO accountDTO, String action) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", action);
        payload.put("id", accountDTO.getId());
        payload.put("name", accountDTO.getName());

        messagingTemplate.convertAndSend("/topic/account-updates", payload);
    }
}