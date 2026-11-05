package com.benzourry.leap.utility;

import com.benzourry.leap.config.AppLogBatchProcessor;
import com.benzourry.leap.model.AppLog;
import com.benzourry.leap.security.UserPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;

public class TenantLogger {

    // Notice: The 'email' parameter is gone!
    public static void info(Long appId, String module, Long moduleId, String data) {
        sendLog(appId, module, moduleId, "INFO", data);
    }

    public static void success(Long appId, String module, Long moduleId, String data) {
        sendLog(appId, module, moduleId, "SUCCESS", data);
    }

    public static void error(Long appId, String module, Long moduleId, String errorData) {
        sendLog(appId, module, moduleId, "FAILED", errorData);
    }

    private static void sendLog(Long appId, String module, Long moduleId, String status, String data) {
        AppLog log = new AppLog();
        log.setAppId(appId);
        log.setModuleId(moduleId);
        log.setModule(module);
        log.setStatus(status);
        log.setData(data);
        log.setTimestamp(new Date());
        log.setEmail(getPrincipalEmail());

        // Queue for DB batch processing
        AppLogBatchProcessor.queueLog(log);

        // Broadcast to connected WebSocket clients dynamically based on appId
        try {
            if (WsContext.getTemplate() != null) {
                WsContext.getTemplate().convertAndSend("/topic/logs/" + appId, log);
            }
        } catch (Exception e) {
            // Failsafe in case WS broker is down
            System.err.println("Failed to broadcast log: " + e.getMessage());
        }
    }

//    private static String getCurrentUserEmail() {
//        try {
//            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//
//            // Check if user is actually logged in (prevents NullPointerExceptions)
//            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal) {
//                UserPrincipal user = (UserPrincipal) auth.getPrincipal();
//                return user.getEmail(); // Assuming your UserPrincipal has getEmail() or getUsername()
//            }
//        } catch (Exception e) {
//            // Ignore security context errors safely
//        }
//
//        // 2. Fallback for background tasks!
//        return "system";
//    }

    public static String getPrincipalEmail() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof UserPrincipal) {
                    return ((UserPrincipal) principal).getEmail();
                } else {
                    // Optionally log or handle unexpected principal types
                    return authentication.getName(); // fallback
                }
            } else {
                return "anonymous";
            }
        } catch (Exception e) {
            // Ignore security context errors safely
        }
        return "anonymous";
    }

}
