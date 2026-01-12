package service;

import entity.Message;
import java.util.List;

public interface MessageService {
    Message create(Message message);
    Message findById(String id);
    List<Message> findAll();
    Message update(String id, String content, String userId, String channelId);
    void delete(String id);
}