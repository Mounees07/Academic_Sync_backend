package com.academic.platform.repository;

import com.academic.platform.model.MentorshipMeeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MentorshipMeetingRepository extends JpaRepository<MentorshipMeeting, Long> {
    List<MentorshipMeeting> findByMentorFirebaseUid(String mentorUid);

    List<MentorshipMeeting> findByMenteeFirebaseUid(String menteeUid);

    List<MentorshipMeeting> findByStartTimeBefore(java.time.LocalDateTime dateTime);
}
