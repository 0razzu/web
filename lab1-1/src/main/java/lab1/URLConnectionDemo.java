package lab1;


import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class URLConnectionDemo {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the address");
        String addr = scanner.next();
        System.out.println("Enter the output file name");
        String filename = scanner.next();
        
        try {
            URL url = new URL("http://" + addr);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                    "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.2 Safari/605.1.15");
            
            Map<String, List<String>> headerFields = connection.getHeaderFields();
            headerFields.get(null).forEach(System.out::print);
            System.out.println();
            for (Map.Entry<String, List<String>> entry: headerFields.entrySet())
                if (entry.getKey() != null) {
                    System.out.print(entry.getKey() + ": ");
                    entry.getValue().forEach(System.out::print);
                    System.out.println();
                }
            
            int remainingContentLength = connection.getContentLength();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(filename, StandardCharsets.UTF_8))) {
                while (remainingContentLength > 0) {
                    writer.write(reader.read());
                    remainingContentLength--;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
