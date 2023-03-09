package com.benzourry.leap.service;

import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.PushSubRepository;
import com.benzourry.leap.repository.UserRepository;
import com.benzourry.leap.utility.FieldRenderer;
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
import org.stringtemplate.v4.ST;
import ua_parser.Client;
import ua_parser.Parser;

import java.security.Security;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.benzourry.leap.config.Constant.IO_BASE_DOMAIN;
import static com.benzourry.leap.config.Constant.UI_BASE_DOMAIN;

@Service
public class PushService {

    private static final Logger logger = LoggerFactory.getLogger(PushMessage.class);


//    @Autowired
final UserRepository userRepository;

//    @Autowired
final AppService appService;

//    @Autowired
final PushSubRepository pushSubRepository;



    private static final String PUBLIC_KEY = "BIRiQCpjtaORtlvwZ7FzFkf8V799iGvEX1kQtO86y-BdiGpAMvXN4UDU1DWEqrpPEAiDDVilG8WKk62NjFc1Opo";
    private static final String PRIVATE_KEY = "XkSQje9W1BtdHTsGvMmVBCc8v1YbuelZxtonNTlZRAA";
//    private static final String SUBJECT = "Foobarbaz";
//    private static final String PAYLOAD = "My fancy message";

//    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();

    public PushService(UserRepository userRepository,
                       AppService appService,
                       PushSubRepository pushSubRepository){
        this.userRepository = userRepository;
        this.appService = appService;
        this.pushSubRepository = pushSubRepository;
    }


    public PushSub findByEndpoint(PushSub pushSub){
        return pushSubRepository.getReferenceById(pushSub.getEndpoint());
    }

    public PushSub subscribe(PushSub pushSub, Long userId) {

        ObjectMapper mapper = new ObjectMapper();

        Parser uaParser = new Parser();

        Client client = uaParser.parse(pushSub.getUserAgent());

            User user = userRepository.findById(userId).get();
//            pushSub.setActive(true);
            pushSub.setAppId(user.getAppId());
            pushSub.setUser(user);
            pushSub.setTimestamp(new Date());
            pushSub.setClient(mapper.valueToTree(client));

            return pushSubRepository.save(pushSub);
    }

//    public PushSub resubscribe(String endpoint) {
//
//        System.out.println("###resubscribe:endpoint="+endpoint);
//
//        PushSub pushSub = pushSubRepository.getReferenceById(endpoint);
//        System.out.println("###resubscribe:one="+pushSub.getEndpoint());
//        pushSub.setActive(true);
//        return pushSubRepository.save(pushSub);
////        if (pushSubRepository.existsById(pushSub.getEndpoint())){
////            return );
//////            push
////        }else{
////            User user = userRepository.findById(userId).get();
////
////            pushSub.setAppId(user.getAppId());
////            pushSub.setUser(user);
////            pushSub.setTimestamp(new Date());
////
////            return pushSubRepository.save(pushSub);
////        }
//    }


    public void unsubscribe(String endpoint) {
//        PushSub pushSub = pushSubRepository.getReferenceById(endpoint);
//
//        pushSub.setActive(false);

        pushSubRepository.deleteById(endpoint);

        // save Subscription to User Object
//        ObjectMapper mapper = new ObjectMapper();
//        User user = userRepository.findById(userId).get();
//
//        user.setPushSub(null);
//        userRepository.save(user);
//
//        Map<String, Object> data = new HashMap<>();
//        data.put("success", true);
//        return data;
    }

    public Map<String, Object> send(Long userId,
                                     String title,
                                     String body, String url) {
        User user = userRepository.findById(userId).get();

        List<PushSub> pushSubs = pushSubRepository.findPushSubsByUser_Id(userId);

        App app = appService.findById(user.getAppId());

        String appLogo = app.getLogo()==null?IO_BASE_DOMAIN + "/"+UI_BASE_DOMAIN+"-72.png":IO_BASE_DOMAIN + "/api/app/logo/"+app.getLogo();

        Map<String, Object> data = new HashMap<>();

        final List<String> results = new ArrayList<>();

        if (pushSubs.size()>0) {
            // add provider only if it's not in the JVM
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

//            String url = "https://"+app.getAppPath()+"."+UI_BASE_DOMAIN;

            pushSubs.forEach(pushSub->{

                String json = "{" +
                        "  \"notification\": {" +
//                "    \"badge\": USVString," +
                        "    \"body\": \""+body+"\"," +
                        (Helper.isNullOrEmpty(url)?"":"    \"data\": {\"url\":\""+url+"\"},") +
//                "    \"dir\": \"auto\"|\"ltr\"|\"rtl\"," +
                        "    \"icon\": \""+appLogo+"\"," +
//                "    \"image\": USVString," +
//                "    \"lang\": DOMString," +
//                "    \"renotify\": boolean," +
//                "    \"requireInteraction\": boolean," +
//                "    \"silent\": boolean," +
//                "    \"tag\": DOMString," +
//                "    \"timestamp\": DOMTimeStamp," +
                        "    \"title\": \""+app.getTitle()+": "+title+"\"" +
                        "  }" +
                        "}";

                try {
                    nl.martijndwars.webpush.PushService pushService = new nl.martijndwars.webpush.PushService(PUBLIC_KEY, PRIVATE_KEY, "mailto: "+ pushSub.getUser().getEmail());

                    Notification notification = new Notification(pushSub.getEndpoint(),pushSub.getP256dh(),pushSub.getAuth(),json, Urgency.HIGH);
                    HttpResponse httpResponse = pushService.send(notification);
                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    String statusReason = httpResponse.getStatusLine().getReasonPhrase();

//                    System.out.println("######### PUSH #########");
//                    System.out.println("Email:"+pushSub.getUser().getEmail());
//                    System.out.println("Title:"+title);
//                    System.out.println("Content:"+body);
//
//                    System.out.println("%%%% PUSH-STATUS:"+statusCode+ " - "+ pushSub.getEndpoint());
//                    System.out.println("%%%% PUSH-REASON:"+statusReason);

                    data.put("result", String.valueOf(statusCode)) ;

                    if (statusCode==201){
                        results.add("Success : ["+ statusCode +"] "+ pushSub.getEndpoint());
                    }
                } catch (Exception e) {
                    data.put("error", e.getMessage());
                    results.add("Failed :"+ pushSub.getEndpoint());
                    e.printStackTrace();
//                return e.getMessage();
                }
            });
        }

        data.put("success", true);
        data.put("result", results);
        return data;
    }

