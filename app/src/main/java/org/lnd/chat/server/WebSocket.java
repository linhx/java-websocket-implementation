/**
 * implement rfc6455 Data Framing
 * @author linhnd
 */
package org.lnd.chat.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Properties;

import org.apache.log4j.Logger;

public class WebSocket implements Runnable {

    private Socket socket;
    private Logger logger = Logger.getLogger(WebSocket.class);

    public WebSocket(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            this.handshake();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        }
        int c;
        InputStream in = null;
        try {
            in = this.socket.getInputStream();
            while ((c = in.read()) != -1) {
//                byte b1 = (byte) c;
//                boolean fin = b1 >> 8 != 0;
//                boolean rsv1 = (b1 & 0x40) != 0; // 0100 0000
//                boolean rsv2 = (b1 & 0x20) != 0; // 0010 0000
//                boolean rsv3 = (b1 & 0x10) != 0; // 0001 0000
//                int optCode = b1 & 15; // 0000 1111

                byte b2 = (byte) in.read();
                boolean isMask = b2 >> 8 != 0;
                long length = b2 & 127; // 0111 1111
                if (length == 126) {
                    byte[] bLength2 = new byte[2];
                    in.read(bLength2);
                    length = ByteBuffer.wrap(new byte[] { 0, 0, bLength2[0], bLength2[1] }).getInt();
                } else if (length == 127) {
                    byte[] bLength3 = new byte[8];
                    in.read(bLength3);
                    length = ByteBuffer.wrap(bLength3).getInt();
                }
                // TODO what if isMask = false
                if (isMask) {
                    byte[] b25 = new byte[4];
                    in.read(b25);
                    String result = readUtf8Message(in, length, b25);
                    logger.info("client: " + result);
                }
                // TODO send message back to WebSocket
            }
        } catch (IOException e2) {
            e2.printStackTrace();
        } finally {
            try {
                this.socket.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Read UTF-8 message from InputStream
     * <br />
     * (implement rfc3629 - UTF-8 definition)
     * 
     * @param in {@link InputStream}
     * @param length the message length
     * @param b25 the masking key
     * @return {@link String} the message
     * @throws IOException
     */
    private static String readUtf8Message (InputStream in, long length, byte[] b25) throws IOException {
        StringBuilder result = new StringBuilder();
        long len = length;
        for (int i = 0; i < len; i++) {
            byte bc1 = (byte) (in.read() ^ b25[i % 4]);
            if (bc1 >> 8 == 0) {
                result.append((char) bc1);
                continue;
            }
            // two byte character format is 110xxxxx 10xxxxxx
            // 110xxxxx > 5 == -2
            if (bc1 >> 5 == -2) {
                i++;
                byte bc2 = (byte) (in.read() ^ b25[i % 4]);
                byte[] x = { bc1, bc2 };
                result.append(new String(x));
                continue;
            }
            // three bytes character format is 1110xxxx 10xxxxxx 10xxxxxx
            // 1110xxxx >> 4 = -6
            if (bc1 >> 4 == -6) {
                i++;
                byte bc2 = (byte) (in.read() ^ b25[i % 4]);
                i++;
                byte bc3 = (byte) (in.read() ^ b25[i % 4]);
                result.append(new String(new byte[] { bc1, bc2, bc3 }));
                continue;
            }
            // four bytes character format is 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
            // 11110xxx >> 3 == -14
            if (bc1 >> 3 == -14) {
                i++;
                byte bc2 = (byte) (in.read() ^ b25[i % 4]);
                i++;
                byte bc3 = (byte) (in.read() ^ b25[i % 4]);
                i++;
                byte bc4 = (byte) (in.read() ^ b25[i % 4]);
                result.append(new String(new byte[] { bc1, bc2, bc3, bc4 }));
                continue;
            }
        }
        return result.toString();
    }

    /**
     * implement rfc6455 - opening handshake
     * 
     * @throws IOException
     * @throws NoSuchAlgorithmException 
     */
    public void handshake() throws IOException, NoSuchAlgorithmException {
        System.out.println("Handshaking...");
        InputStream in = this.socket.getInputStream();
        byte[] bytes = new byte[1024];
        in.read(bytes);

        String accept = new String(bytes);
        System.out.println(accept);

        String[] clientHeader = accept.split("\r\n");

        Properties p = new Properties();
        String[] pair;
        for (int i = 0; i < clientHeader.length; i++) {
            if (!clientHeader[i].contains(":"))
                continue;
            pair = clientHeader[i].split(": ");
            p.put(pair[0], pair[1]);
        }

        String secAccept = sha1base64(p.getProperty("Sec-WebSocket-Key") + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11");

        String handshake = "HTTP/1.1 101 Switching Protocols\r\n" +
                           "Upgrade: websocket\r\n" + 
                           "Connection: Upgrade\r\n" + 
                           "Sec-WebSocket-Accept: " + secAccept + "\r\n\r\n";

        OutputStream out = this.socket.getOutputStream();
        byte[] res = handshake.getBytes("UTF-8");
        out.write(res, 0, res.length);
        out.flush();
    }

    private static String sha1base64(String str) throws NoSuchAlgorithmException {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
            return Base64.getEncoder().encodeToString(md.digest(str.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw e;
        }
    }
}
