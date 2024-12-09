package com.sinuohao.service.impl;

import com.sinuohao.model.Message;
import com.sinuohao.repository.MessageRepository;
import com.sinuohao.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Implementation of MessageService interface.
 */
@Service
public class MessageServiceImpl implements MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);

    @Autowired
    private MessageRepository messageRepository;

    @Override
    public Message saveMessage(Message message) {
        try {
            logger.debug("Saving message: {}", message);
            return messageRepository.save(message);
        } catch (Exception e) {
            logger.error("Error saving message: {}", message, e);
            throw new RuntimeException("Failed to save message", e);
        }
    }

    @Override
    public List<Message> searchMessages(String query, int start, int end) {
        try {
            int limit = end - start;
            if (limit <= 0) {
                logger.warn("Invalid range: start={}, end={}", start, end);
                return List.of();
            }
            
            logger.debug("Searching messages - query: {}, start: {}, end: {}", query, start, end);
            return messageRepository.searchMessages(query, start, limit);
        } catch (Exception e) {
            logger.error("Error searching messages with query: {}", query, e);
            throw new RuntimeException("Failed to search messages", e);
        }
    }
}
