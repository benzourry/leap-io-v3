package com.benzourry.leap.service;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.App;
import com.benzourry.leap.model.EmailTemplate;
import com.benzourry.leap.model.PushSub;
import com.benzourry.leap.model.User;
import com.benzourry.leap.repository.PushSubRepository;
import com.benzourry.leap.repository.UserRepository;
import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.Urgency;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ua_parser.Client;
import ua_parser.Parser;

import java.security.Security;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.benzourry.leap.config.Constant.IO_BASE_DOMAIN;
import static com.benzourry.leap.config.Constant.UI_BASE_DOMAIN;

@Service
public class PushService {

    final UserRepository userRepository;
    final AppService appService;
    final PushSubRepository pushSubRepository;
    private static final String PUBLIC_KEY = "BIRiQCpjtaORtlvwZ7FzFkf8V799iGvEX1kQtO86y-BdiGpAMvXN4UDU1DWEqrpPEAiDDVilG8WKk62NjFc1Opo";
    private static final String PRIVATE_KEY = "XkSQje9W1BtdHTsGvMmVBCc8v1YbuelZxtonNTlZRAA";
    private final ObjectMapper MAPPER;

    public PushService(UserRepository userRepository,
                       AppService appService,
                       PushSubRepository pushSubRepository, ObjectMapper MAPPER) {
        this.userRepository = userRepository;
        this.appService = appService;
        this.pushSubRepository = pushSubRepository;
        this.MAPPER = MAPPER;
    }


    public PushSub findByEndpoint(PushSub pushSub) {
        return pushSubRepository.findById(pushSub.getEndpoint())
                .orElseThrow(() -> new ResourceNotFoundException("PushSub", "id", pushSub.getEndpoint()));
    }

    public PushSub subscribe(PushSub pushSub, Long userId) {

        Parser uaParser = new Parser();

        Client client = uaParser.parse(pushSub.getUserAgent());

        User user = userRepository.findById(userId).get();
        pushSub.setAppId(user.getAppId());
        pushSub.setUser(user);
        pushSub.setTimestamp(new Date());
        pushSub.setClient(MAPPER.valueToTree(client));

        return pushSubRepository.save(pushSub);
    }

    public void unsubscribe(String endpoint) {
        pushSubRepository.deleteById(endpoint);
    }

    public Map<String, Object> send(Long userId,
                                    String title,
                                    String body, String url) {
        User user = userRepository.findById(userId).get();

        List<PushSub> pushSubs = pushSubRepository.findPushSubsByUser_Id(userId);

        App app = appService.findById(user.getAppId());

        String appLogo = app.getLogo() == null ? IO_BASE_DOMAIN + "/" + UI_BASE_DOMAIN + "-72.png" : IO_BASE_DOMAIN + "/api/app/logo/" + app.getLogo();

        Map<String, Object> data = new HashMap<>();

        final List<String> results = new ArrayList<>();

        if (pushSubs.size() > 0) {
            // add provider only if it's not in the JVM
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

//            String url = "https://"+app.getAppPath()+"."+UI_BASE_DOMAIN;

            pushSubs.forEach(pushSub -> {

                String json = "{" +
                        "  \"notification\": {" +
//                "    \"badge\": USVString," +
                        "    \"body\": \"" + body + "\"," +
                        (Helper.isNullOrEmpty(url) ? "" : "    \"data\": {\"url\":\"" + url + "\"},") +
//                "    \"dir\": \"auto\"|\"ltr\"|\"rtl\"," +
                        "    \"icon\": \"" + appLogo + "\"," +
//                "    \"image\": USVString," +
//                "    \"lang\": DOMString," +
//                "    \"renotify\": boolean," +
//                "    \"requireInteraction\": boolean," +
//                "    \"silent\": boolean," +
//                "    \"tag\": DOMString," +
//                "    \"timestamp\": DOMTimeStamp," +
                        "    \"title\": \"" + app.getTitle() + ": " + title + "\"" +
                        "  }" +
                        "}";

                try {
                    nl.martijndwars.webpush.PushService pushService = new nl.martijndwars.webpush.PushService(PUBLIC_KEY, PRIVATE_KEY, "mailto: " + pushSub.getUser().getEmail());

                    Notification notification = new Notification(pushSub.getEndpoint(), pushSub.getP256dh(), pushSub.getAuth(), json, Urgency.HIGH);
                    HttpResponse httpResponse = pushService.send(notification);
                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    String statusReason = httpResponse.getStatusLine().getReasonPhrase();

                    data.put("result", String.valueOf(statusCode));

                    if (statusCode == 201) {
                        results.add("Success : [" + statusCode + "] " + pushSub.getEndpoint());
                    }
                } catch (Exception e) {
                    data.put("error", e.getMessage());
                    results.add("Failed :" + pushSub.getEndpoint());
                    e.printStackTrace();
//                return e.getMessage();
                }
            });
        }

        data.put("success", true);
        data.put("result", results);
        return data;
    }

