package com.thenullproject.etherminewebscraper.service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EmailService {

    private final String HOST = "smtp.gmail.com"; // gmail smtp server to send emails
    private final Properties PROPS = new Properties();

    private final String SENDER_EMAIL;
    private final String SENDER_PASSWORD;

    public EmailService(String senderEmail, String senderPassword) {
        // init
        SENDER_EMAIL = senderEmail;
        SENDER_PASSWORD = senderPassword;

        PROPS.put("mail.smtp.auth", "true");
        PROPS.put("mail.smtp.starttls.enable", "true");
        PROPS.put("mail.smtp.host", HOST);
        PROPS.put("mail.smtp.port", 587); // 587 for TLS

    }

    public boolean sendEmail(String worker, String email, String message) {
        boolean success = true;

        //TODO: catch AuthenticationFailedException

        System.out.println("Sending mail ... ");

        Session session = Session.getInstance(PROPS,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                    }
                });

        try {
            Message mMessage = new MimeMessage(session);
            mMessage.setFrom(new InternetAddress(SENDER_EMAIL));
            mMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            mMessage.setSubject("ALERT: hash rate change for " + worker);
            mMessage.setText(message);

            Transport.send(mMessage);
        } catch (MessagingException e) {
            System.out.println(e.getLocalizedMessage());
            success = false;
        }

        return success;
    }
}
