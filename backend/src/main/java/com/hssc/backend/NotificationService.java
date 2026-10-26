package com.hssc.backend;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class NotificationService {
    private final NotificationRepository notifications; private final UserRepository users;
    public NotificationService(NotificationRepository notifications,UserRepository users){this.notifications=notifications;this.users=users;}
    @Transactional public void notify(UserAccount recipient,String type,String message){
        if(recipient!=null) notifications.save(new Notification(recipient,type,message,actionUrl(type)));
    }
    @Transactional public void notifyAdmins(String type,String message){
        users.findByRoleAndStatus(Role.ADMIN,AccountStatus.ACTIVE)
                .forEach(u->notifications.save(new Notification(u,type,message,actionUrl(type))));
    }
    @Transactional(readOnly=true) public List<Api.NotificationResponse> forAccount(UserAccount user){
        return notifications.findByRecipientIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(n->new Api.NotificationResponse(n.getId(),n.getType(),n.getMessage(),n.isRead(),n.getCreatedAt(),n.getActionUrl())).toList();
    }
    @Transactional public void markRead(UserAccount user,UUID id){
        Notification n=notifications.findById(id).orElseThrow(()->ApiException.notFound("Notification not found"));
        if(!n.getRecipient().getId().equals(user.getId())) throw ApiException.forbidden("Notification belongs to another account");
        n.markRead();
    }
    private String actionUrl(String type){
        return switch(type){
            case "SCHOOL_REGISTRATION","POSSIBLE_DUPLICATE" -> "/admin/registrations";
            case "STUDENT_REGISTRATION" -> "/school/students";
            case "RESULTS_RELEASED","DISTRICT_RESULTS_RELEASED" -> "/admin/results";
            case "ACCOUNT_REVIEW" -> "/dashboard";
            case "RESULT_RELEASED" -> "/results";
            default -> null;
        };
    }
}
