package com.benzourry.leap.service;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.Notification;
import com.benzourry.leap.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Service
public class NotificationService {
//    @Autowired
    final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository){
        this.notificationRepository = notificationRepository;
    }

    public Notification save(Notification notification){
        return notificationRepository.save(notification);
    }

    public List<Notification> saveAll(List<Notification> notifications){
        return notificationRepository.saveAll(notifications);
    }

    public Notification markRead(Long notificationId, String email){
        Notification n = notificationRepository.findById(notificationId).orElseThrow(()->new ResourceNotFoundException("Notification","id",notificationId));
        n.setStatus("read");
        if (n.getReceipt()==null) n.setReceipt(new HashMap<>());
        n.getReceipt().put(email,true);
        return notificationRepository.save(n);
    }

    public List<Notification> notifyAll(String[]to, String content, String subject, String url, String from, Long appId){
        List<Notification> nList = new ArrayList<>();
        for (String email : to) {
            Notification n = new Notification();
            n.setStatus("new");
            n.setEmail(email);
            n.setSender(from);
            n.setContent(content);
            n.setSubject(subject);
            n.setAppId(appId);
            n.setUrl(url);
            n.setTimestamp(new Date());
            nList.add(n);
        }
        return notificationRepository.saveAll(nList);
    }

    public Page<Notification> findByAppIdAndEmail(Long appId, String email, Pageable pageable){
        if (email!=null) {
            email = "%" + email.toLowerCase() + "%";
        }
        return notificationRepository.findByAppIdAndEmail(appId,email,pageable);
    }

    public Page<Notification> findByAppIdAndParam(Long appId, String searchText, String email,Long tplId, Pageable pageable){
        if (searchText!=null) {
            searchText = "%" + searchText.toLowerCase() + "%";
        }
        if (email!=null) {
            email = email.toLowerCase();
        }
        return notificationRepository.findByAppIdAndParam(appId,searchText,email,tplId,pageable);
    }
}
