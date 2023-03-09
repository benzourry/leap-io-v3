/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.benzourry.leap.controller;

import com.benzourry.leap.mixin.MailerMixin;
import com.benzourry.leap.model.EmailTemplate;
import com.benzourry.leap.service.EmailTemplateService;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author MohdRazif
 */
@RestController
@RequestMapping({"api/mailer"})
//@CrossOrigin(allowCredentials="true")
public class MailerController {
    private final EmailTemplateService emailTemplateService;


    public MailerController(EmailTemplateService emailTemplateService){
        this.emailTemplateService = emailTemplateService;
    }


//
//    @Autowired
//    public void setEmailTemplateService(EmailTemplateService emailTemplateService) {
//        this.emailTemplateService = emailTemplateService;
//    }

//    @Autowired
//    private SentMailService sentMailService;
    
//    @GetMapping
//    @JsonResponse(mixins = {
//            @JsonMixin(target = EmailTemplate.class, mixin = MailerMixin.MailerBasicList.class)
//    })
//    public Page<EmailTemplate> listEmailTemplate(@RequestParam(value = "searchText", defaultValue = "") String searchText,
//                                                 @RequestParam(value = "appId", required = false) String email,
//                                                 Pageable pageable) throws Exception{
//        return emailTemplateService.getEmailTemplateList(email, searchText, pageable);
//    }

    @GetMapping
    @JsonResponse(mixins = {
            @JsonMixin(target = EmailTemplate.class, mixin = MailerMixin.MailerBasicList.class)
    })
    public Page<EmailTemplate> listEmailTemplate(@RequestParam(value = "searchText", defaultValue = "") String searchText,
                                                 @RequestParam(value = "appId", required = false) Long appId,
                                                 Pageable pageable) {
        return emailTemplateService.findByAppId(appId, searchText, pageable);
    }

    @GetMapping("pickable")
//    @JsonResponse(mixins = {
//            @JsonMixin(target = EmailTemplate.class, mixin = MailerMixin.MailerBasicList.class)
//    })
    public Page<EmailTemplate> listPickableEmailTemplate(@RequestParam(value = "searchText", defaultValue = "") String searchText,
                                                 @RequestParam(value = "appId", required = false) Long appId,
                                                 Pageable pageable) {
        return emailTemplateService.findPickableByAppId(appId, searchText, pageable);
    }

    @PostMapping
    public EmailTemplate createEmailTemplate(@RequestBody EmailTemplate imObj,
                                             @RequestParam Long appId,
                                             @RequestParam String email) {
        return emailTemplateService.create(imObj, appId, email);
    }

    @GetMapping("{id}")
    public EmailTemplate viewEmailTemplate(@PathVariable Long id) {
        return emailTemplateService.getEmailTemplate(id);
    }


    /**
     * This url method will delete the Research entry based on the id in the path variable
     * DELETE /api/research/{id}
     * @param id
     */
    @PostMapping("{id}/delete")
    public Map<String,Object> delete(@PathVariable("id") Long id) {
        Map<String, Object> data = new HashMap<>();
        emailTemplateService.delete(id);
        data.put("success", "true");
        return data;
    }
//
//    @RequestMapping(value="email-sent", method = RequestMethod.GET)
//    public Page<SentMail> findByQuery(@RequestParam(value = "search", required = false, defaultValue = "") String search,
//                                      @RequestParam(value = "templateCode", required = false) List<String> templateCode,
//                                      @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso= DateTimeFormat.ISO.DATE_TIME) Date fromDate,
//                                      @RequestParam(value ="toDate", required = false) @DateTimeFormat(iso= DateTimeFormat.ISO.DATE_TIME) Date toDate,
//                                      Pageable pageable){
//        return sentMailService.findByQuery(search, templateCode,fromDate, toDate, pageable);
//    }
//
//    @RequestMapping(value="email-sent/{id}", method = RequestMethod.GET)
//    public SentMail findSentMail(@PathVariable("id") Long id){
//        return sentMailService.findOne(id);
//    }
//

}
