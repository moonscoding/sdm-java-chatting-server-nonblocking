import model.Server;

/**
 * [ Class :: main.MainClient ]
 *
 * @경로 :: /src/main.MainClient.java
 * @목적 :: 실행클래스
 * @진행 :: 
 * @주의 ::
 * @일자 :: 2018.07.16
 * @작성 :: SDM
 * @참조 ::
 * */
public class Main {

    public static void main(String[] args) {
        Server server = new Server();
        server.startServer();
    }

}
