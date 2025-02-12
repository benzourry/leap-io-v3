package com.benzourry.leap.controller;

import com.benzourry.leap.exception.BadRequestException;
import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.App;
import com.benzourry.leap.model.AuthProvider;
import com.benzourry.leap.model.Notification;
import com.benzourry.leap.model.User;
import com.benzourry.leap.payload.*;
import com.benzourry.leap.repository.AppRepository;
import com.benzourry.leap.repository.UserRepository;
import com.benzourry.leap.security.CurrentUser;
import com.benzourry.leap.security.TokenProvider;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.service.MailService;
import com.benzourry.leap.config.Constant;
import com.benzourry.leap.service.NotificationService;
import com.benzourry.leap.utility.Helper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;

    private final UserRepository userRepository;

    private final AppRepository appRepository;

    private final PasswordEncoder passwordEncoder;

    private final TokenProvider tokenProvider;


    private NotificationService notificationService;

    private final MailService mailService;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          TokenProvider tokenProvider,
                          AppRepository appRepository,
                          NotificationService notificationService,
                          MailService mailService){
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.appRepository = appRepository;
        this.notificationService = notificationService;
        this.mailService = mailService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody @Valid LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail()+"\n"+loginRequest.getAppId(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = tokenProvider.createToken(authentication);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/remove")
    public ResponseEntity<?> removeUser(@CurrentUser UserPrincipal userPrincipal) {
        userRepository.deleteById(userPrincipal.getId());
//        Optional<User> userOptional = userRepository.findById(userPrincipal.getId());
        return ResponseEntity.ok(new ApiResponse(true, "User removed successfully"));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody @Valid SignUpRequest signUpRequest) {
        Optional<User> userOptional = userRepository.findFirstByEmailAndAppId(signUpRequest.getEmail(),signUpRequest.getAppId());
        User user = new User();

        if (userOptional.isPresent()){
            User userTmp = userOptional.get();
            if (!AuthProvider.undetermine.equals(userTmp.getProvider())){
                throw new BadRequestException("Email address already in use in AppId="+signUpRequest.getAppId());
            }else{
                user = userTmp;
            }
        }

        // Creating user's account
        user.setName(signUpRequest.getName());
        user.setEmail(signUpRequest.getEmail().trim());
        user.setPassword(signUpRequest.getPassword());
        user.setProvider(AuthProvider.local);
        user.setAppId(signUpRequest.getAppId());
        user.setFirstLogin(new Date());
        user.setLastLogin(new Date());

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User result = userRepository.save(user);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/user/me")
                .buildAndExpand(result.getId()).toUri();

        return ResponseEntity.created(location)
                .body(new ApiResponse(true, "User registered successfully"));
    }


    @PostMapping("/changepwd")
    public ResponseEntity<?> changePassword(@RequestBody @Valid ChangePasswordRequest cpRequest) {
        if(!userRepository.existsByEmailAndAppId(cpRequest.getEmail(), cpRequest.getAppId())) {
            throw new BadRequestException("User not exist!");
        }

        User user = userRepository.findFirstByEmailAndAppId(cpRequest.getEmail(), cpRequest.getAppId()).get();

        if (passwordEncoder.matches(cpRequest.getPassword(),user.getPassword())){
            if (cpRequest.getPasswordNew1().equals(cpRequest.getPasswordNew2())){
                user.setPassword(passwordEncoder.encode(cpRequest.getPasswordNew1()));
                userRepository.save(user);
            }else{
                throw new BadRequestException("New Password #2 different from New Password #1");
            }
        }else{
            throw new BadRequestException("Invalid current password");
        }

        return ResponseEntity.ok()
                .body(new ApiResponse(true, "Password changed successfully"));
    }




    @PostMapping("/resetpwd")
    public ResponseEntity<?> resetPassword(@RequestParam("email") String email,
                                           @RequestParam("appId") Long appId) {
        if(!userRepository.existsByEmailAndAppId(email,appId)) {
            throw new BadRequestException("User not exist!");
        }

        User user = userRepository.findFirstByEmailAndAppId(email,appId).get();

        App app = null;
        String appPath = "";
        String appMailer = "";
        if (appId!=null && appId!=-1){
            app = appRepository.findById(appId).orElseThrow(()-> new ResourceNotFoundException("App","id", appId));
            appPath = app.getAppPath()+".";
            appMailer = app.getAppPath()+"_";
        }

        String tempPassword = Helper.getAlphaNumericString(5);

        user.setPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        mailService.sendMail(appMailer + Constant.LEAP_MAILER,new String[]{email},null,null,"Password Reset for " + appPath + Constant.UI_BASE_DOMAIN,
                "Hi "+user.getName()+"<br/>Your temporary password is <strong>"+tempPassword+ "</strong>" +
                        "<br/>Please make sure to change your password after successfully login." +
                        "<br/><br/>If you think this password reset is performed without your knowledge, please inform to us at support-"+Constant.LEAP_MAILER+"" +
                        "<br/><br/>Regards", app);

        Notification n = new Notification();
        n.setEmail(email); // for now, save all to with single email
        n.setTimestamp(new Date());
        if (app!=null) {
            n.setAppId(app.getId());
        }
        n.setEmailTemplateId(null);
        n.setSubject("Password Reset for " + appPath + Constant.UI_BASE_DOMAIN);
        n.setContent("Hi "+user.getName()+"<br/>Your temporary password is <strong>"+tempPassword+ "</strong>" +
                "<br/>Please make sure to change your password after successfully login." +
                "<br/><br/>If you think this password reset is performed without your knowledge, please inform to us at support-"+Constant.LEAP_MAILER+"" +
                "<br/><br/>Regards");
        n.setSender(appMailer + Constant.LEAP_MAILER);
        n.setInitBy(email);
        n.setStatus("new");
        notificationService.save(n);

        System.out.println(n);




//        if (passwordEncoder.matches(cpRequest.getPassword(),user.getPassword())){
//            if (cpRequest.getPasswordNew1().equals(cpRequest.getPasswordNew2())){
//                user.setPassword(passwordEncoder.encode(cpRequest.getPasswordNew1()));
//                userRepository.save(user);
//            }else{
//                throw new BadRequestException("New Password #2 different from New Password #1");
//            }
//        }else{
//            throw new BadRequestException("Invalid current password");
//        }


        return ResponseEntity.ok()
                .body(new ApiResponse(true, "Password reset successfully. New temporary password has been sent to "+ email));
    }

}
