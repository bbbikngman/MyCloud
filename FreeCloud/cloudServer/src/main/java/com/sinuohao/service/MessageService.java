package com.sinuohao.service;

import com.sinuohao.model.Message;
import java.util.List;

public interface MessageService {
    List<Message> getAllMessages();
    Message saveMessage(Message message);
}