    public Map<String, Object> sendByEmail(String email, Long appId,
                                     String title,
                                     String body, String url) {
        User user = userRepository.findFirstByEmailAndAppId(email,appId).get();
//        System.out.println("UserId:" + user.getId());
        return send(user.getId(),title, body, url);
    }


    public void sendAll( Long appId,  String title,  String body, String url) {

        ObjectMapper mapper = new ObjectMapper();
        Security.addProvider(new BouncyCastleProvider());

        App app = appService.findById(appId);

//        String url = "https://"+app.getAppPath()+"."+UI_BASE_DOMAIN;

        String appLogo = app.getLogo()==null?IO_BASE_DOMAIN + "/"+UI_BASE_DOMAIN+"-72.png":IO_BASE_DOMAIN + "/api/app/logo/"+app.getLogo();

        List<PushSub> pushSubs = pushSubRepository.findPushSubsByAppId(appId);

        String json = "{" +
                "  \"notification\": {" +
//                "    \"badge\": USVString," +
                "    \"body\": \""+body+"\"," +
                (Helper.isNullOrEmpty(url)?"":"    \"data\": {\"url\":\""+url+"\"},") +
//                "    \"data\": any," +
//                "    \"dir\": \"auto\"|\"ltr\"|\"rtl\"," +
                "    \"icon\": \""+appLogo+"\"," +
//                "    \"image\": USVString," +
//                "    \"lang\": DOMString," +
//                "    \"renotify\": boolean," +
//                "    \"requireInteraction\": boolean," +
//                "    \"silent\": boolean," +
//                "    \"tag\": DOMString," +
//                "    \"timestamp\": DOMTimeStamp," +
                "    \"title\": \""+app.getTitle()+": "+title+"\"" +
                "  }" +
                "}";

        for (PushSub pushSub : pushSubs) {


            try {
                nl.martijndwars.webpush.PushService pushService = new nl.martijndwars.webpush.PushService(PUBLIC_KEY, PRIVATE_KEY, "mailto: "+ pushSub.getUser().getEmail());
//                Notification notification = new Notification(subscription, json);
                Notification notification = new Notification(pushSub.getEndpoint(),pushSub.getP256dh(),pushSub.getAuth(),json, Urgency.HIGH);


                HttpResponse httpResponse = pushService.send(notification);
                int statusCode = httpResponse.getStatusLine().getStatusCode();

//                System.out.println(String.valueOf(statusCode));
            } catch (Exception e) {
                e.printStackTrace();
//                ExceptionUtils.getStackTrace(e);
            }
        }
    }



    @Async
    public void sendMailPush(String from, String[] to, String[] cc, String[] bcc, EmailTemplate emailTemplate, Map<String, Object> subjectParameter, Map<String, Object> contentParameter, App app) {

        if (emailTemplate != null) {
            try {

                //build subject
                ST subject = new ST(MailService.rewriteTemplate(emailTemplate.getSubject()), '$', '$');
                for (Map.Entry<String, Object> entry : subjectParameter.entrySet()) {
                    subject.add(entry.getKey(), entry.getValue());
                }

//                System.out.println(MailService.rewriteTemplate(emailTemplate.getContent()));
                //build content
                ST content = new ST(MailService.rewriteTemplate(emailTemplate.getContent()), '$', '$');
                for (Map.Entry<String, Object> entry : contentParameter.entrySet()) {
                    content.add(entry.getKey(), entry.getValue());
                }

                AtomicReference<String> renderedUrl=new AtomicReference<>();
                if (!Helper.isNullOrEmpty(emailTemplate.getPushUrl())) {
                    ST url = new ST(MailService.rewriteTemplate(emailTemplate.getPushUrl()), '$', '$');
                    for (Map.Entry<String, Object> entry : contentParameter.entrySet()) {
                        url.add(entry.getKey(), entry.getValue());
                    }
                    renderedUrl.set(url.render());
                }

                content.groupThatCreatedThisInstance.registerRenderer(Object.class, new FieldRenderer());

                Arrays.stream(to).forEach(email->{
//                    System.out.println("######### PUSH #########");
//                    System.out.println("Email:"+email);
//                    System.out.println("Title:"+subject.render());
//                    System.out.println("Content:"+content.render().replaceAll("\\<[^>]*>"," "));
                    sendByEmail(email,app.getId(),subject.render(),content.render().replaceAll("\\<[^>]*>"," "),renderedUrl.get());
                });

            } catch (Exception e) {
                e.printStackTrace();
                logger.warn("Cannot push notification. Invalid or incomplete parameters specified. Please make sure to supply all the parameters needed for the template you have chosen.");
            }
        } else {
            logger.warn("Cannot push notification. Invalid Template Id specified");
        }
    }

    public List<PushSub> getSubscriptions(Long userId) {
        return pushSubRepository.findPushSubsByUser_Id(userId);
    }
}
