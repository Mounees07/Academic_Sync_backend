package com.academic.platform.controller;

import com.academic.platform.model.AgendaItem;
import com.academic.platform.service.AgendaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/agenda")
@CrossOrigin(origins = { "http://localhost:5173", "http://10.10.188.128:5173" }, allowCredentials = "true")
public class AgendaController {

    @Autowired
    private AgendaService agendaService;

    @GetMapping
    public ResponseEntity<List<AgendaItem>> getAgenda(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(agendaService.getAgendaForDate(date));
    }

    @PostMapping
    public ResponseEntity<AgendaItem> addAgendaItem(@RequestBody AgendaItem item) {
        return ResponseEntity.ok(agendaService.addAgendaItem(item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgendaItem(@PathVariable Long id) {
        agendaService.deleteAgendaItem(id);
        return ResponseEntity.noContent().build();

    }

    @PutMapping("/{id}")
    public ResponseEntity<AgendaItem> updateAgendaItem(@PathVariable Long id, @RequestBody AgendaItem item) {
        return ResponseEntity.ok(agendaService.updateAgendaItem(id, item));
    }
}
