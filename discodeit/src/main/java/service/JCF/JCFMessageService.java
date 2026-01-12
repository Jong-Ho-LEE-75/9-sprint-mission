package service.jcf;

import entity.Message;
import service.MessageService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JCFMessageService implements MessageService {
    private final Map<String, Message> data;

    public JCFMessageService() {
        this.data = new HashMap<>();
    }

    @Override
    public Message create(Message message) {
        data.put(message.getId(), message);
        return message;
    }

    @Override
    public Message findById(String id) {
        return data.get(id);
    }

    @Override
    public List<Message> findAll() {
        return data.values().stream().collect(Collectors.toList());
    }

    @Override
    public Message update(String id, String content, String userId, String channelId) {
        Message message = data.get(id);
        if (message != null) {
            message.update(content, userId, channelId);
        }
        return message;
    }

    @Override
    public void delete(String id) {
        data.remove(id);
    }
}