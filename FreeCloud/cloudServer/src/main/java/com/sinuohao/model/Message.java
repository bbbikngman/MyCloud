package com.sinuohao.model;

import lombok.Data;
import javax.persistence.*;

import org.apache.tomcat.util.net.openssl.ciphers.EncryptionLevel;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String content;

    @Column(name = "sender_id")
    private String senderId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "encryption_level")
    @Enumerated(EnumType.STRING)
    private EncryptionLevel encryptionLevel;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}