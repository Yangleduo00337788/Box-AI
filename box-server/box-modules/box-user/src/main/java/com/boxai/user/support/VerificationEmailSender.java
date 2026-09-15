package com.boxai.user.support;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class VerificationEmailSender {

    private static final Logger log = LoggerFactory.getLogger(VerificationEmailSender.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final SystemConfigMailSenderFactory systemConfigMailSenderFactory;
    private final String defaultFromAddress;
    private final boolean exposeCode;

    public VerificationEmailSender(ObjectProvider<JavaMailSender> mailSenderProvider,
                                   SystemConfigMailSenderFactory systemConfigMailSenderFactory,
                                   @Value("${box.mail.from:noreply@box.ai}") String defaultFromAddress,
                                   @Value("${box.auth.verification.expose-code:false}") boolean exposeCode) {
        this.mailSenderProvider = mailSenderProvider;
        this.systemConfigMailSenderFactory = systemConfigMailSenderFactory;
        this.defaultFromAddress = defaultFromAddress;
        this.exposeCode = exposeCode;
    }

    public void send(String email, VerificationCodePurpose purpose, String code) {
        ResolvedMail resolved = resolveMailSender();
        if (resolved == null) {
            log.warn("Mail sender not configured; verification code for {} ({}) not emailed", email, purpose);
            if (!exposeCode) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "邮件服务未配置，无法发送验证码");
            }
            return;
        }

        try {
            MimeMessage message = resolved.sender().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(resolved.fromAddress());
            helper.setTo(email);
            helper.setSubject(subject(purpose));
            helper.setText(textBody(purpose, code), htmlBody(purpose, code));
            resolved.sender().send(message);
        } catch (MessagingException e) {
            log.error("Failed to compose verification email to {}", email, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "验证码邮件发送失败，请稍后重试");
        } catch (Exception e) {
            log.error("Failed to send verification email to {}", email, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "验证码邮件发送失败，请稍后重试");
        }
    }

    private ResolvedMail resolveMailSender() {
        var fromConfig = systemConfigMailSenderFactory.resolve();
        if (fromConfig.isPresent()) {
            var item = fromConfig.get();
            return new ResolvedMail(item.sender(), item.fromAddress());
        }
        JavaMailSender springSender = mailSenderProvider.getIfAvailable();
        if (springSender == null) {
            return null;
        }
        return new ResolvedMail(springSender, defaultFromAddress);
    }

    private record ResolvedMail(JavaMailSender sender, String fromAddress) {
    }

    private String subject(VerificationCodePurpose purpose) {
        return switch (purpose) {
            case REGISTER -> "Box 注册验证码";
            case RESET_PASSWORD -> "Box 重置密码验证码";
        };
    }

    private String textBody(VerificationCodePurpose purpose, String code) {
        String action = purpose == VerificationCodePurpose.REGISTER ? "注册 Box 账号" : "重置 Box 账号密码";
        return action + "的验证码是：" + code + "\n\n验证码 10 分钟内有效，请勿泄露给他人。";
    }

    private String htmlBody(VerificationCodePurpose purpose, String code) {
        String action = purpose == VerificationCodePurpose.REGISTER ? "注册 Box 账号" : "重置 Box 账号密码";
        return """
                <div style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;line-height:1.6;color:#1f2329;">
                  <p>你好，</p>
                  <p>你正在%s，验证码为：</p>
                  <p style="font-size:28px;font-weight:700;letter-spacing:4px;margin:16px 0;">%s</p>
                  <p style="color:#8a919f;">验证码 10 分钟内有效，请勿泄露给他人。</p>
                  <p style="color:#8a919f;">如非本人操作，请忽略此邮件。</p>
                </div>
                """.formatted(action, code);
    }
}
