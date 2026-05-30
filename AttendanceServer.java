import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.FileWriter;
import java.io.File;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class AttendanceServer {

    // List to prevent duplicate attendance from the same device
    static ArrayList<String> usedIps = new ArrayList<>();
    
    public static void main(String[] args) throws Exception {
        // Listening on 0.0.0.0 allows Dev Tunnels and Wi-Fi connections
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8000), 0);
        
        server.createContext("/", new FormHandler());
        server.createContext("/submit", new SubmitHandler());
        
        server.setExecutor(null); 
        server.start();
        
        System.out.println(" Attendance Server is running!");
        System.out.println(" Local Test: http://localhost:8000");
        System.out.println(" Waiting for connections on port 8000...");
    }

    // Handles showing the Attendance Form
    static class FormHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String html = "<html><body style='font-family: Arial; text-align: center; margin-top: 50px;'>" +
                          "<h2>Student Attendance Portal</h2>" +
                          "<form action='/submit' method='POST'>" +
                          "<b>Enter Roll Number:</b> <input type='text' name='rollno' required style='padding: 5px;'> " +
                          "<input type='submit' value='Mark Present' style='padding: 5px 10px; background: blue; color: white; border: none; cursor: pointer;'>" +
                          "</form></body></html>";
            
            t.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            byte[] responseBytes = html.getBytes();
            t.sendResponseHeaders(200, responseBytes.length);
            OutputStream os = t.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }

    // Handles the submission and file saving
    static class SubmitHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equals(t.getRequestMethod())) {
                InputStream is = t.getRequestBody();
                String formData = new String(is.readAllBytes());
                
                // Extract Roll Number
                String rollNo = formData.length() > 7 ? formData.split("=")[1] : "Unknown";
                String ipAddress = t.getRemoteAddress().getAddress().getHostAddress();
                String responseHtml;

                // Check for duplicate IP
                if (usedIps.contains(ipAddress)) {
                    responseHtml = "<html><body style='font-family: Arial; text-align: center; color: red; margin-top: 50px;'>" +
                                   "<h2> Proxy Attempt Blocked!</h2><p>This device has already marked attendance.</p></body></html>";
                } else {
                    usedIps.add(ipAddress);
                    
                    // Save data to text file
                    try (FileWriter fw = new FileWriter("attendance.txt", true)) {
                        fw.write("Roll No: " + rollNo + " | IP: " + ipAddress + " | Time: " + LocalDateTime.now() + "\n");
                        
                        // This prints the EXACT location of your file in the VS Code terminal
                        System.out.println(" DATA SAVED for Roll No: " + rollNo);
                        System.out.println(" File Location: " + new File("attendance.txt").getAbsolutePath());
                    } catch (Exception e) {
                        System.out.println("Error saving file: " + e.getMessage());
                    }

                    responseHtml = "<html><body style='font-family: Arial; text-align: center; color: green; margin-top: 50px;'>" +
                                   "<h2> Success!</h2><p>Attendance marked for Roll No: " + rollNo + ".</p></body></html>";
                }

                t.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                byte[] responseBytes = responseHtml.getBytes();
                t.sendResponseHeaders(200, responseBytes.length);
                OutputStream os = t.getResponseBody();
                os.write(responseBytes);
                os.close();
            }
        }
    }
}
