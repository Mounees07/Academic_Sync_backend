package com.academic.platform.dto;

import com.academic.platform.model.User;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentStudentsDirectoryDTO {
    private Stats stats;
    private List<User> students;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stats {
        private int totalStudents;
        private int undergraduates;
        private int postgraduates;
        private int atRisk;
    }
}
