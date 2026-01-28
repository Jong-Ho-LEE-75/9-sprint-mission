package com.sprint.mission.discodeit.entity;
import lombok.Getter;

@Getter
public class Channel extends BaseEntity {
    private final ChannelType type;
    private String name;
    private String description;

    public Channel(ChannelType type, String name, String description) {
        super();
        this.type = type;
        this.name = name;
        this.description = description;
    }

    public void update(String name, String description) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        updateTimeStamp();
    }
}