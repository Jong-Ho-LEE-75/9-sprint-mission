package com.sprint.mission.discodeit.example15;

import java.util.List;

public class MenuServiceStub extends MenuService {

    @Override
    public List<String> getMenuList() {
        return List.of("샘플1커피", "샘플2커피"); // Stub 데이터로 저장
    }
}
