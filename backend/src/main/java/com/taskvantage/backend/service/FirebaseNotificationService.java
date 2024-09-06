package com.taskvantage.backend.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

@Service
public class FirebaseNotificationService {

    public void sendNotification(String token, String title, String body) {
        if (token != null) {
            // Create a message to send to the user's device
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            // Log the details of the message being sent
            System.out.println("Sending notification with the following details:");
            System.out.println("Token: " + token);
            System.out.println("Title: " + title);
            System.out.println("Body: " + body);

            // Send the notification
            try {
                String response = FirebaseMessaging.getInstance().send(message);
                System.out.println("Successfully sent message: " + response);
            } catch (Exception e) {
                System.err.println("Error sending FCM notification: " + e.getMessage());
            }
        } else {
            System.err.println("FCM token is null, cannot send notification.");
        }
    }
}