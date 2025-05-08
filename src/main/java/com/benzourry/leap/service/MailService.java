/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.benzourry.leap.service;

import com.benzourry.leap.config.Constant;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.AppUserRepository;
import com.benzourry.leap.repository.UserRepository;
import com.benzourry.leap.utility.FieldRenderer;
import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.MimeMessage;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

//    private final PushService pushService;

//    private SentMailService sentMailService;

    //    @Autowired
    public MailService(JavaMailSender mailSender,
                       NotificationService notificationService,
                       EmailTemplateService emailTemplateService,
                       UserRepository userRepository,
                       AppUserRepository appUserRepository) {
        this.mailSender = mailSender;
        this.notificationService = notificationService;
        this.emailTemplateService = emailTemplateService;
        this.userRepository = userRepository;
        this.appUserRepository = appUserRepository;
//        this.pushService = pushService;
//        this.sentMailService = sentMailService;

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
//                    logger.info("template != null");
                Map<String, Object> contentMap = new HashMap<>();
//                Map<String, Object> subjectMap = new HashMap<>();
                ObjectMapper mapper = new ObjectMapper();
                contentMap.put("_", mapper.convertValue(entry, Map.class));
//                subjectMap.put("_", mapper.convertValue(entry, Map.class));
                Map<String, Object> result = mapper.convertValue(entry.getData(), Map.class);
                Map<String, Object> prev = mapper.convertValue(entry.getPrev(), Map.class);


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
//                subjectMap.put("data", result);

                contentMap.put("prev", prev);
//                subjectMap.put("prev", prev);

                Optional<User> u = userRepository.findFirstByEmailAndAppId(entry.getEmail(), entry.getForm().getApp().getId());
                if (u.isPresent()) {
                    Map userMap = mapper.convertValue(u.get(), Map.class);
                    contentMap.put("user", userMap);
                }

//                if (gat != null) {
////                        gat = entry.getForm().getTiers().get(entry.getCurrentTier());
//                    contentMap.put("tier", gat);
//                    subjectMap.put("tier", gat);
//                }

//                if (entry.getApproval() != null && gat != null) {
//                    EntryApproval approval_ = entry.getApproval().get(gat.getId());
//                    if (approval_ != null) {
//                        Map<String, Object> approval = mapper.convertValue(approval_.getData(), Map.class);
//                        subjectMap.put("approval_", approval_);
//                        contentMap.put("approval_", approval_);
//                        subjectMap.put("approval", approval);
//                        contentMap.put("approval", approval);
//                    }
//                }

                List<String> recipients = new ArrayList<>();// Arrays.asList(entry.getEmail());
                if (template.isToUser()) {
                    recipients.add(entry.getEmail());
                }
                if (template.isToAdmin()) {
                    if (entry.getForm().getAdmin() != null) {
                        List<String> adminEmails = appUserRepository.findByGroupId(entry.getForm().getAdmin().getId(), Pageable.unpaged())
                                .getContent().stream().map(appUser -> appUser.getUser().getEmail()).toList();
                        if (!adminEmails.isEmpty()) {
                            recipients.addAll(adminEmails);
                        }
                    }
                }
//                if (gat != null && template.isToApprover()) {
//                    if (!entry.getApprover().isEmpty() && entry.getApprover().get(gat.getId()) != null) {
//                        recipients.addAll(Arrays.asList(entry.getApprover().get(gat.getId()).replaceAll(" ", "").split(",")));
//                    }
//                }
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
                                .getContent().stream()
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

//                if (template.isPushable()) {
//                    pushService.sendMailPush(entry.getForm().getApp().getAppPath() + "_" + Constant.LEAP_MAILER, rec, recCc, null, template, subjectMap, contentMap, entry.getForm().getApp());
//                }

                sendMail(entry.getForm().getApp().getAppPath() + "_" + Constant.LEAP_MAILER, rec, recCc, null, template, contentMap, entry.getForm().getApp(), initBy, entry.getId());
            } else {
//                    logger.info("template == null");
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

            message.setFrom(from);
            message.setTo(to);
            message.setSubject(title);
            message.setText(render, true);
//            message.setCc(cc);
//            message.setBcc(bcc);

            if (cc != null && cc.length > 0)
                message.setCc(cc);
            if (bcc != null && bcc.length > 0)
                message.setBcc(bcc);


            mailSender.send(mimeMessage);
        } catch (Exception e) {
            System.out.println("Email sending error:"+ e.getMessage());
//            e.printStackTrace();
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
//            message.setCc(cc);
//            message.setBcc(bcc);

            if (cc != null && cc.length > 0)
                message.setCc(cc);
            if (bcc != null && bcc.length > 0)
                message.setBcc(bcc);


            mailSender.send(mimeMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /*
    Send massive emails to designated recipient with customized subject and content for each.
    List<Map> items : Map being to, subject, content
    */
//    public void sendMail(String from, List<Map> items, EmailTemplate emailTemplate) throws MessagingException {
//        MimeMessage[] messages = new MimeMessage[items.size()];
//
//        for (int i = 0; i < items.size(); i++) {
//            messages[i] = mailSender.createMimeMessage();
//            MimeMessageHelper message = new MimeMessageHelper(messages[i], true, "UTF-8");
//            Map<String, Object> item = items.get(i);
//
//            String to = item.get("to")+"";
//
//
//            Map<String, Object> subjectParameter = (Map<String, Object>)item.get("subjectParameter");
//
//            //build subject
//            ST subject = new ST(emailTemplate.getSubject(), '$', '$');
//            for (Map.Entry<String, Object> entry : subjectParameter.entrySet()) {
//                subject.add(entry.getKey(), entry.getValue());
//            }
//
//            Map<String, Object> contentParameter = (Map<String, Object>)item.get("contentParameter");
//
//            //build content
//            ST content = new ST(emailTemplate.getContent(), '$', '$');
//            for (Map.Entry<String, Object> entry : contentParameter.entrySet()) {
//                content.add(entry.getKey(), entry.getValue());
//            }
//
//
//            // Load the file
//            final STGroup stGroup = new STGroupFile("/email.tpl.stg",'$','$');
//
//            // Pick the correct template
//            final ST templateExample = stGroup.getInstanceOf("emailTemplate");
//
//            // Pass on values to use when rendering
//            templateExample.add("content", content.render());
//
//            // Render
//            final String render = templateExample.render();
//
//            message.setFrom(from);
//            message.setTo(to);
//            message.setSubject(subject.render());
//            message.setText(render, true);
//
//
////            try {
////                SentMail sentMail = new SentMail().builder()
////                        .emailTo(Optional.ofNullable(item.get("to")).get().toString())
////                        .emailFrom(from)
////                        .subject(subject.render())
////                        .content(content.render())
////                        .timestamp(new Date())
////                        .templateCode(emailTemplate.getCode())
////                        .build();
////
////                sentMailService.save(sentMail);
////            }catch(Exception e){
////                e.printStackTrace();
////            }
//
////            System.out.println("to:" + messages[i].getTo()[0] + ", subject:" + messages[i].getSubject() + ", content:" + messages[i].getText());
//        }
//
//        mailSender.send(messages);
//    }

    @Async("asyncExec")
    public void sendMail(String from, String[] to, String[] cc, String[] bcc, EmailTemplate emailTemplate,  Map<String, Object> contentParameter, App app, String initBy, Long entryId) {

        if (emailTemplate != null && Integer.valueOf(1).equals(emailTemplate.getEnabled())) {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8"); //, "UTF-8"); // true = multipart

                String subject = Helper.compileTpl(emailTemplate.getSubject(),contentParameter);
                String content = Helper.compileTpl(emailTemplate.getContent(),contentParameter);

                //build subject
//                ST subject = new ST(rewriteTemplate(emailTemplate.getSubject()), '$', '$');
//                ST content = new ST(rewriteTemplate(emailTemplate.getContent()), '$', '$');
//                for (Map.Entry<String, Object> entry : contentParameter.entrySet()) {
//                    subject.add(entry.getKey(), entry.getValue());
//                    content.add(entry.getKey(), entry.getValue());
//                }

//                System.out.println(rewriteTemplate(emailTemplate.getContent()));
                //build content
//                for (Map.Entry<String, Object> entry : contentParameter.entrySet()) {
//                }

//                subject.groupThatCreatedThisInstance.registerRenderer(Object.class, new FieldRenderer());
//                content.groupThatCreatedThisInstance.registerRenderer(Object.class, new FieldRenderer());

                System.out.println("Content rendered:"+content);
//                System.out.println("###full to:"+ String.join(",",to));

//                ClassPathResource classPathResource = new ClassPathResource("/email.tpl.stg");
                // Load the file
                final STGroup stGroup = new STGroupFile("/email.tpl.stg", '{', '}');
//                final STGroup stGroup = new STGroupFile("/email.tpl.stg",'$','$');
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
//                to = ArrayUtils.removeElement(to,"anonymous");
//                System.out.println("###full email:"+render);
//                System.out.println("###full subject:"+subject.render());
                if (emailTemplate.isLog()) {
//                    List<Notification> nList = new ArrayList<>();
//                    Arrays.stream(to).forEach(email -> {
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
//                        nList.add(n);
//                    });
//                    notificationService.saveAll(nList);
                    notificationService.save(n);
                }
//                System.out.println("OKOKOK");


                message.setFrom(from);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(render, true);
//                System.out.println(render);
                if (cc != null && cc.length > 0)
                    message.setCc(cc);
                if (bcc != null && bcc.length > 0)
                    message.setBcc(bcc);
                mailSender.send(mimeMessage);


                // Add email history
//                try {
//                    SentMail sentMail = new SentMail().builder()
//                            .emailTo(String.join(", ",Optional.ofNullable(to).orElse(new String[]{""})))
//                            .emailFrom(from)
//                            .emailCc(String.join(", ",Optional.ofNullable(cc).orElse(new String[]{""})))
//                            .subject(subject.render())
//                            .content(content.render())
//                            .timestamp(new Date())
//                            .templateCode(emailTemplate.getCode())
//                            .build();
//
//                    sentMailService.save(sentMail);
//                }catch(Exception e){
//                    e.printStackTrace();
//                }

            } catch (AddressException e) {
                logger.warn("Invalid email address:" + Arrays.stream(to).toList()+", in string:"+e.getRef());
            } catch (Exception e) {
//                e.printStackTrace();
                logger.warn("Cannot send email. Invalid or incomplete parameters specified. Please make sure to supply all the parameters needed for the template you have chosen.");
                logger.warn("Exception message: "+ e.getMessage());
            }
        } else {
//            System.out.println("template null");
            logger.warn("Cannot send e-mail. Invalid Template Id specified");
        }
        // } else {
        //    System.out.println("###parameters not specified");
        //    logger.warn("Cannot send e-mail. Template parameters are not specified");
        // }
    }

//    public static String compileTpl(String text, Map<String, Object> obj) {
//        ST content = new ST(rewriteTemplate(text), '$', '$');
//        for (Map.Entry<String, Object> entry : obj.entrySet()) {
//            content.add(entry.getKey(), entry.getValue());
//        }
//        content.groupThatCreatedThisInstance.registerRenderer(Object.class, new FieldRenderer());
//        return content.render();
//    }
//
//    public static String rewriteTemplate(String str) {
//        if (str != null) {
//            str = str.replace("$$_", "approval_");
//            str = str.replace("$$", "approval");
//            str = str.replace("$uiUri$", "uiUri");
//            str = str.replace("$approval$", "approval");
//            str = str.replace("$viewUri$", "viewUri");
//            str = str.replace("$editUri$", "editUri");
//            str = str.replace("$tier$", "tier");
//            str = str.replace("$prev$.$code", "prev_code");
//            str = str.replace("$prev$.$id", "prev_id");
//            str = str.replace("$prev$.$counter", "prev_counter");
//            str = str.replace("$conf$", "conf"); // just to allow presetFilter with $conf$ dont throw error because of succcessive replace of '$'. Normally it will become $$confdata.category$
//            str = str.replace("$prev$", "prev");
//            str = str.replace("$user$", "user");
//            str = str.replace("$_", "_");
//            str = str.replace("$.$code", "code");
//            str = str.replace("$.$id", "id");
//            str = str.replace("$.$counter", "counter");
//            str = str.replace("$.", "data.");
//            str = str.replace("{{", "$");
//            str = str.replace("}}", "$");
//
//        }
//        System.out.println(str);
//        return str;
//    }


}
