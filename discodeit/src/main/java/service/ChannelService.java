package service;

import entity.Channel;
import java.util.List;

public interface ChannelService {
    Channel create(Channel channel);
    Channel findById(String id);
    List<Channel> findAll();
    Channel update(String id, String name, String description, String type);
    void delete(String id);
}