package model;

import util.Define;
import util.Util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    public ExecutorService executorService;
    public ServerSocketChannel serverSocketChannel;
    public Selector selector;
    public List<Client> clients;
    public RoomManager roomManager;

    public Server() {
        this.clients = new Vector<>();
        this.roomManager = new RoomManager(clients);
    }

    /**
     * [ Method :: startServer ]
     *
     * @DES :: 서버실행함수
     * @S.E :: connect()로 연결
     * */
    public void startServer() {

        // ### 가용한 프로세서만큼 스레드 생성 ###
        executorService = Executors.newFixedThreadPool( Runtime.getRuntime().availableProcessors() ); // 4
        // executorService = Executors.newFixedThreadPool( 16 ); // 16

        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();

            // ### 넌블로킹설정 ###
            serverSocketChannel.configureBlocking(false);

            // ### bind(port) ###
            serverSocketChannel.bind( new InetSocketAddress(Define.PORT) );

            // ### register() ###
            serverSocketChannel.register( selector, SelectionKey.OP_ACCEPT );
            System.out.println("[채팅서버] 서버실행");

            // 스레드생성후 새로운소켓 계속감시
            connect();

        } catch (IOException e) {
            System.out.println("[채팅서버] 같은 포트의 서버가 이미 실행중 일 수 있습니다.");
            e.printStackTrace();
        }
    }

    /**
     * [ Method :: stopServer ]
     *
     * @DES :: 서버종료함수
     * @S.E :: 없음
     * */
    public void stopServer() {
        try {
            Iterator<Client> iterator = clients.iterator();
            while(iterator.hasNext()) {
                Client client = iterator.next();
                client.socketChannel.close();
                iterator.remove();
            }
            if(serverSocketChannel != null && serverSocketChannel.isOpen()) {
                serverSocketChannel .close();
            }
            if(executorService != null && executorService.isShutdown()) {
                executorService.shutdown();
            }
            Util.log("[채팅서버] 서버종료");
        } catch (IOException e) {}
    }

    /**
     * [ Method :: connect ]
     *
     * @DES :: 스레드생성후 새로운소켓 계속감시 (스레드풀)
     * @S.E :: accept()를 처리하는 함수
     * */
    public void connect() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {

                        // ### select() ###
                        int keyCount = selector.select();
                        if(keyCount == 0) { continue; }

                        Set<SelectionKey> selectionKeys = selector.selectedKeys();
                        Iterator<SelectionKey> iterator = selectionKeys.iterator();

                        while(iterator.hasNext()) {
                            SelectionKey selectionKey = iterator.next();

                            // 연결수락작업을 경우 (accept)
                            if(selectionKey.isAcceptable()) {
                                accept(selectionKey);
                            }
                            // 읽기작업일 경우 (read)
                            else if(selectionKey.isReadable()) {
                                  Client client = (Client) selectionKey.attachment();
                                 client.receive();
                            }
                            // 쓰기작업일 경우 (write)
                            else if(selectionKey.isWritable()) { //
                                Client client = (Client) selectionKey.attachment();
                                client.send();
                            }

                            // 처리완료된 SelectionKey 제거
                            iterator.remove();
                        }
                    } catch (IOException e) {
                        if(serverSocketChannel.isOpen()) stopServer();
                        break;
                    }
                }
            }
        };
        executorService.submit(runnable);
    }

    /**
     * [ Method :: accept ]
     *
     * @DES :: 소켓연결을 받는 함수
     * @IP1 :: selectionKey {SelectionKey}
     * */
    void accept(SelectionKey selectionKey) {
        try {
            // ### accept ###
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
            SocketChannel socketChannel = serverSocketChannel.accept();

            // (확인할수있는데이터)
            InetSocketAddress isa = (InetSocketAddress) socketChannel.getRemoteAddress();
            Util.log("[채팅서버] 새로운 클라이언트접속 " + isa.getHostName() );
            Util.log("[채팅서버] 현스레드 : " + Thread.currentThread().getName() );


            Client client = new Client(executorService, socketChannel, clients, roomManager, selector );
            clients.add(client);

            Util.log("[채팅서버] 현재접속 클라이언트 수 : " + clients.size());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
