package com.autoriaclone.service;

import com.autoriaclone.entity.Advertisement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public void notifyManagerAboutFailedAd(Advertisement ad) {
        String subject = "Оголошення #" + ad.getId() + " не пройшло авто-модерацію (3 спроби)";
        String body = String.format(
                "Вітаємо! Оголошення #%d (%s %s) від продавця %s переведено у статус INACTIVE_REVIEW після 3 невдалих спроб редагування. Опис: %s",
                ad.getId(), ad.getBrand().getName(), ad.getModel().getName(),
                ad.getSeller().getEmail(), ad.getDescription()
        );

        log.warn("[EMAIL TO MANAGER] Subject: {} | Body: {}", subject, body);

        if (mailSender != null) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo("manager@autoria.ua");
                msg.setSubject(subject);
                msg.setText(body);
                mailSender.send(msg);
            } catch (Exception e) {
                log.info("SMTP не налаштовано в середовищі, лист зафіксовано в логах системи.");
            }
        }
    }
}
