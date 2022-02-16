package lab1;


import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;


public class SocketDemo {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the address");
        String[] hostAndResource = scanner.next().split("/", 2);
        System.out.println("Enter the output file name");
        String filename = scanner.next();
        
        String host = hostAndResource[0];
        String resource = "/";
        if (hostAndResource.length == 2)
            resource += hostAndResource[1];
        
        try (Socket socket = new Socket(host, 80)) {
            socket.getOutputStream().write((
                    "GET " + resource + " HTTP/1.1\n" +
                            "Host: " + host + ":80\n" +
                            "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                            "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.2 Safari/605.1.15\n\n"
            ).getBytes());
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                int remainingContentLength = -1;
                
                String line;
                while (!(line = reader.readLine()).isEmpty()) {
                    if (line.toLowerCase().startsWith("content-length"))
                        remainingContentLength = Integer.parseInt(line.split(" ")[1]);
                    
                    System.out.println(line);
                }
                
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, StandardCharsets.UTF_8))) {
                    while (remainingContentLength > 0) {
                        writer.write(reader.read());
                        remainingContentLength--;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}