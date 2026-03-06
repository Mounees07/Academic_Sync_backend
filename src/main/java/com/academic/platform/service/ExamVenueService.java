package com.academic.platform.service;

import com.academic.platform.model.ExamVenue;
import com.academic.platform.repository.ExamVenueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExamVenueService {

    @Autowired
    private ExamVenueRepository examVenueRepository;

    public List<ExamVenue> getAllVenues() {
        return examVenueRepository.findAll();
    }

    public ExamVenue createVenue(ExamVenue venue) {
        return examVenueRepository.save(venue);
    }

    public ExamVenue updateVenue(Long id, ExamVenue venueDetails) {
        ExamVenue venue = examVenueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venue not found"));

        venue.setName(venueDetails.getName());
        venue.setBlock(venueDetails.getBlock());
        venue.setCapacity(venueDetails.getCapacity());
        venue.setExamType(venueDetails.getExamType());
        venue.setAvailable(venueDetails.isAvailable());

        return examVenueRepository.save(venue);
    }

    public void deleteVenue(Long id) {
        examVenueRepository.deleteById(id);
    }
}
