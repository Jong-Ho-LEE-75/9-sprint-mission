package service.jcf;

import entity.Channel;
import entity.ChannelType;
import service.ChannelService;
import java.util.*;

public class JCFChannelService implements ChannelService {
    private final Map<UUID, Channel> channelMap;

    public JCFChannelService() {
        this.channelMap = new HashMap<>();
    }

    @Override
    public Channel create(ChannelType type, String name, String description) {
        Channel channel = new Channel(type, name, description);
        channelMap.put(channel.getId(), channel);
        return channel;
    }

    @Override
    public Channel find(UUID id) {
        return channelMap.get(id);
    }

    @Override
    public List<Channel> findAll() {
        return new ArrayList<>(channelMap.values());
    }

    @Override
    public Channel update(UUID id, String name, String description) {
        Channel channel = channelMap.get(id);
        if (channel != null) {
            channel.update(name, description);
        }
        return channel;
    }

    @Override
    public void delete(UUID id) {
        channelMap.remove(id);
    }
}