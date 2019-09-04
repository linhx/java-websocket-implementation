/**
 * @author linhnd
 */
package org.lnd.chat.server;

import java.io.IOException;
import java.net.ServerSocket;

import org.apache.log4j.BasicConfigurator;

public class Main {
    public static void main(String[] args) {
        BasicConfigurator.configure();
        ServerSocket server = null;
        try {
            server = new ServerSocket(8080);
            while (true) {
                System.out.println("Waiting...");
                WebSocket s = new WebSocket(server.accept());
                System.out.println("Accept...");
                Thread socket = new Thread(s);
                socket.start();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                server.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

}
