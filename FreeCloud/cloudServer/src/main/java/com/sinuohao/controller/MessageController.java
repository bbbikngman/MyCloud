package com.sinuohao.controller;

import com.sinuohao.model.Message;
import com.sinuohao.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageController {
    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private MessageService messageService;

    @PostMapping
    public ResponseEntity<?> createMessage(@RequestBody Message message) {
        try {
            logger.debug("Creating message: {}", message);
            Message savedMessage = messageService.saveMessage(message);
            return ResponseEntity.ok(savedMessage);
        } catch (Exception e) {
            logger.error("Error creating message: {}", message, e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> searchMessages(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "100") int end) {
        try {
            if (start < 0 || end < start) {
                return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", "Invalid range: start must be non-negative and end must be greater than start"));
            }
            
            logger.debug("Searching messages - query: {}, start: {}, end: {}", query, start, end);
            List<Message> messages = messageService.searchMessages(query, start, end);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            logger.error("Error searching messages with query: {}", query, e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
}