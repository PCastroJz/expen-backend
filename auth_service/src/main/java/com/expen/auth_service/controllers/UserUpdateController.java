package com.expen.auth_service.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.expen.auth_service.models.User;

@Controller
public class UserUpdateController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void broadcastUserUpdate(User updatedUser) {
        messagingTemplate.convertAndSend(
                "/topic/user-updates",
                updatedUser);
    }

    public void broadcastNewUser(User newUser) {
        messagingTemplate.convertAndSend(
                "/topic/new-users",
                newUser);
    }
}