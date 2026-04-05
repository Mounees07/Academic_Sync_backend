package com.academic.platform.service;

import com.academic.platform.model.Notification;
import com.academic.platform.model.User;
import com.academic.platform.repository.NotificationRepository;
import com.academic.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepo;

    @Autowired
    private UserRepository userRepository;

    public List<Notification> getAll(String userUid) {
        return notificationRepo.findByUserFirebaseUidOrderByCreatedAtDesc(userUid);
    }

    public List<Notification> getUnread(String userUid) {
        return notificationRepo.findByUserFirebaseUidAndIsReadFalseOrderByCreatedAtDesc(userUid);
    }

    public long getUnreadCount(String userUid) {
        return notificationRepo.countByUserFirebaseUidAndIsReadFalse(userUid);
    }

    public Notification markRead(Long notifId) {
        Notification n = notificationRepo.findById(notifId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notifId));
        n.setIsRead(true);
        return notificationRepo.save(n);
    }

    public int markAllRead(String userUid) {
        return notificationRepo.markAllReadByUserFirebaseUid(userUid);
    }

    public void deleteNotification(Long notifId, String userUid) {
        long deleted = notificationRepo.deleteByIdAndUserFirebaseUid(notifId, userUid);
        if (deleted == 0) {
            throw new RuntimeException("Notification not found: " + notifId);
        }
    }

    public Notification createNotification(String userUid, String type, String title,
                                            String message, String actionUrl) {
        User user = userRepository.findByFirebaseUid(userUid)
                .orElseThrow(() -> new RuntimeException("User not found: " + userUid));

        Notification n = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .actionUrl(actionUrl)
                .isRead(false)
                .build();
        return notificationRepo.save(n);
    }

    /** Helper method to send low attendance alert */
    public void sendLowAttendanceAlert(String userUid, String subjectName, double percentage) {
        String message = String.format(
                "Your attendance in %s is %.1f%%. Minimum required is 75%%. Take action now!",
                subjectName, percentage);
        createNotification(userUid, "LOW_ATTENDANCE",
                "⚠️ Low Attendance Alert", message, "/attendance");
    }

    /** Helper: fee due reminder */
    public void sendFeeDueAlert(String userUid, double amount) {
        String message = String.format(
                "Fee payment of ₹%.0f is overdue. Pay now to avoid late fees.", amount);
        createNotification(userUid, "FEE_DUE",
                "💰 Fee Due Reminder", message, "/student/fees");
    }

    /** Helper: leave status update */
    public void sendLeaveUpdate(String userUid, String leaveType, String newStatus) {
        String message = String.format(
                "Your %s leave application has been %s.", leaveType, newStatus.toLowerCase());
        createNotification(userUid, "LEAVE_UPDATE",
                "📋 Leave Status Update", message, "/student/leaves");
    }
}
