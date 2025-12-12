/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.benzourry.leap.service;

import com.benzourry.leap.config.Constant;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.AppUserRepository;
import com.benzourry.leap.repository.UserRepository;
import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.MimeMessage;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import org.stringtemplate.v4.StringRenderer;

import java.time.Year;
import java.util.*;

import static com.benzourry.leap.config.Constant.*;


@Service("mailService")
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);
    private final JavaMailSender mailSender;
    private final NotificationService notificationService;
    private final EmailTemplateService emailTemplateService;
    private final UserRepository userRepository;
    private final AppUserRepository appUserRepository;
    private final ObjectMapper MAPPER;

    @Value("${app.mailer.use-email}")
    boolean useEmail;

    public MailService(JavaMailSender mailSender,
                       NotificationService notificationService,
                       EmailTemplateService emailTemplateService,
                       UserRepository userRepository,
                       AppUserRepository appUserRepository, ObjectMapper MAPPER) {
        this.mailSender = mailSender;
        this.notificationService = notificationService;
        this.emailTemplateService = emailTemplateService;
        this.userRepository = userRepository;
        this.appUserRepository = appUserRepository;

        this.MAPPER = MAPPER;
    }

    /**
     * FOR LAMBDA USAGE
     **/
    public void send(Map<String, String> params, Lambda lambda, String initBy) {

        String from = Optional.ofNullable(params.get("from")).orElse(lambda.getApp().getAppPath() + "_" + LEAP_MAILER);

        String[] to = Optional.ofNullable(params.get("to")).orElse("").split(",");
        String[] cc = null;
        String[] bcc = null;
        if (params.get("cc") != null) {
            cc = params.get("cc").split(",");
        }
        if (params.get("bcc") != null) {
            bcc = params.get("bcc").split(",");
        }
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
        try {
            if (template != null) {
                Map<String, Object> contentMap = new HashMap<>();
                contentMap.put("_", MAPPER.convertValue(entry, Map.class));
                Map<String, Object> result = MAPPER.convertValue(entry.getData(), Map.class);
                Map<String, Object> prev = MAPPER.convertValue(entry.getPrev(), Map.class);


                App app = entry.getForm().getApp();
                String url = "https://";
                if (entry.getForm().getApp().getAppDomain() != null) {
                    url += app.getAppDomain() + "/#";
                } else {
                    String dev = app.isLive() ? "" : "--dev";
                    url += app.getAppPath() + dev + "." + Constant.UI_BASE_DOMAIN + "/#";
                }

                contentMap.put("uiUri", url);
                contentMap.put("viewUri", url + "/form/" + entry.getForm().getId() + "/view?entryId=" + entry.getId());
                contentMap.put("editUri", url + "/form/" + entry.getForm().getId() + "/edit?entryId=" + entry.getId());

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

                Optional<User> u = userRepository.findFirstByEmailAndAppId(entry.getEmail(), entry.getForm().getApp().getId());
                if (u.isPresent()) {
                    Map userMap = MAPPER.convertValue(u.get(), Map.class);
                    contentMap.put("user", userMap);
                }

                List<String> recipients = new ArrayList<>();// Arrays.asList(entry.getEmail());
                if (template.isToUser()) {
                    recipients.add(entry.getEmail());
                }
                if (template.isToAdmin()) {
                    if (entry.getForm().getAdmin() != null) {
                        List<String> adminEmails = appUserRepository.findByGroupId(entry.getForm().getAdmin().getId(), Pageable.unpaged())
                            .filter(appUser -> appUser.getUser() != null)
                            .map(appUser -> appUser.getUser().getEmail()).toList();
                        if (!adminEmails.isEmpty()) {
                            recipients.addAll(adminEmails);
                        }
                    }
                }
                if (!Objects.isNull(template.getToExtra())) {
                    String extra = Helper.compileTpl(template.getToExtra(), contentMap);
                    if (!extra.isEmpty()) {
                        recipients.addAll(Arrays.stream(extra.replaceAll(" ", "").split(","))
                                .filter(str -> !str.isBlank())
                                .toList());
                    }
                }

                List<String> recipientsCc = new ArrayList<>();
                if (template.isCcUser()) {
                    recipientsCc.add(entry.getEmail());
                }
                if (template.isCcAdmin()) {
                    if (entry.getForm().getAdmin() != null) {
                        List<String> adminEmails = appUserRepository.findByGroupId(entry.getForm().getAdmin().getId(), Pageable.unpaged())
                                .filter(appUser -> appUser.getUser() != null)
                                .map(appUser -> appUser.getUser().getEmail()).toList();
                        if (!adminEmails.isEmpty()) {
                            recipientsCc.addAll(adminEmails);
                        }
                    }

                }
                if (!Objects.isNull(template.getCcExtra())) {
                    String ccextra = Helper.compileTpl(template.getCcExtra(), contentMap);
                    if (!ccextra.isEmpty()) {
                        recipientsCc.addAll(Arrays.stream(ccextra.replaceAll(" ", "").split(","))
                                .filter(str -> !str.isBlank())
                                .toList());
                    }
                }


                String[] rec = recipients.toArray(new String[0]);
                String[] recCc = recipientsCc.toArray(new String[0]);

                String from = entry.getForm().getApp().getAppPath() + "_" + Constant.LEAP_MAILER;

                sendMail(from, rec, recCc, null, template, contentMap, entry.getForm().getApp(), initBy, entry.getId());
            } else {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void sendMailApp(String from, String[] to, String[] cc, String[] bcc, String title, String content, App app) {

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // Load the file
            final STGroup stGroup = new STGroupFile("/email.tpl.stg", '{', '}');

            // Pick the correct template
            final ST templateExample = stGroup.getInstanceOf("emailTemplate");

            // Pass on values to use when rendering
            templateExample.add("content", content);

            templateExample.add("appName", app.getTitle());

            String appLogo = app.getLogo() == null ? IO_BASE_DOMAIN + "/" + UI_BASE_DOMAIN + "-72.png" : IO_BASE_DOMAIN + "/api/app/logo/" + app.getLogo();
            templateExample.add("appLogo", appLogo);

            int currentYear = Year.now().getValue();
            templateExample.add("currentYear", currentYear);

            // Render
            final String render = templateExample.render();

            if (useEmail){
                from = LEAP_MAILER;
                title = "["+ app.getAppPath()+"] " + title;
            }

            message.setFrom(from);
            message.setTo(to);
            message.setSubject(title);
            message.setText(render, true);

            if (cc != null && cc.length > 0)
                message.setCc(cc);
            if (bcc != null && bcc.length > 0)
                message.setBcc(bcc);

            mailSender.send(mimeMessage);
        } catch (Exception e) {
            System.out.println("Email sending error:"+ e.getMessage());
        }
    }


    public void sendMail(String from, String[] to, String[] cc, String[] bcc, String title, String content, App app) {

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // Load the file
            final STGroup stGroup = new STGroupFile("/email.tpl.stg", '{', '}');

            // Pick the correct template
            final ST templateExample = stGroup.getInstanceOf("emailTemplate");

            // Pass on values to use when rendering
            templateExample.add("content", content);

            String appLogo = IO_BASE_DOMAIN + "/" + UI_BASE_DOMAIN + "-72.png";
            if (app!=null){
                appLogo = app.getLogo() == null ? IO_BASE_DOMAIN + "/" + UI_BASE_DOMAIN + "-72.png" : IO_BASE_DOMAIN + "/api/app/logo/" + app.getLogo();
            }

            templateExample.add("appLogo", appLogo);

            int currentYear = Year.now().getValue();
            templateExample.add("currentYear", currentYear);

            // Render
            final String render = templateExample.render();

            message.setFrom(from);
            message.setTo(to);
            message.setSubject(title);
            message.setText(render, true);

            if (cc != null && cc.length > 0)
                message.setCc(cc);
            if (bcc != null && bcc.length > 0)
                message.setBcc(bcc);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Async("asyncExec")
    public void sendMail(String from, String[] to, String[] cc, String[] bcc, EmailTemplate emailTemplate,  Map<String, Object> contentParameter, App app, String initBy, Long entryId) {

        if (emailTemplate != null && Integer.valueOf(1).equals(emailTemplate.getEnabled())) {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8"); //, "UTF-8"); // true = multipart

                String subject = Helper.compileTpl(emailTemplate.getSubject(),contentParameter);
                String content = Helper.compileTpl(emailTemplate.getContent(),contentParameter);

                if (useEmail){
                    from = Constant.LEAP_MAILER;
                    subject = "["+ app.getAppPath()+"] " + subject;
                }

                // Load the file
                final STGroup stGroup = new STGroupFile("/email.tpl.stg", '{', '}');
                stGroup.registerRenderer(Object.class, new StringRenderer());
                // Pick the correct template
                final ST templateExample = stGroup.getInstanceOf("emailTemplate");

                // Pass on values to use when rendering
                templateExample.add("content", content);
                templateExample.add("appName", app.getTitle());

                String appLogo = app.getLogo() == null ? IO_BASE_DOMAIN + "/" + UI_BASE_DOMAIN + "-72.png" : IO_BASE_DOMAIN + "/api/app/logo/" + app.getLogo();
                templateExample.add("appLogo", appLogo);

                int currentYear = Year.now().getValue();
                templateExample.add("currentYear", currentYear + "");

                // Render
                final String render = templateExample.render();


                to = ArrayUtils.removeElement(to,"anonymous");
                if (emailTemplate.isLog()) {
                    Notification n = new Notification();
                    n.setEmail(String.join(",",to)); // for now, save all to with single email
                    n.setTimestamp(new Date());
                    n.setAppId(app.getId());
                    n.setEmailTemplateId(emailTemplate.getId());
                    n.setSubject(subject);
                    n.setContent(content);
                    n.setSender(from);
                    n.setInitBy(initBy != null ? initBy : from);
                    n.setEntryId(entryId);
                    n.setStatus("new");
                    notificationService.save(n);
                }

                message.setFrom(from);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(render, true);

                if (cc != null && cc.length > 0)
                    message.setCc(cc);
                if (bcc != null && bcc.length > 0)
                    message.setBcc(bcc);
                mailSender.send(mimeMessage);

            } catch (AddressException e) {
                logger.warn("Invalid email address:" + Arrays.stream(to).toList()+", in string:"+e.getRef());
            } catch (Exception e) {
                logger.warn("Cannot send email. Invalid or incomplete parameters specified. Please make sure to supply all the parameters needed for the template you have chosen.");
                logger.warn("Exception message: "+ e.getMessage());
            }
        } else {
            logger.warn("Cannot send e-mail. Invalid Template Id specified");
        }
    }


}
