import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

public class MyServer {

    public static void createDatabase() {
        try {
            File myObj = new File("database.txt");
            if (myObj.createNewFile()) {
                System.out.println("Database created: " + myObj.getName());
            } else {
                System.out.println("Database already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred whilst creating Database.");
            e.printStackTrace();
        }
    }

    public static void writeToDatabase(String tx) {
        // don't add duplicate entries
        if (getEntireAgenda().contains(tx))
            return;
        try {
            FileWriter myWriter = new FileWriter("database.txt", true);
            myWriter.write(tx + "\n");
            myWriter.close();
            System.out.println("Successfully wrote to the Database.");
        } catch (IOException e) {
            System.out.println("An error occurred whilst writing to the Database.");
            e.printStackTrace();
        }
    }

    public static List<String> searchDatabase(String qry) {
        try {
            File file = new File("database.txt");
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            StringBuffer sb = new StringBuffer();

            String line;
            List<String> output = new ArrayList<String>();
            while ((line = br.readLine()) != null) {
                String DBEntry = line.substring(0, line.toString().indexOf(",")).trim().toLowerCase();
            
                if (DBEntry.contains(qry.toLowerCase().trim())) {
                    output.add(line);
                }

            }
            fr.close();
            br.close();

            System.out.print(output);

            return output;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public static List<String> getEntireAgenda() {
        try {
            File file = new File("database.txt");
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            StringBuffer sb = new StringBuffer();

            String line;
            List<String> output = new ArrayList<String>();
            while ((line = br.readLine()) != null) {
                output.add(line);
            }
            fr.close();
            br.close();

            return output;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public static boolean deleteFromDatabase(String lineToRemove) {
        File inputFile = new File("database.txt");
        File tempFile = new File("temp.txt");
        Boolean out = false;
        try {

            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

            String currentLine;

            System.out.println("DELETEING THE LINE: ");
            System.out.println(lineToRemove);

            while ((currentLine = reader.readLine()) != null) {

                if (currentLine.equals(lineToRemove)) {
                    out = true;
                    continue;
                }
                writer.write(currentLine + System.getProperty("line.separator"));
            }
            writer.close();
            reader.close();
            tempFile.renameTo(inputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return out;
    }

    static String getServerTime(int GMT) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        String gmtZone;
        if (GMT > 0)
            gmtZone = "+" + String.valueOf(GMT);
        else
            gmtZone = String.valueOf(GMT);

        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT" + gmtZone));
        return dateFormat.format(calendar.getTime());
    }

    public static void main(String[] args) throws Exception {
        Random r = new Random();
        // start server on random port
        int port = r.nextInt(2000 - 1000) + 1 + 1000;
        port = 1713;
        ServerSocket serverSocket = new ServerSocket(port);
        System.err.println("Server running on : " + port);

        // repeatedly wait for connections, and process
        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.err.println("Client Connected");

            // create reader and writer to clientSocket
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

            String SearchQry = "";
            // interpret GET request
            String req;
            while ((req = in.readLine()) != null) {
                System.out.println(req);
                if (req.contains("GET")) {
                    req = req.substring(6);
                    req = req.split(" ")[0];
                    System.out.println(req);
                    String reqParts[] = req.split("&");
                    if (!reqParts[0].split("=")[0].equals("operation"))
                        continue;

                    // add calendar entry
                    if (reqParts[0].split("=")[1].equals("add")) {
                        // clean up text
                        String name = URLDecoder.decode(reqParts[1].split("=")[1], StandardCharsets.UTF_8.toString());
                        String datetime = URLDecoder.decode(reqParts[2].split("=")[1],
                                StandardCharsets.UTF_8.toString());
                        String description = URLDecoder.decode(reqParts[3].split("=")[1],
                                StandardCharsets.UTF_8.toString());
                        writeToDatabase(name + ", " + datetime + ", " + description);
                        SearchQry = "";
                    }

                    // remove calendar entry
                    if (reqParts[0].split("=")[1].equals("remove")) {
                        // clean up text
                        String deleteme = URLDecoder.decode(reqParts[1]);
                        deleteFromDatabase(deleteme);
                    }

                    // search calendar entry
                    if (reqParts[0].split("=")[1].equals("search")) {
                        // clean up text
                        SearchQry = URLDecoder.decode(reqParts[1]);
                        SearchQry = SearchQry.split("=")[1];
                    }

                }
                if (req.isEmpty()) {
                    break;
                }
            }

            // set headers
            out.write("HTTP/1.0 200 OK\r\n");
            // get server time (server situated in GMT+2)
            out.write("Date: " + getServerTime(2) + "\r\n");
            out.write("Content-Type: text/html\r\n");
            out.write("\r\n");

            // start with page content
            out.write("<TITLE>AGENDA | COS332 PRAC4</TITLE>");
            out.write("<h2>My Agenda</h2>");

            // SEARCH
            out.write("<form method='get'>");
            out.write("<h4>Search</h4>");
            out.write("<input type='hidden' name='operation' value='search'></input>");
            out.write(
                    "<label for='name'>Name of Person</label><br/><input required type='text' id='name' name='name'>  ");
            out.write("<input type='submit' value='Search Agenda'>");
            out.write("</form>");

            out.write("<h4>Agenda</h4>");
            // output agenda
            out.write("<ol>");
            if (SearchQry != "") {
                if (searchDatabase(SearchQry) != null && searchDatabase(SearchQry).size() > 0)
                    for (int i = 0; i < searchDatabase(SearchQry).size(); i++) {
                        out.write("<li>" + searchDatabase(SearchQry).get(i) + " <a href='/?operation=remove&"
                                + searchDatabase(SearchQry).get(i) + "'>[Remove Entry]</a></li> ");
                    }
                else 
                    out.write("<li>No results found</li>");
            } else {
                if (getEntireAgenda() != null)
                    for (int i = 0; i < getEntireAgenda().size(); i++) {
                        out.write("<li>" + getEntireAgenda().get(i) + " <a href='/?operation=remove&"
                                + getEntireAgenda().get(i) + "'>[Remove Entry]</a></li> ");
                    }
            }
            out.write("</ol>");

            out.write("<form>");
            out.write("<input type='submit' value='Clear Search'>");
            out.write("</form>");

            // add to agenda frontend
            out.write("<form method='get'>");
            out.write("<h4>Add meeting to Agenda</h4>");
            out.write("<input type='hidden' name='operation' value='add'></input>");
            out.write(
                    "<label for='name'>Name of Person</label><br/><input required type='text' id='name' name='name'><br/><br/>");
            out.write(
                    "<label for='datetime'>Date & Time:</label><br/><input required type='datetime-local' id='datetime' name='datetime'><br/><br/>");
            out.write(
                    "<label for='description'>Description:</label><br/><input required type='text' id='description' name='description'><br/><br/>");
            out.write("<input type='submit' value='Add To Agenda'>");
            out.write("</form>");

            // close everything
            System.err.println("Client Disconnected");
            out.close();
            in.close();
            clientSocket.close();
        }
    }
}