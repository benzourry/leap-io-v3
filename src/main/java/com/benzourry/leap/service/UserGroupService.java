package com.benzourry.leap.service;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.App;
import com.benzourry.leap.model.UserGroup;
import com.benzourry.leap.repository.AppRepository;
import com.benzourry.leap.repository.AppUserRepository;
import com.benzourry.leap.repository.UserGroupRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserGroupService {
    AppRepository appRepository;
    UserGroupRepository userGroupRepository;
    AppUserRepository appUserRepository;

    public UserGroupService(AppRepository appRepository,
                            UserGroupRepository userGroupRepository,
                            AppUserRepository appUserRepository){
        this.appRepository = appRepository;
        this.userGroupRepository = userGroupRepository;
        this.appUserRepository = appUserRepository;
    }

    public UserGroup save(UserGroup userGroup, Long appId){
        App app = appRepository.getReferenceById(appId);
        userGroup.setApp(app);
        return userGroupRepository.save(userGroup);
    }

    // for designer
    public Page<UserGroup> findByAppId(Long appId, Pageable pageable){
        App app = appRepository.findById(appId).orElseThrow(()->new ResourceNotFoundException("App","id",appId));
        return this.userGroupRepository.findByAppIdAll(appId,
                app.getX().at("/userFromApp").asLong(), pageable);
    }

    public UserGroup findById(Long id) {
        return userGroupRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("UserGroup","id",id));
    }

    @Transactional
    public void delete(Long id) {
        appUserRepository.deleteByUserGroup(id);
        userGroupRepository.deleteById(id);
    }

    // used to display group that can be register during registration
    public List<UserGroup> findRegListByAppId(Long appId) {
        return this.userGroupRepository.findRegListByAppId(appId);
    }

    // for admin to manage user
    public List<UserGroup> findAllListByAppId(Long appId) {
        return this.userGroupRepository.findAllListByAppId(appId);
    }
}
