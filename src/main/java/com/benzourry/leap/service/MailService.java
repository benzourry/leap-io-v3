package com.benzourry.leap.service;

import com.benzourry.leap.config.Constant;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.AppUserRepository;
import com.benzourry.leap.repository.UserRepository;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.TenantLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import org.stringtemplate.v4.StringRenderer;
import org.stringtemplate.v4.compiler.STException;

import java.time.Year;
import java.util.*;
import java.util.regex.Pattern;

import static com.benzourry.leap.config.Constant.*;

@Service("mailService")
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final STGroup ST_GROUP;

    // 3. Load from Classpath inside the static block
    static {
        // Fetch the template as a URL from the classpath
        java.net.URL templateUrl = MailService.class.getResource("/email.tpl.stg");

        if (templateUrl == null) {
            TenantLogger.error(null, "mailer", null, "Critical error: Could not find /email.tpl.stg in the classpath!");
            throw new IllegalStateException("Critical error: Could not find /email.tpl.stg in the classpath!");
        }

        // Pass the URL to the constructor, along with the encoding and delimiters
        ST_GROUP = new STGroupFile(templateUrl, "UTF-8", '{', '}');
        ST_GROUP.registerRenderer(Object.class, new StringRenderer());
    }

    private final JavaMailSender mailSender;
    private final NotificationService notificationService;
    private final EmailTemplateService emailTemplateService;
    private final UserRepository userRepository;
    private final AppUserRepository appUserRepository;
    private final ObjectMapper MAPPER;
    private final MailService self;

    @Value("${app.mailer.use-email}")
    boolean useEmail;

    public MailService(JavaMailSender mailSender,
                       NotificationService notificationService,
                       EmailTemplateService emailTemplateService,
                       UserRepository userRepository,
                       @Lazy MailService self,
                       AppUserRepository appUserRepository, ObjectMapper MAPPER) {
        this.mailSender = mailSender;
        this.notificationService = notificationService;
        this.emailTemplateService = emailTemplateService;
        this.userRepository = userRepository;
        this.appUserRepository = appUserRepository;
        this.self = self;
        this.MAPPER = MAPPER;
    }

    /**
     * FOR LAMBDA USAGE
     **/
    public void send(Map<String, String> params, Lambda lambda, String initBy) {
        String from = Optional.ofNullable(params.get("from")).orElse(lambda.getApp().getAppPath() + "_" + LEAP_MAILER);

        String toParam = params.get("to");
        String[] to = (toParam != null && !toParam.isBlank()) ? toParam.split(",") : null;

        String[] cc = params.get("cc") != null ? params.get("cc").split(",") : null;
        String[] bcc = params.get("bcc") != null ? params.get("bcc").split(",") : null;

        String subject = params.get("subject");
        String content = params.get("content");

        if (params.get("mailerId") != null) {
            EmailTemplate et = emailTemplateService.getEmailTemplate(Long.valueOf(params.get("mailerId")));
            subject = et.getSubject();
            content = et.getContent();
        }

        this.sendMailApp(from, to, cc, bcc, subject, content, lambda.getApp());
    }

    /**
     * FOR LAMBDA USAGE
     **/
    public void sendWithTemplate(Integer templateId, Entry entry, Lambda lambda, String initBy) {
        EmailTemplate et = emailTemplateService.getEmailTemplate(Long.valueOf(templateId));
        triggerMailer(et, Optional.ofNullable(entry).orElse(new Entry()), initBy);
    }

    public void triggerMailer(EmailTemplate template, Entry entry, String initBy) {
        if (template == null || entry == null) return;

        try {
            Map<String, Object> contentMap = new HashMap<>();
            contentMap.put("_", MAPPER.convertValue(entry, Map.class));

            Map<String, Object> result = MAPPER.convertValue(entry.getData(), Map.class);
            Map<String, Object> prev = MAPPER.convertValue(entry.getPrev(), Map.class);

            if (result != null) {
                contentMap.put("code", result.get("$code"));
                contentMap.put("id", result.get("$id"));
                contentMap.put("counter", result.get("$counter"));
            }

            if (prev != null) {
                contentMap.put("prev_code", prev.get("$code"));
                contentMap.put("prev_id", prev.get("$id"));
                contentMap.put("prev_counter", prev.get("$counter"));
            }

            contentMap.put("data", result);
            contentMap.put("prev", prev);

            // 1. SAFEGUARD: Extract Form and App safely
            Form form = entry.getForm();
            App app = form != null ? form.getApp() : null;

            // 2. SAFEGUARD: Only build URIs and fetch user if App exists
            if (app != null) {
                String url = "https://";
                if (app.getAppDomain() != null) {
                    url += app.getAppDomain() + "/#";
                } else {
                    String dev = app.isLive() ? "" : "--dev";
                    url += app.getAppPath() + dev + "." + Constant.UI_BASE_DOMAIN + "/#";
                }

                contentMap.put("uiUri", url);

                // Only set viewUri and editUri if Form exists
                if (form != null && form.getId() != null) {
                    contentMap.put("viewUri", url + "/form/" + form.getId() + "/view?entryId=" + entry.getId());
                    contentMap.put("editUri", url + "/form/" + form.getId() + "/edit?entryId=" + entry.getId());
                }

                if (entry.getEmail() != null) {
                    userRepository.findFirstByEmailAndAppId(entry.getEmail(), app.getId())
                            .ifPresent(u -> contentMap.put("user", MAPPER.convertValue(u, Map.class)));
                }
            }

            // 3. SAFEGUARD: Fetch admin emails only if Form and Admin exist
            List<String> adminEmails = new ArrayList<>();
            if (form != null && form.getAdmin() != null && (template.isToAdmin() || template.isCcAdmin())) {
                adminEmails = appUserRepository.findByGroupId(form.getAdmin().getId(), Pageable.unpaged())
                        .filter(appUser -> appUser.getUser() != null)
                        .map(appUser -> appUser.getUser().getEmail())
                        .toList();
            }

            List<String> recipients = new ArrayList<>();
            if (template.isToUser() && entry.getEmail() != null) recipients.add(entry.getEmail());
            if (template.isToAdmin() && !adminEmails.isEmpty()) recipients.addAll(adminEmails);

            if (template.getToExtra() != null) {
                String extra = Helper.compileTpl(template.getToExtra(), contentMap);
                if (!extra.isEmpty()) {
                    recipients.addAll(Arrays.stream(extra.replaceAll(" ", "").split(","))
                            .filter(str -> !str.isBlank()).toList());
                }
            }

            List<String> recipientsCc = new ArrayList<>();
            if (template.isCcUser() && entry.getEmail() != null) recipientsCc.add(entry.getEmail());
            if (template.isCcAdmin() && !adminEmails.isEmpty()) recipientsCc.addAll(adminEmails);

            if (template.getCcExtra() != null) {
                String ccextra = Helper.compileTpl(template.getCcExtra(), contentMap);
                if (!ccextra.isEmpty()) {
                    recipientsCc.addAll(Arrays.stream(ccextra.replaceAll(" ", "").split(","))
                            .filter(str -> !str.isBlank()).toList());
                }
            }

            // 4. SAFEGUARD: Fallback 'from' prefix if App is null
            String from = (app != null ? app.getAppPath() + "_" : "") + Constant.LEAP_MAILER;

            self.sendMail(from, recipients.toArray(new String[0]), recipientsCc.toArray(new String[0]), null, template, contentMap, app, initBy, entry.getId());

        } catch (Exception e) {
            TenantLogger.error(template.getAppId(), "mailer", template.getId(), "Error triggering mailer: " + e.getMessage());
            logger.error("Error triggering mailer", e);
        }
    }


    private String resolveAppLogo(App app) {
        if (app == null) return IO_BASE_DOMAIN + "/" + UI_BASE_DOMAIN + "-72.png";
        return app.getLogo() == null
                ? IO_BASE_DOMAIN + "/" + UI_BASE_DOMAIN + "-72.png"
                : IO_BASE_DOMAIN + "/api/app/logo/" + app.getLogo();
    }

    // 2. Private Helper: Centralizes the regex array sanitization logic
    private String[] sanitizeEmails(String[] emails) {
        if (emails == null) return null;
        return Arrays.stream(emails)
                .map(String::trim)
                .filter(e -> !e.isEmpty() && EMAIL_PATTERN.matcher(e).matches())
                .toArray(String[]::new);
    }


    public void sendMailApp(String from, String[] to, String[] cc, String[] bcc, String title, String content, App app) {
        try {
            to = sanitizeEmails(to);
            cc = sanitizeEmails(cc);
            bcc = sanitizeEmails(bcc);

            boolean hasTo = to != null && to.length > 0;
            boolean hasCc = cc != null && cc.length > 0;
            boolean hasBcc = bcc != null && bcc.length > 0;

            if (!hasTo && !hasCc && !hasBcc) {
                throw new IllegalArgumentException("No validly formatted recipients (to, cc, or bcc) found after filtering.");
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // CRITICAL: Reverted to using the class-level ST_GROUP cache.
            // Creating new STGroupFile on every email reads from the disk every time and will bottleneck your app!
//            final STGroup stGroup = new STGroupFile("/email.tpl.stg", '{', '}');
//            final ST templateExample = stGroup.getInstanceOf("emailTemplate");

            final ST templateExample = ST_GROUP.getInstanceOf("emailTemplate");

            templateExample.add("content", content);
            templateExample.add("appName", app.getTitle());
            templateExample.add("appLogo", resolveAppLogo(app));
            templateExample.add("currentYear", Year.now().getValue());

            if (useEmail) {
                from = LEAP_MAILER;
                title = "[" + app.getAppPath() + "] " + title;
            }

            message.setFrom(from);
            message.setSubject(title);
            message.setText(templateExample.render(), true);

            if (hasTo) message.setTo(to);
            if (hasCc) message.setCc(cc);
            if (hasBcc) message.setBcc(bcc);

            mailSender.send(mimeMessage);


        } catch (AddressException e) {
            // 1. Added full recipient context to AddressException
            String recipientsContext = String.format("To: %s, Cc: %s, Bcc: %s", Arrays.toString(to), Arrays.toString(cc), Arrays.toString(bcc));
            String errorMsg = String.format("Invalid email address. %s | Error Ref: %s", recipientsContext, e.getRef());

            logger.warn(errorMsg);
            if (app != null) TenantLogger.error(app.getId(), "mailer", null, errorMsg);

        } catch (STException e) {
            String errorMsg = "Cannot send email, problem with mailer template script. Reason: " + e.getMessage();

            logger.warn(errorMsg);
            if (app != null) TenantLogger.error(app.getId(), "mailer", null, errorMsg);

        } catch (Exception e) {
            String recipientsContext = String.format("To: %s, Cc: %s, Bcc: %s", Arrays.toString(to), Arrays.toString(cc), Arrays.toString(bcc));
            String errorMsg = String.format("Error sending email Subject: [%s] | From: [%s] | %s | Reason: %s",
                    title, from, recipientsContext, e.getMessage());

            logger.error(errorMsg, e); // Full stack trace logged to console
            if (app != null) TenantLogger.error(app.getId(), "mailer", null, errorMsg);
        }

    }

    public void sendMail(String from, String[] to, String[] cc, String[] bcc, String title, String content, App app) {
        try {
            to = sanitizeEmails(to);
            cc = sanitizeEmails(cc);
            bcc = sanitizeEmails(bcc);

            boolean hasTo = to != null && to.length > 0;
            boolean hasCc = cc != null && cc.length > 0;
            boolean hasBcc = bcc != null && bcc.length > 0;

            if (!hasTo && !hasCc && !hasBcc) {
                throw new IllegalArgumentException("No validly formatted recipients (to, cc, or bcc) found after filtering.");
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // Load the file
//            final STGroup stGroup = new STGroupFile("/email.tpl.stg", '{', '}');
//            final ST templateExample = stGroup.getInstanceOf("emailTemplate");

            final ST templateExample = ST_GROUP.getInstanceOf("emailTemplate");
            templateExample.add("content", content);
            templateExample.add("appLogo", resolveAppLogo(app));
            templateExample.add("currentYear", Year.now().getValue());

            message.setFrom(from);
            message.setSubject(title);
            message.setText(templateExample.render(), true);

            if (hasTo) message.setTo(to);
            if (hasCc) message.setCc(cc);
            if (hasBcc) message.setBcc(bcc);

            mailSender.send(mimeMessage);


        } catch (AddressException e) {
            // 1. Added full recipient context to AddressException
            String recipientsContext = String.format("To: %s, Cc: %s, Bcc: %s", Arrays.toString(to), Arrays.toString(cc), Arrays.toString(bcc));
            String errorMsg = String.format("Invalid email address. %s | Error Ref: %s", recipientsContext, e.getRef());

            logger.warn(errorMsg);
            if (app != null) TenantLogger.error(app.getId(), "mailer", null, errorMsg);

        } catch (STException e) {
            String errorMsg = "Cannot send email, problem with mailer template script. Reason: " + e.getMessage();

            logger.warn(errorMsg);
            if (app != null) TenantLogger.error(app.getId(), "mailer", null, errorMsg);

        } catch (Exception e) {
            String recipientsContext = String.format("To: %s, Cc: %s, Bcc: %s", Arrays.toString(to), Arrays.toString(cc), Arrays.toString(bcc));
            String errorMsg = String.format("Error sending email Subject: [%s] | From: [%s] | %s | Reason: %s",
                    title, from, recipientsContext, e.getMessage());

            logger.error(errorMsg, e); // Full stack trace logged to console
            if (app != null) TenantLogger.error(app.getId(), "mailer", null, errorMsg);
        }

    }

    @Async("asyncExec")
    public void sendMail(String from, String[] to, String[] cc, String[] bcc, EmailTemplate emailTemplate, Map<String, Object> contentParameter, App app, String initBy, Long entryId) {

        if (emailTemplate == null || !Integer.valueOf(1).equals(emailTemplate.getEnabled())) {
            logger.warn("Cannot send e-mail. Invalid or disabled Template Id specified");
            if (emailTemplate != null) {
                TenantLogger.error(emailTemplate.getAppId(), "mailer", emailTemplate.getId(), "Cannot send e-mail. Invalid Template Id specified or Mailer Template is DISABLED");
            }
            return;
        }

        String subject = null;
        String content = null;
        String status = "failed";

        // Keep booleans scoped outside the try block so the finally/notification block can read them safely
        boolean hasTo = false;
        boolean hasCc = false;
        boolean hasBcc = false;

        try {
            to = sanitizeEmails(to);
            cc = sanitizeEmails(cc);
            bcc = sanitizeEmails(bcc);

            hasTo = to != null && to.length > 0;
            hasCc = cc != null && cc.length > 0;
            hasBcc = bcc != null && bcc.length > 0;

            if (!hasTo && !hasCc && !hasBcc) {
                throw new IllegalArgumentException("No validly formatted recipients (to, cc, or bcc) found after filtering.");
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            // OPTIMIZATION: Removed 'true' flag to reduce MIME payload size
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            subject = Helper.compileTpl(emailTemplate.getSubject(), contentParameter);
            content = Helper.compileTpl(emailTemplate.getContent(), contentParameter);

            if (useEmail) {
                from = Constant.LEAP_MAILER;
                subject = "[" + app.getAppPath() + "] " + subject;
            }

            final ST templateExample = ST_GROUP.getInstanceOf("emailTemplate");
            templateExample.add("content", content);
            templateExample.add("appName", app.getTitle());
            templateExample.add("appLogo", resolveAppLogo(app));
            templateExample.add("currentYear", String.valueOf(Year.now().getValue()));

            message.setFrom(from);
            message.setSubject(subject);
            message.setText(templateExample.render(), true);

            if (hasTo) message.setTo(to);
            if (hasCc) message.setCc(cc);
            if (hasBcc) message.setBcc(bcc);

            mailSender.send(mimeMessage);
            status = "new";

        } catch (AddressException e) {
            String recipientsContext = String.format("To: %s, Cc: %s, Bcc: %s", Arrays.toString(to), Arrays.toString(cc), Arrays.toString(bcc));
            String errorMsg = String.format("Invalid email address. %s | Error Ref: %s", recipientsContext, e.getRef());

            logger.warn(errorMsg);
            if (app != null) TenantLogger.error(app.getId(), "mailer", null, errorMsg);

        } catch (STException e) {
            String errorMsg = "Cannot send email, problem with mailer template script. Reason: " + e.getMessage();

            logger.warn(errorMsg);
            if (app != null) TenantLogger.error(app.getId(), "mailer", null, errorMsg);

        } catch (Exception e) {
            String recipientsContext = String.format("To: %s, Cc: %s, Bcc: %s", Arrays.toString(to), Arrays.toString(cc), Arrays.toString(bcc));
            String errorMsg = String.format("Error sending email Subject: [%s] | From: [%s] | %s | Reason: %s",
                    subject, from, recipientsContext, e.getMessage());

            logger.error(errorMsg, e);
            if (app != null) TenantLogger.error(app.getId(), "mailer", null, errorMsg);
        }

        if (emailTemplate.isLog()) {
            Notification n = new Notification();

            // OPTIMIZATION: Reusing boolean flags so arrays don't have to be re-evaluated
            StringJoiner allEmails = new StringJoiner(",");
            if (hasTo) allEmails.add(String.join(",", to));
            if (hasCc) allEmails.add(String.join(",", cc));
            if (hasBcc) allEmails.add(String.join(",", bcc));

            n.setEmail(allEmails.toString());
            n.setTimestamp(new Date());
            n.setAppId(app.getId());
            n.setEmailTemplateId(emailTemplate.getId());
            n.setSubject(subject);
            n.setContent(content);
            n.setSender(from);
            n.setInitBy(initBy != null ? initBy : from);
            n.setEntryId(entryId);
            n.setStatus(status);
            notificationService.save(n);
        }
    }
}