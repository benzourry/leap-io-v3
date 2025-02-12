/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.benzourry.leap.service;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.App;
import com.benzourry.leap.model.EmailTemplate;
import com.benzourry.leap.repository.AppRepository;
import com.benzourry.leap.repository.EmailTemplateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author MohdRazif
 */
@Service("emailTemplateService")
public class EmailTemplateService {
    
//    @Autowired
    private final EmailTemplateRepository emailTemplateDAO;

//    @Autowired
    private final AppRepository appRepository;

    public EmailTemplateService(EmailTemplateRepository emailTemplateDAO,
                                AppRepository appRepository){
        this.emailTemplateDAO = emailTemplateDAO;
        this.appRepository = appRepository;
    }

    /**
     * This service will create a new EmailTemplate
     * @param template EmailTemplate object that is to be created
     * @return The newly created EmailTemplate Object
     */
    @Transactional
     public EmailTemplate create(EmailTemplate template, Long appId, String email) {
        App app = appRepository.getReferenceById(appId);
        template.setApp(app);
        template.setCreator(email);
        return emailTemplateDAO.save(template);
    }

//    /**
//     * This service will retun the list of all EmailTemplate
//     * @return The coresponding EMailTemplate list of the query
//     */
//    public List<EmailTemplate> getEmailTemplateList() {
//        return emailTemplateDAO.findAll();
//    }

    /**
     * This service will retun the list of all EmailTemplate
     * @return The coresponding EMailTemplate list of the query
     */
//    public Page<EmailTemplate> getEmailTemplateList(String email,String searchText,Pageable pageable) {
//        searchText = "%" + searchText.toUpperCase() + "%";
//        return emailTemplateDAO.findByCreator(email,searchText,pageable);
//    }

    public Page<EmailTemplate> findByAppId(Long appId,String searchText,Pageable pageable) {
        searchText = "%" + searchText.toUpperCase() + "%";
        return emailTemplateDAO.findByAppId(appId,searchText,pageable);
    }

    public Page<EmailTemplate> findPickableByAppId(Long appId,String searchText,Pageable pageable) {
        searchText = "%" + searchText.toUpperCase() + "%";
        return emailTemplateDAO.findPickableByAppId(appId,searchText,pageable);
    }

    /**
     * This service method will retrieve a single object on the EmailTemplate
     * @param id The EmailTemplate id that is to be retrieved
     * @return The retrieved EmailTemplate object
     */
    public EmailTemplate getEmailTemplate(Long id){
        return emailTemplateDAO.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("EmailTemplate","id",id));

    }


    public void delete(Long id) {
        emailTemplateDAO.deleteById(id);
    }
}
