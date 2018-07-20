# Java
## Chatting Socket Server
<div class="pull-right">  업데이트 :: 2018.07.19 </div><br>

---

<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->
<!-- code_chunk_output -->

* [Java](#java)
	* [Chatting Socket Server](#chatting-socket-server)
		* [Java Socket Server](#java-socket-server)
				* [구현환경](#구현환경)
				* [라이브러리](#라이브러리)
				* [실행방법](#실행방법)
		* [Blocking vs NonBlocking 비교](#blocking-vs-nonblocking-비교)
				* [Blocking](#blocking)
				* [NonBlocking](#nonblocking)
		* [멀티룸구조](#멀티룸구조)
		* [추가기능](#추가기능)

<!-- /code_chunk_output -->

### Java Socket Server

##### 구현환경

- Mac OS X
- Java 1.8
- IntelliJ

##### 라이브러리

- [json-parser]()

##### 실행방법

- lib 폴더에 있는 라이브러리 연동
- Main.java 실행

### Blocking vs NonBlocking 비교

이번 채팅예제는 블로킹방식과 넌블로킹방식 2가지로 만들어 보았습니다.
만드는 과정에서 느낀 차이점입니다.

##### Blocking

블로킹방식은 언제 데이터가 보내질지 모르기때문에 "accept()"와 "read()"에서 블로킹됩니다.

그래서 스레드를 할당해야 합니다. 이런 방식을 해결하기 위해서 스레드풀을 사용하지만,

연결된 client마다 "read()"에 스레드를 할당해야 한다는 점은 변하지 않습니다.

##### NonBlocking

넌블로킹방식은 "conenct()", "accept()", "read()", "write()"에 블로킹이 되지 않습니다.

클라이언트의 연결요청이 없다면 즉시 null을 리턴합니다.

그래서 Selector를 사용하는데, Selector는 하나의 작업스레드로 처리할수 있도록 멀티플렉서역할을 합니다 (싱글스레드가 아닌 스레드풀 사용도 가능)

작업스레드가 블로킹되지 않기때문에 적은수의 스레드로 고속으로 처리할 수 있습니다.


### 멀티룸구조

RoomManager

```
RoomManager -> Room(A)  -> Client(A)
                        -> Client(B)
                        -> Client(C)
            -> Room(B)  -> Client(D)
                        -> Client(E)
                        -> Client(F)
            -> Room(C)  -> Client(G)
                        -> Client(H)
                        -> Client(I)
```

ClientManager

```
ClientManager -> Client(A)
              -> Client(B)
              -> Client(C)
              -> Client(D)
              -> Client(E)
              -> Client(F)
              -> Client(G)
              -> Client(H)
              -> Client(I)
```

### 추가예정기능

- 채팅방 비밀번호 기능 (서버에서비교처리)
- 방장기능 강퇴등등
- 실시간 인원수 확인기능
- 블로킹서버와 넌블로킹서버의 비교


---

**Created by SDM**

==작성자 정보==

e-mail :: jm921106@naver.com || jm921106@gmail.com

github :: https://github.com/moonscoding
