package com.academic.platform.service;

import com.academic.platform.model.CollegeCalendarEvent;
import com.academic.platform.repository.CollegeCalendarEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class CollegeCalendarService {

    @Autowired
    private CollegeCalendarEventRepository eventRepository;

    public List<CollegeCalendarEvent> getEvents(String month, LocalDate startDate, LocalDate endDate) {
        LocalDate effectiveStart = startDate;
        LocalDate effectiveEnd = endDate;

        if (month != null && !month.isBlank()) {
            YearMonth parsedMonth = YearMonth.parse(month);
            effectiveStart = parsedMonth.atDay(1);
            effectiveEnd = parsedMonth.atEndOfMonth();
        }

        if (effectiveStart == null || effectiveEnd == null) {
            LocalDate today = LocalDate.now();
            YearMonth currentMonth = YearMonth.from(today);
            effectiveStart = currentMonth.atDay(1);
            effectiveEnd = currentMonth.atEndOfMonth();
        }

        return eventRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAscEndDateAsc(
                effectiveEnd,
                effectiveStart
        );
    }

    public List<CollegeCalendarEvent> getUpcomingEvents(int limit) {
        List<CollegeCalendarEvent> events = eventRepository.findTop12ByEndDateGreaterThanEqualOrderByStartDateAscEndDateAsc(LocalDate.now());
        return events.stream().limit(Math.max(1, limit)).toList();
    }

    public CollegeCalendarEvent createEvent(CollegeCalendarEvent event) {
        normalizeAndValidate(event);
        return eventRepository.save(event);
    }

    public CollegeCalendarEvent updateEvent(Long id, CollegeCalendarEvent payload) {
        CollegeCalendarEvent existing = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Calendar event not found"));

        existing.setTitle(payload.getTitle());
        existing.setType(payload.getType());
        existing.setStartDate(payload.getStartDate());
        existing.setEndDate(payload.getEndDate());
        existing.setAudience(payload.getAudience());
        existing.setDescription(payload.getDescription());
        existing.setAllDay(payload.isAllDay());
        existing.setColorToken(payload.getColorToken());

        normalizeAndValidate(existing);
        return eventRepository.save(existing);
    }

    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new IllegalArgumentException("Calendar event not found");
        }
        eventRepository.deleteById(id);
    }

    private void normalizeAndValidate(CollegeCalendarEvent event) {
        if (event.getTitle() == null || event.getTitle().isBlank()) {
            throw new IllegalArgumentException("Event title is required");
        }
        if (event.getType() == null || event.getType().isBlank()) {
            throw new IllegalArgumentException("Event type is required");
        }
        if (event.getAudience() == null || event.getAudience().isBlank()) {
            event.setAudience("ALL");
        }
        if (event.getStartDate() == null || event.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        if (event.getEndDate().isBefore(event.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        event.setType(event.getType().trim().toUpperCase());
        event.setAudience(event.getAudience().trim().toUpperCase());
        if (event.getDescription() != null) {
            event.setDescription(event.getDescription().trim());
        }
        if (event.getColorToken() != null && event.getColorToken().isBlank()) {
            event.setColorToken(null);
        }
    }
}
