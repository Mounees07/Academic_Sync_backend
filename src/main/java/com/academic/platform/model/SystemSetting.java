package com.academic.platform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "system_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemSetting {
    @Id
    @Column(name = "setting_key")
    private String key; // e.g., "site_name", "maintenance_mode"

    @Column(name = "setting_value", nullable = false)
    private String value;

    private String description;

    // Helper for boolean conversion
    public boolean getBooleanValue() {
        return Boolean.parseBoolean(value);
    }
}
