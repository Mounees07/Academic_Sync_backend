package com.academic.platform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "agenda_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgendaItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    // Store time as string "08:00 am" strictly as requested, or preferably
    // LocalTime and format on retrieval.
    // Given the UI shows things like "08:00 am", let's store LocalTime for proper
    // sorting and format on DTO/Frontend.
    private LocalTime time;

    private String type; // e.g. "All Grade", "Grade 3-5"

    private String colorClass; // e.g. "purple", "orange", "blue" for styling classes

    private LocalDate date;

    @PrePersist
    public void prePersist() {
        if (date == null) {
            date = LocalDate.now();
        }
    }
}
