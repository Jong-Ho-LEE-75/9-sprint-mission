package service;

import entity.Message;
import java.util.List;
import java.util.UUID;

public interface MessageService {
    Message create(String content, UUID channelId, UUID authorId);
    Message find(UUID id); // Long -> UUID
    List<Message> findAll();
    Message update(UUID id, String content);
    void delete(UUID id); // Long -> UUID
}