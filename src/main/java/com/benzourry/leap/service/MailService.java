/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.benzourry.leap.service;

import com.benzourry.leap.model.App;
import com.benzourry.leap.model.EmailTemplate;
import com.benzourry.leap.model.Lambda;
import com.benzourry.leap.model.Notification;
import com.benzourry.leap.utility.FieldRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import org.stringtemplate.v4.StringRenderer;

import jakarta.mail.internet.MimeMessage;
import java.time.Year;
import java.util.*;

import static com.benzourry.leap.config.Constant.*;


@Service("mailService")
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;

    private final NotificationService notificationService;

//    private SentMailService sentMailService;

//    @Autowired
    public MailService(JavaMailSender mailSender, NotificationService notificationService){
        this.mailSender = mailSender;
        this.notificationService = notificationService;
//        this.sentMailService = sentMailService;

    }

    /** FOR LAMBDA USAGE **/
    public void send(Map<String,String> params, Lambda lambda){
     //   System.out.println(params);
        String from = Optional.ofNullable(params.get("from")).orElse(lambda.getApp().getAppPath()+ "_" +LEAP_MAILER);
        String [] to = Optional.ofNullable(params.get("to")).orElse("").split(",");
        String[] cc=null;
        if (params.get("cc")!=null) {
            cc = params.get("cc").split(",");
        }
        String subject = params.get("subject");
        String content = params.get("content");
        this.sendMailApp(from,to,cc,null,subject,content, lambda.getApp());
    }

    /** FOR LAMBDA USAGE **/
//    public void sendWithTemplate(Map<String,String> params){
//        System.out.println(params);
//        String from = Optional.ofNullable(params.get("from")).orElse("auto-"+LEAP_MAILER);
//        String [] to = Optional.ofNullable(params.get("to")).orElse("").split(",");
//        String[] cc=null;
//        if (params.get("cc")!=null) {
//            cc = params.get("cc").split(",");
//        }
//        String subject = params.get("subject");
//        String content = params.get("content");
//        this.sendMail(from,to,cc,null,subject,content);
//    }


    public void sendMailApp(String from, String[] to, String[] cc, String[] bcc, String title, String content, App app) {

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // Load the file
            final STGroup stGroup = new STGroupFile("/email.tpl.stg",'$','$');

            // Pick the correct template
            final ST templateExample = stGroup.getInstanceOf("emailTemplate");

            // Pass on values to use when rendering
            templateExample.add("content", content);

            templateExample.add("appName", app.getTitle());

            String appLogo = app.getLogo()==null?IO_BASE_DOMAIN + "/"+UI_BASE_DOMAIN+"-72.png":IO_BASE_DOMAIN + "/api/app/logo/"+app.getLogo();
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


    public void sendMail(String from, String[] to, String[] cc, String[] bcc, String title, String content) {

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // Load the file
            final STGroup stGroup = new STGroupFile("/email.tpl.stg",'$','$');

            // Pick the correct template
            final ST templateExample = stGroup.getInstanceOf("emailTemplate");

            // Pass on values to use when rendering
            templateExample.add("content", content);

            String appLogo = IO_BASE_DOMAIN +"/"+UI_BASE_DOMAIN+"-72.png";
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
    public void sendMail(String from, String[] to, String[] cc, String[] bcc, EmailTemplate emailTemplate, Map<String, Object> subjectParameter, Map<String, Object> contentParameter, App app) {

        // if (!subjectParameter.isEmpty() && !contentParameter.isEmpty()) {
        //System.out.println();
        //   EmailTemplate et = emailTemplateDAO.findById(emailTemplateId);
        if (emailTemplate != null) {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8"); //, "UTF-8"); // true = multipart


                //build subject
                ST subject = new ST(rewriteTemplate(emailTemplate.getSubject()), '$', '$');
                for (Map.Entry<String, Object> entry : subjectParameter.entrySet()) {
                    subject.add(entry.getKey(), entry.getValue());
                }

//                System.out.println(rewriteTemplate(emailTemplate.getContent()));
                //build content
                ST content = new ST(rewriteTemplate(emailTemplate.getContent()), '$', '$');
                for (Map.Entry<String, Object> entry : contentParameter.entrySet()) {
                    content.add(entry.getKey(), entry.getValue());
                }

                content.groupThatCreatedThisInstance.registerRenderer(Object.class, new FieldRenderer());

//                System.out.println("Content rendered:"+content.render());
//                System.out.println("###full to:"+ String.join(",",to));

//                ClassPathResource classPathResource = new ClassPathResource("/email.tpl.stg");
                // Load the file
                final STGroup stGroup = new STGroupFile("/email.tpl.stg",'$','$');
//                final STGroup stGroup = new STGroupFile("/email.tpl.stg",'$','$');
                stGroup.registerRenderer(Object.class, new StringRenderer());
                // Pick the correct template
                final ST templateExample = stGroup.getInstanceOf("emailTemplate");

                // Pass on values to use when rendering
                templateExample.add("content", content.render());
                templateExample.add("appName", app.getTitle());

                String appLogo = app.getLogo()==null?IO_BASE_DOMAIN + "/"+UI_BASE_DOMAIN+"-72.png":IO_BASE_DOMAIN + "/api/app/logo/"+app.getLogo();
                templateExample.add("appLogo", appLogo);

                int currentYear = Year.now().getValue();
                templateExample.add("currentYear", currentYear);

                // Render
                final String render = templateExample.render();

//                System.out.println("###full email:"+render);
//                System.out.println("###full subject:"+subject.render());
                List<Notification> nList = new ArrayList<>();
                Arrays.stream(to).forEach(email->{
                    Notification n = new Notification();
                    n.setEmail(email);
                    n.setTimestamp(new Date());
                    n.setAppId(app.getId());
                    n.setContent(content.render());
                    n.setSender(from);
                    n.setStatus("new");
                    nList.add(n);
                });
                notificationService.saveAll(nList);
//                System.out.println("OKOKOK");


                message.setFrom(from);
                message.setTo(to);
                message.setSubject(subject.render());
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

            } catch (Exception e) {
                e.printStackTrace();
                logger.warn("Cannot send email. Invalid or incomplete parameters specified. Please make sure to supply all the parameters needed for the template you have chosen.");
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

    public static String compileTpl(String text, Map<String, Object> obj){
        ST content = new ST(rewriteTemplate(text), '$', '$');
        for (Map.Entry<String, Object> entry : obj.entrySet()) {
            content.add(entry.getKey(), entry.getValue());
        }
        content.groupThatCreatedThisInstance.registerRenderer(Object.class, new FieldRenderer());
        return content.render();
    }

    public static String rewriteTemplate(String str){
        if (str!=null){
            str = str.replace("$$_","approval_");
            str = str.replace("$$","approval");
            str = str.replace("$uiUri$","uiUri");
            str = str.replace("$approval$","approval");
            str = str.replace("$viewUri$","viewUri");
            str = str.replace("$editUri$","editUri");
            str = str.replace("$tier$","tier");
            str = str.replace("$prev$","prev");
            str = str.replace("$user$","user");
            str = str.replace("$_","_");
            str = str.replace("$.$code","code");
            str = str.replace("$.$id","id");
            str = str.replace("$.$counter","counter");
            str = str.replace("$prev$.$code","prev_code");
            str = str.replace("$prev$.$id","prev_id");
            str = str.replace("$prev$.$counter","prev_counter");
            str = str.replace("$.","data.");
            str = str.replace("{{","$");
            str = str.replace("}}","$");

        }
        return str;
    }



}
