package service.jcf;

import entity.Channel;
import service.ChannelService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JCFChannelService implements ChannelService {
    private final Map<String, Channel> data;

    public JCFChannelService() {
        this.data = new HashMap<>();
    }

    @Override
    public Channel create(Channel channel) {
        data.put(channel.getId(), channel);
        return channel;
    }

    @Override
    public Channel findById(String id) {
        return data.get(id);
    }

    @Override
    public List<Channel> findAll() {
        return data.values().stream().collect(Collectors.toList());
    }

    @Override
    public Channel update(String id, String name, String description, String type) {
        Channel channel = data.get(id);
        if (channel != null) {
            channel.update(name, description, type);
        }
        return channel;
    }

    @Override
    public void delete(String id) {
        data.remove(id);
    }
}