package com.academic.platform.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ScheduleCleanup implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🧹 Running Schedule Cleanup...");
        try {
            // Task: Truncate existing Exam Schedules that might have been uploaded by
            // non-COE users
            // or just clean slate all schedules as 'Truncate' usually implies.
            // BUT, safest interpretation of "Truncate the Schedule Uploaded By HOD and
            // maintain the Examination schedule only done by th COE"
            // is to delete exams linked to HODs.
            // Since we don't have a strict link to 'role' in the schedules table (only
            // department),
            // and previously HODs uploaded exams, we might need to delete ALL exams to be
            // safe and ask COE to re-upload,
            // OR delete exams where department is NOT 'COE' (if COE has a department).
            // Assuming COE uploads are new, let's just delete ALL 'INTERNAL_EXAM' and
            // 'SEMESTER_EXAM'
            // and let the COE re-upload properly. This ensures "only done by the COE"
            // constraint is met moving forward.
            // Wait, the user said "Truncate the Schedule Uploaded By HOD".
            // Let's delete ALL schedules where type in (INTERNAL_EXAM, SEMESTER_EXAM).
            // This forces a clean slate for exams.

            // String sql = "DELETE FROM academic_schedules WHERE type IN ('INTERNAL_EXAM',
            // 'SEMESTER_EXAM')";
            // int rows = jdbcTemplate.update(sql);
            // System.out.println("✅ Deleted " + rows + " existing Exam entries. COE must
            // re-upload official schedules.");
            System.out.println("ℹ️ Schedule Cleanup Disabled. Existing entries preserved.");

            // Note: HOD class schedules (ACADEMIC etc) are preserved.

        } catch (Exception e) {
            System.out.println("⚠️ Schedule cleanup error: " + e.getMessage());
        }
    }
}
