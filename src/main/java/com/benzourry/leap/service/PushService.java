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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(PushService.class);
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

    public Map<String, Object> send(Long userId, String title, String body, String url) {
        Map<String, Object> data = new HashMap<>();

        // 1. Safe Optional unwrapping
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        List<PushSub> pushSubs = pushSubRepository.findPushSubsByUser_Id(userId);

        // 2. Early return if no subscriptions exist
        if (pushSubs.isEmpty()) {
            data.put("success", true);
            data.put("result", Collections.emptyList());
            return data;
        }

        App app = appService.findById(user.getAppId());
        String appLogo = app.getLogo() == null
                ? IO_BASE_DOMAIN + "/" + UI_BASE_DOMAIN + "-72.png"
                : IO_BASE_DOMAIN + "/api/app/logo/" + app.getLogo();

        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        // 3. Build JSON safely using Maps to prevent breaking syntax with quotes/newlines
        Map<String, Object> notificationNode = new HashMap<>();
        notificationNode.put("body", body);
        notificationNode.put("icon", appLogo);
        notificationNode.put("title", app.getTitle() + ": " + title);

        if (!Helper.isNullOrEmpty(url)) {
            notificationNode.put("data", Map.of("url", url));
        }

        String json;
        try {
            json = MAPPER.writeValueAsString(Map.of("notification", notificationNode));
        } catch (Exception e) {
            logger.error("Failed to serialize push notification JSON", e);
            data.put("success", false);
            data.put("error", "Failed to build notification payload");
            return data;
        }

        List<String> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            // 4. Instantiate PushService exactly ONCE since the user email is the same for all subs
            nl.martijndwars.webpush.PushService pushService = new nl.martijndwars.webpush.PushService(PUBLIC_KEY, PRIVATE_KEY, "mailto:" + user.getEmail());

            for (PushSub pushSub : pushSubs) {
                try {
                    Notification notification = new Notification(
                            pushSub.getEndpoint(),
                            pushSub.getP256dh(),
                            pushSub.getAuth(),
                            json,
                            Urgency.HIGH
                    );

                    HttpResponse httpResponse = pushService.send(notification);
                    int statusCode = httpResponse.getStatusLine().getStatusCode();

                    if (statusCode == 201) {
                        results.add("Success : [" + statusCode + "] " + pushSub.getEndpoint());
                    } else {
                        String reason = httpResponse.getStatusLine().getReasonPhrase();
                        results.add("Failed : [" + statusCode + "] " + pushSub.getEndpoint() + " - " + reason);
                    }
                } catch (Exception e) {
                    errors.add(e.getMessage());
                    results.add("Failed : " + pushSub.getEndpoint());
                    logger.error("Error sending push to " + pushSub.getEndpoint(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to initialize PushService", e);
            data.put("success", false);
            data.put("error", "Failed to initialize PushService: " + e.getMessage());
            return data;
        }

        // 5. Cleaned up data payload compilation
        data.put("success", true);
        data.put("result", results);
        if (!errors.isEmpty()) {
            data.put("errors", errors); // Track errors safely without overwriting result
        }

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
            }
        }
    }

    @Async("asyncExec")
    public void sendMailPush(String from, String[] to, String[] cc, String[] bcc, EmailTemplate emailTemplate, Map<String, Object> parameter, App app) {

        if (emailTemplate != null) {
            try {

                //build subject
                String subject = Helper.compileTpl(emailTemplate.getSubject(), parameter);

                String content = Helper.compileTpl(emailTemplate.getContent(), parameter).replaceAll("\\<[^>]*>", " ");

                AtomicReference<String> renderedUrl = new AtomicReference<>();
                if (!Helper.isNullOrEmpty(emailTemplate.getPushUrl())) {
                    renderedUrl.set(Helper.compileTpl(emailTemplate.getPushUrl(), parameter));
                }

                for (String email : to) {
                    sendPushByEmail(email, app.getId(), subject, content, renderedUrl.get());
                }

            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Cannot push notification. Invalid or incomplete parameters specified. Please make sure to supply all the parameters needed for the template you have chosen.");
            }
        } else {
            logger.error("Cannot push notification. Invalid Template Id specified");
        }
    }

    public List<PushSub> getSubscriptions(Long userId) {
        return pushSubRepository.findPushSubsByUser_Id(userId);
    }
}
