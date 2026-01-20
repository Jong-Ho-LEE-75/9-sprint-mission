package com.sprint.mission.discodeit.entity;

public class Channel extends BaseEntity {
    private final ChannelType type;
    private String name;
    private String description;

    public Channel(ChannelType type, String name, String description) {
        super(); // ID 생성
        this.type = type;
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }

    // update 로직
    public void update(String name, String description) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        updateTimeStamp();
    }

    public ChannelType getType() {
        return type;
    }

    public String getDescription() { return description; }
}