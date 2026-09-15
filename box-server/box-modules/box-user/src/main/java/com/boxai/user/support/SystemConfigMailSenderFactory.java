package com.boxai.user.support;

import com.boxai.domain.config.SystemConfig;
import com.boxai.domain.config.SystemConfigRepository;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SystemConfigMailSenderFactory {

    private static final List<String> MAIL_KEYS = List.of(
            "mail.smtp.host",
            "mail.smtp.port",
            "mail.smtp.username",
            "mail.smtp.password",
            "mail.from");

    private final SystemConfigRepository systemConfigRepository;

    public SystemConfigMailSenderFactory(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    public Optional<ResolvedMailSender> resolve() {
        Map<String, String> values = systemConfigRepository.findByKeys(MAIL_KEYS).stream()
                .collect(Collectors.toMap(SystemConfig::getConfigKey, SystemConfig::getConfigValue, (a, b) -> a));
        String host = value(values, "mail.smtp.host");
        if (host == null || host.isBlank()) {
            return Optional.empty();
        }
        int port = parsePort(value(values, "mail.smtp.port"), 587);
        String username = value(values, "mail.smtp.username");
        String password = value(values, "mail.smtp.password");
        String from = value(values, "mail.from");
        if (from == null || from.isBlank()) {
            from = username;
        }
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host.trim());
        sender.setPort(port);
        sender.setUsername(username.trim());
        sender.setPassword(password);
        sender.setDefaultEncoding("UTF-8");

        var props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        if (port == 465) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", String.valueOf(port));
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        return Optional.of(new ResolvedMailSender(sender, from.trim()));
    }

    private static String value(Map<String, String> values, String key) {
        return values.get(key);
    }

    private static int parsePort(String raw, int defaultPort) {
        if (raw == null || raw.isBlank()) {
            return defaultPort;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return defaultPort;
        }
    }

    public record ResolvedMailSender(JavaMailSender sender, String fromAddress) {
    }
}
