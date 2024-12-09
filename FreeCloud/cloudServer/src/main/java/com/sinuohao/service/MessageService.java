package com.sinuohao.service;

import com.sinuohao.model.Message;
import java.util.List;

public interface MessageService {
    Message saveMessage(Message message);
    List<Message> searchMessages(String query, int start, int end);
}
