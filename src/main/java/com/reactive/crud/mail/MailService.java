package com.reactive.crud.mail;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MailService {

    private static final Logger LOG = Logger.getLogger(MailService.class);

    @Inject
    ReactiveMailer mailer;

    public Uni<Void> sendProductCreatedNotification(Long productId, String productName, String recipientEmail) {
        String subject = "New Product Created";
        String body = String.format(
                "Hello,\n\n" +
                "A new product has been created:\n\n" +
                "Product ID: %d\n" +
                "Product Name: %s\n\n" +
                "Best regards,\n" +
                "Reactive CRUD App",
                productId, productName
        );

        return sendMail(recipientEmail, subject, body);
    }

    public Uni<Void> sendProductUpdatedNotification(Long productId, String productName, String recipientEmail) {
        String subject = "Product Updated";
        String body = String.format(
                "Hello,\n\n" +
                "A product has been updated:\n\n" +
                "Product ID: %d\n" +
                "Product Name: %s\n\n" +
                "Best regards,\n" +
                "Reactive CRUD App",
                productId, productName
        );

        return sendMail(recipientEmail, subject, body);
    }

    public Uni<Void> sendProductDeletedNotification(Long productId, String productName, String recipientEmail) {
        String subject = "Product Deleted";
        String body = String.format(
                "Hello,\n\n" +
                "A product has been deleted:\n\n" +
                "Product ID: %d\n" +
                "Product Name: %s\n\n" +
                "Best regards,\n" +
                "Reactive CRUD App",
                productId, productName
        );

        return sendMail(recipientEmail, subject, body);
    }

    private Uni<Void> sendMail(String to, String subject, String body) {
        return mailer.send(
                Mail.withText(to, subject, body)
        )
        .invoke(() -> LOG.infof("Email sent to %s with subject: %s", to, subject))
        .onFailure().invoke(failure ->
                LOG.errorf("Failed to send email to %s: %s", to, failure.getMessage())
        );
    }

    public Uni<Void> sendHtmlMail(String to, String subject, String htmlBody) {
        return mailer.send(
                Mail.withHtml(to, subject, htmlBody)
        )
        .invoke(() -> LOG.infof("HTML email sent to %s with subject: %s", to, subject))
        .onFailure().invoke(failure ->
                LOG.errorf("Failed to send HTML email to %s: %s", to, failure.getMessage())
        );
    }
}
