package model;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import util.Define;
import util.Util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class Client {

    public ExecutorService executorService;
    public SocketChannel socketChannel;
    public List<Client> clients;
    public RoomManager roomManager;
    public Selector selector;
    public SelectionKey selectionKey;
    public Room room;
    public String response;

    public Client(
            ExecutorService executorService,
            SocketChannel socketChannel,
            List<Client> clients,
            RoomManager roomManager,
            Selector selector) {
        try {
            // System.out.println("client create");

            this.executorService = executorService;
            this.socketChannel = socketChannel;
            this.clients = clients;
            this.roomManager = roomManager;
            this.selector = selector;
            this.room = null;

            // OP_READ - SelectionKey 추가
            this.socketChannel.configureBlocking(false);
            this.selectionKey = this.socketChannel.register(selector, SelectionKey.OP_READ);
            this.selectionKey.attach(this);

            this.sendStatus();
            selector.wakeup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * [ Method :: receive ]
     *
     * @DES :: 수신메소드 ( Json 데이터 수신 )
     * @S.E :: "method" 타입별 분기 처리
     */
    void receive() {
        try {
            ByteBuffer bb = ByteBuffer.allocate(Define.BUFFER_SIZE);

            int byteCount = socketChannel.read(bb);
            if (byteCount == -1) throw new IOException(); // ### 정상종료 :: close() 호출 ###

            // ### 메세지 ###
            bb.flip();
            Charset cs = Charset.forName("UTF-8");
            String strJson = cs.decode(bb).toString();
            try {
                JSONParser jsonParser = new JSONParser();
                JSONObject token = (JSONObject) jsonParser.parse(strJson);
                String method = token.get("method").toString();

                Util.log("[채팅서버] 수신 " + socketChannel.getRemoteAddress() + method);
                // Util.log("[채팅서버] 현스레드 : " + Thread.currentThread().getName() );
                switch (method) {
                    // #방생성
                    case "/room/create":
                        if (room == null) {
                            roomManager.createRoom(token.get("title").toString(), Client.this);
                            Util.log("[채팅서버] 채팅방 개설 " + socketChannel.getRemoteAddress());
                            Util.log("[채팅서버] 현재 채팅방 갯수 " + roomManager.rooms.size());
                        }
                        break;
                    // #방입장
                    case "/room/entry":
                        if (room == null) {
                            for (int i = 0; i < roomManager.rooms.size(); i++) {
                                if (roomManager.rooms.get(i).id.equals(token.get("id").toString())) {
                                    roomManager.rooms.get(i).entryRoom(Client.this);
                                    Util.log("[채팅서버] 채팅방 입장" + socketChannel.getRemoteAddress());
                                    break;
                                }
                            }
                        }
                        break;
                    // #방탈출
                    case "/room/leave":
                        if (room != null) {
                            Client.this.room.leaveRoom(Client.this);
                            Util.log("[채팅서버] 채팅방 나감" + socketChannel.getRemoteAddress());
                        }
                        break;
                    // #쳇전송
                    case "/chat/send":
                        if (room != null) {
                            for (Client c : room.clients) {
                                if (c != Client.this) {
                                    c.sendEcho(token.get("id").toString(), token.get("message").toString());
                                }
                            }
                        }
                        break;
                    default:
                        Util.log("[채팅서버] 메소드가 올바르지 않습니다. : " + socketChannel.getRemoteAddress());
                }

                // ### wakeup() ###
                selector.wakeup();

            } catch (Exception errA) {
                // Todo 올바른 데이터 형식이 아닙니다.
                Util.log("[채팅서버] 올바른 데이터 형식이 아닙니다. : " + strJson);
                errA.printStackTrace();
            }
        } catch (IOException errB) {
            terminate();
        }
    }

    /**
     * [ Method :: sendStatus ]
     *
     * @DES :: entry & leave 의 상태에 따라 계속적으로 클라이언트에게 인원수 전송
     * @S.E :: "method" /room/status
     */
    void send() {
        try {

            // 테스트코드
            // System.out.println(roomManager.roomStatus);

            // ### write() ###
            String packet = this.response;
            Charset cs = Charset.forName("UTF-8");
            ByteBuffer bb = cs.encode(packet);
            socketChannel.write(bb);

            Util.log("[채팅서버] 송신 : " + this.response);
            // Util.log("[채팅서버] 현스레드 : " + Thread.currentThread().getName() );

            // ### wakeUp() ###
            selectionKey.interestOps(SelectionKey.OP_READ);
            selector.wakeup();

        } catch (IOException e) {
            terminate();
        }
    }

    /**
     * [ Method :: sendStatus ]
     *
     * @DES :: entry & leave 의 상태에 따라 계속적으로 클라이언트에게 인원수 전송
     * @S.E :: "method" /room/status
     */
    void sendStatus() {
        // ### /room/status ###
        response = "";
        this.response = String.format("{\"method\":\"%s\",\"rooms\":%s}",
                "/room/status",
                roomManager.roomStatus);
        this.selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * [ Method :: sendEcho ]
     *
     * @DES :: 채팅내용을 같은 방에 있는 모든 유저에게 전달
     * @IP1 :: message {String}
     * @S.E :: "method" /chat/echo
     */
    void sendEcho(String id, String message) {
        // ### /chat/echo ###
        response = "";
        response = String.format("{\"method\":\"%s\",\"id\":\"%s\",\"message\":\"%s\"}",
                "/chat/echo",
                id,
                message);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * [ Method :: terminate ]
     *
     * @DES :: Client 종료함수
     */
    void terminate() {
        try {
            Util.log("[채팅서버] 통신두절 : " + socketChannel.getRemoteAddress());
            if (room != null) Client.this.room.leaveRoom(Client.this);
            clients.remove(Client.this);
            socketChannel.close();
        } catch (IOException e) {
        }
    }
}
