package com.benzourry.leap.controller;//package com.benzourry.reka.controller;
//
//import com.benzourry.reka.FcmClient;
//import com.benzourry.reka.model.Subscription;
//import com.benzourry.reka.repository.SubscriptionRepository;
//import org.springframework.http.HttpStatus;
//import org.springframework.web.bind.annotation.*;
////import reactor.core.publisher.Mono;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.ExecutionException;
//
//@RestController
//@CrossOrigin
//public class RegistryController {
//
//    private final FcmClient fcmClient;
//
//    private SubscriptionRepository subscriptionRepository;
//
//    public RegistryController(FcmClient fcmClient,
//                              SubscriptionRepository subscriptionRepository) {
//        this.fcmClient = fcmClient;
//        this.subscriptionRepository = subscriptionRepository;
//    }
//
////    @PostMapping("/register")
////    @ResponseStatus(HttpStatus.NO_CONTENT)
////    public Mono<Void> register(@RequestBody Mono<String> token) {
////        return token.doOnNext(t -> this.fcmClient.subscribe("chuck", t)).then();
////    }
//
//    @PostMapping("api/push")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public Subscription sub(@RequestBody Subscription subs) {
//        return subscriptionRepository.save(subs);
//    }
//
//    @GetMapping("api/public/testno")
//    public void testno() throws ExecutionException, InterruptedException {
//        Map<String,String> data = new HashMap<>();
//
//        fcmClient.send(data);
//    }
//
//}