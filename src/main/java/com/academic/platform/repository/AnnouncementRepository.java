package com.academic.platform.repository;

import com.academic.platform.model.Announcement;
import com.academic.platform.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByTargetSection(Section targetSection);
}
