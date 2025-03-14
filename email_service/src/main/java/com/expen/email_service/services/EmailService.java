package com.expen.email_service.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.util.Arrays;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String text, boolean isHtml) {
        sendEmail(new String[]{to}, subject, text, isHtml, null);
    }

    public void sendEmail(String[] to, String subject, String text, boolean isHtml) {
        sendEmail(to, subject, text, isHtml, null);
    }


    public void sendEmail(String to, String subject, String text, boolean isHtml, File[] attachments) {
        sendEmail(new String[]{to}, subject, text, isHtml, attachments);
    }

    public void sendEmail(String[] to, String subject, String text, boolean isHtml, File[] attachments) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, isHtml);

            if (attachments != null && attachments.length > 0) {
                for (File file : attachments) {
                    helper.addAttachment(file.getName(), file);
                }
            }

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new EmailSendingException("Error al enviar el correo", e);
        }
    }

    public static class EmailSendingException extends RuntimeException {
        public EmailSendingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}