    public Map<String, Object> sendPushByEmail(String email, Long appId,
                                               String title,
                                               String body, String url) {
        User user = userRepository.findFirstByEmailAndAppId(email, appId).get();
        return send(user.getId(), title, body, url);
    }


    public void sendAll(Long appId, String title, String body, String url) {

        Security.addProvider(new BouncyCastleProvider());

        App app = appService.findById(appId);

//        String url = "https://"+app.getAppPath()+"."+UI_BASE_DOMAIN;

        String appLogo = app.getLogo() == null ? IO_BASE_DOMAIN + "/" + UI_BASE_DOMAIN + "-72.png" : IO_BASE_DOMAIN + "/api/app/logo/" + app.getLogo();

        List<PushSub> pushSubs = pushSubRepository.findPushSubsByAppId(appId);

        String json = """
                {
                   "notification": {
                      "body": "$body",
                      $addData
                      "icon": "$appLogo",
                      "title": "$title"
                   }
                }
                """
                .replace("$body", body)
                .replace("$addData", (Helper.isNullOrEmpty(url) ? "" : "\"data\": {\"url\":\"" + url + "\"},"))
                .replace("$appLogo", appLogo)
                .replace("$title", app.getTitle() + ": " + title);


        for (PushSub pushSub : pushSubs) {
            try {
                nl.martijndwars.webpush.PushService pushService = new nl.martijndwars.webpush.PushService(PUBLIC_KEY, PRIVATE_KEY, "mailto: " + pushSub.getUser().getEmail());
                Notification notification = new Notification(pushSub.getEndpoint(), pushSub.getP256dh(), pushSub.getAuth(), json, Urgency.HIGH);


                HttpResponse httpResponse = pushService.send(notification);
                int statusCode = httpResponse.getStatusLine().getStatusCode();

            } catch (Exception e) {
                e.printStackTrace();
//                ExceptionUtils.getStackTrace(e);
            }
        }
    }


    @Async("asyncExec")
    public void sendMailPush(String from, String[] to, String[] cc, String[] bcc, EmailTemplate emailTemplate, Map<String, Object> parameter, App app) {

        if (emailTemplate != null) {
            try {

                //build subject
//                ST subject = new ST(MailService.rewriteTemplate(emailTemplate.getSubject()), '$', '$');
//                ST content = new ST(MailService.rewriteTemplate(emailTemplate.getContent()), '$', '$');
//                for (Map.Entry<String, Object> entry : contentParameter.entrySet()) {
//                    subject.add(entry.getKey(), entry.getValue());
//                    content.add(entry.getKey(), entry.getValue());
//                }

                String subject = Helper.compileTpl(emailTemplate.getSubject(), parameter);

                String content = Helper.compileTpl(emailTemplate.getContent(), parameter).replaceAll("\\<[^>]*>", " ");

                AtomicReference<String> renderedUrl = new AtomicReference<>();
                if (!Helper.isNullOrEmpty(emailTemplate.getPushUrl())) {
//                    ST url = new ST(MailService.rewriteTemplate(emailTemplate.getPushUrl()), '$', '$');
//                    for (Map.Entry<String, Object> entry : contentParameter.entrySet()) {
//                        url.add(entry.getKey(), entry.getValue());
//                    }
                    renderedUrl.set(Helper.compileTpl(emailTemplate.getPushUrl(), parameter));
                }

//                subject.groupThatCreatedThisInstance.registerRenderer(Object.class, new FieldRenderer());
//                content.groupThatCreatedThisInstance.registerRenderer(Object.class, new FieldRenderer());

                Arrays.stream(to).forEach(email -> {
                    sendPushByEmail(email, app.getId(), subject, content, renderedUrl.get());
                });

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Cannot push notification. Invalid or incomplete parameters specified. Please make sure to supply all the parameters needed for the template you have chosen.");
            }
        } else {
            System.out.println("Cannot push notification. Invalid Template Id specified");
        }
    }

    public List<PushSub> getSubscriptions(Long userId) {
        return pushSubRepository.findPushSubsByUser_Id(userId);
    }
}
