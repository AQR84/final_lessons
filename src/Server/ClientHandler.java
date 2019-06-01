package Server;

import javafx.application.Application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler {

    Socket socket = null;
    DataInputStream in;
    DataOutputStream out;
    MainServer server;

    public String getNick() {
        return nick;
    }

    String nick;
    String login;

    long startTime;
    long timeSpent;
    long CLOSEMLS = 1200000;

    public ClientHandler(MainServer server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        startTime = System.currentTimeMillis();
                        while (true) {
                            timeSpent = System.currentTimeMillis() - startTime;

                            if (timeSpent>CLOSEMLS) {
                                out.writeUTF("/serverClosed");
                            }
                            String str = in.readUTF();
                            if(str.startsWith("/auth")) {
                                String[] tokens = str.split(" ");
                                String newNick = AuthService.getNickByLoginAndPass(tokens[1], tokens[2]);
                                if(newNick != null) {
                                    startTime = System.currentTimeMillis();
                                    if(!server.isNickBusy(newNick)) {
                                        sendMsg("/authok");
                                        nick = newNick;
                                        login = tokens[1];
                                        server.subscribe(ClientHandler.this);
                                        break;
                                    } else {
                                        sendMsg("Учетная запись уже используется!");
                                    }
                                } else {
                                    sendMsg("Неверный логин/пароль!");
                                }
                            }
                        }

                        while (true) {

                            if (timeSpent>CLOSEMLS) {
                                out.writeUTF("/serverClosed");
                            }

                            String str = in.readUTF();
                            if(str.startsWith("/")) {
                                startTime = System.currentTimeMillis();
                                if(str.equals("/end")) {
                                    out.writeUTF("/serverClosed");
                                }
                                if(str.startsWith("/w ")) {
                                    String[] tokens = str.split(" ",3);
                                    server.sendPersonalMsg(ClientHandler.this, tokens[1], tokens[2]);
                                }
                                if(str.startsWith("/blacklist ")) {
                                    String[] tokens = str.split(" ");
                                    AuthService.addBlackList(nick,tokens[1]);
                                    sendMsg("Вы добавили пользователя " + tokens[1] + " в черный список");
                                }
                            } else {
                                startTime = System.currentTimeMillis();
                                server.broadcastMsg(ClientHandler.this,nick + ": " + str);
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        server.unsubscribe(ClientHandler.this);
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
