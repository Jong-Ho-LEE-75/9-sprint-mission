package service;

import entity.Channel;
import entity.ChannelType;
import java.util.List;
import java.util.UUID;

public interface ChannelService {
    Channel create(ChannelType type, String name, String description);
    Channel find(UUID id); // Long -> UUID
    List<Channel> findAll();
    Channel update(UUID id, String name, String description);
    void delete(UUID id); // Long -> UUID
}