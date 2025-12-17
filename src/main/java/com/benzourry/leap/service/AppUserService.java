package com.benzourry.leap.service;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.AppUser;
import com.benzourry.leap.model.User;
import com.benzourry.leap.model.UserGroup;
import com.benzourry.leap.repository.AppUserRepository;
import com.benzourry.leap.repository.UserGroupRepository;
import com.benzourry.leap.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    public AppUserService(AppUserRepository appUserRepository,
                          UserGroupRepository userGroupRepository,
                          UserRepository userRepository){
        this.appUserRepository = appUserRepository;
        this.userGroupRepository = userGroupRepository;
        this.userRepository = userRepository;
    }

    public AppUser save(AppUser appUser){
        return this.appUserRepository.save(appUser);
    }

    public Page<AppUser> findByAppIdAndStatus(Long appId, String searchText, List<String> status, Long group, Pageable pageable){
        return this.appUserRepository.findByAppIdAndParam(appId, searchText,status, group, pageable);
    }

    public Page<AppUser> findByGroupId(Long groupId, Pageable pageable) {
        return this.appUserRepository.findByGroupId(groupId, pageable);
    }

    public List<AppUser> findByAppIdAndEmail(Long appId, String email) {
        return this.appUserRepository.findByAppIdAndEmail(appId,email);
    }

    public List<AppUser> findByUserId(Long userId) {
        return this.appUserRepository.findByUserId(userId);
    }

    public AppUser approval(Long appUserId, String status, List<String> tags){

        AppUser appUser = appUserRepository.findById(appUserId)
                .orElseThrow(()->new ResourceNotFoundException("AppUser","id",appUserId));

        appUser.setStatus(status);
        appUser.setTags(tags);
        appUserRepository.save(appUser);

        User user = userRepository.getReferenceById(appUser.getUser().getId());

        List<UserGroup> regable = userGroupRepository.findRegListByAppId(user.getAppId());

        List<AppUser> appUserList = appUserRepository.findByUserId(user.getId());

        long approvedCount = 0;
        long rejectedCount = 0;

        for (AppUser au : appUserList) {
            String s = au.getStatus();
            if ("approved".equals(s)) {
                approvedCount++;
            } else if ("rejected".equals(s)) {
                rejectedCount++;
            }
        }

        if (approvedCount>0){
            user.setStatus("approved");
        }else if (rejectedCount == regable.size()){
            user.setStatus("rejected");
        }else{
            user.setStatus("pending");
        }

        userRepository.save(user);

        return appUser;

    }
    public User userApproval(Long userId, String status){
        User user = userRepository.findById(userId)
                .orElseThrow(()->new ResourceNotFoundException("User","id",userId));
        user.setStatus(status);
        userRepository.save(user);
        return user;
    }

    public void deleteById(Long id) {
        AppUser appUser = appUserRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("AppUser","id",id));
        Long userId = appUser.getUser().getId();

        appUserRepository.deleteById(id);

        User user = userRepository.findById(userId)
                .orElseThrow(()->new ResourceNotFoundException("User","id",id));

        List<UserGroup> regable = userGroupRepository.findRegListByAppId(user.getAppId());

        List<AppUser> appUserList = appUserRepository.findByUserId(user.getId());

        if (appUserList.size()==0){
            // if no more appUser just remove the user
            userRepository.deleteById(userId);
        }else{
            // if still got appUser, check for and update user status
            long approvedCount = appUserList.stream().filter(au->"approved".equals(au.getStatus())).count();

            long rejectedCount = appUserList.stream().filter(au->"rejected".equals(au.getStatus())).count();

            if (approvedCount>0){
                user.setStatus("approved");
            }else if (rejectedCount == regable.size()){
                user.setStatus("rejected");
            }else{
                user.setStatus("pending");
            }
            userRepository.save(user);
        }
    }

    public void deleteUserById(Long id) {
       userRepository.deleteById(id);
    }

    public List<Map<String, Long>> saveOrder(List<Map<String, Long>> userOrderList) {
        for (Map<String, Long> element : userOrderList) {
            AppUser fi = appUserRepository.findById(element.get("id")).get();
            fi.setSortOrder(element.get("sortOrder"));
            appUserRepository.save(fi);
        }
        return userOrderList;
    }

    public Page<AppUser> userByGroupId(Long id, String searchText, List<String> status, Pageable pageable) {
        return appUserRepository.findByGroupIdAndParams(id, searchText, status,
                Optional.ofNullable(status).orElse(List.of()).isEmpty(), pageable);
    }
}
