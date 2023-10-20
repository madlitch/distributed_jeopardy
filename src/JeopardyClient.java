// Massimo Albanese
// SOFE4790U
// Distributed Systems

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.net.Socket;
import java.util.Scanner;

public class JeopardyClient {
    static final String CLIENT_KEY_WORD_INPUT = "CLIENT-INPUT";
    static final int HTTP_PORT = 3500;

    public static void main(String argv[]) throws Exception {
        try {
            Scanner scanner = new Scanner(System.in);
            Socket echo = new Socket("localhost", HTTP_PORT);
            DataInputStream is = new DataInputStream(echo.getInputStream());
            DataOutputStream os = new DataOutputStream(echo.getOutputStream());

            System.out.println("Welcome to Jeopardy!");
            System.out.println("What is your name?");
            String name = scanner.next();
            os.writeUTF(name);
            os.flush();

            while (true) {
                String s = is.readUTF();
                if (s.equals(CLIENT_KEY_WORD_INPUT)) {
                    // Server asks client for input using special keyword
                    String input = scanner.next();
                    os.writeUTF(input);
                } else {
                    // Output message from server
                    System.out.println(s);
                }
            }
        } catch (EOFException e) {
            System.out.println("Connection Closed.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

