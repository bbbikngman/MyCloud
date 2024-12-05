package com.sinuohao.model;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private String id;
    
    @Column(nullable = false)
    private String username;
    
    @Column(name = "last_active")
    private LocalDateTime lastActive;
    
    @PrePersist
    protected void onCreate() {
        lastActive = LocalDateTime.now();
    }
}