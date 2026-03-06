package com.academic.platform.service;

import com.academic.platform.model.AgendaItem;
import com.academic.platform.repository.AgendaItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AgendaService {

    @Autowired
    private AgendaItemRepository agendaRepository;

    public List<AgendaItem> getAgendaForDate(LocalDate date) {
        return agendaRepository.findByDateOrderByTimeAsc(date);
    }

    public AgendaItem addAgendaItem(AgendaItem item) {
        return agendaRepository.save(item);
    }

    public void deleteAgendaItem(Long id) {
        agendaRepository.deleteById(id);

    }

    public AgendaItem updateAgendaItem(Long id, AgendaItem updatedItem) {
        return agendaRepository.findById(id).map(item -> {
            item.setTitle(updatedItem.getTitle());
            item.setType(updatedItem.getType());
            item.setDate(updatedItem.getDate());
            item.setTime(updatedItem.getTime());
            item.setColorClass(updatedItem.getColorClass());
            return agendaRepository.save(item);
        }).orElseThrow(() -> new RuntimeException("Agenda item not found"));
    }
}
