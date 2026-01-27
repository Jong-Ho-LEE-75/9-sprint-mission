package com.sprint.mission.discodeit.example15;

public class DIExampleUsingExtends {
    public static void main(String[] args) {
        //실제 운영용
        MenuService service = new MenuService();
        MenuController controller = new MenuController(service);
        System.out.println("==실제 운영 예시==");
        controller.printMenu();

        // 테스트용 또는 stub 교체
        MenuService stubService = new MenuServiceStub();
        MenuController testController = new MenuController(stubService);
        System.out.println("\n==테스트용 혹은 stub 예시==");
        testController.printMenu();
    }
}
