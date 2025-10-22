import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
    private ArrayList<HandleConnection> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server(){
        connections = new ArrayList<>();
    }

    @Override
    public void run(){
        try{
            ServerSocket server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while(!done){
                Socket client = server.accept();
                HandleConnection handler = new HandleConnection(client);
                connections.add(handler);
                pool.execute(handler);
            }
        }catch (IOException e) {
            shutdown();

        }
    }

    public void broadcast(String message){
        for (HandleConnection hc : connections){
            if (hc != null){
                hc.sendMessage(message);
            }
        }
    }

    public void shutdown(){
        try{
            done = true;
            pool.shutdown();
            if(!server.isClosed()){
                server.close();
            }
            for (HandleConnection hc : connections){
                hc.shutdown();
            }    
        } catch (IOException e) {
            //ignore
        }
    }

    class HandleConnection implements Runnable{
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String name;

        public HandleConnection(Socket client){
            this.client = client;
        }

        @Override
        public void run(){
            try{
                out = new PrintWriter(client.getOutputStream(),
                        true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("What would you like to name your chatbot: ");
                name = in.readLine();
                System.out.println(name + " entered the chat!");
                broadcast(name + " joined the chat!");
                String message;
                while((message = in.readLine()) != null){
                    if(message.startsWith("/name")){
                        String[] splitMessage = message.split(" ",2);
                        if (splitMessage.length == 2){
                            broadcast(name + "renamed themselves to " + splitMessage[1]);
                            System.out.println(name + "renamed themselves to " + splitMessage[1]);
                            name = splitMessage[1];
                            out.println("Successfully changed name to " + name);
                        } else {
                           out.println("No name provided!");
                        }
                    }else if (message.startsWith("/quit")){
                        broadcast(name + " left the chat!");
                        shutdown();
                    }else{
                        broadcast(name + ": " + message);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }

        public void sendMessage(String message){
            out.println(message);
        }

        public void shutdown(){
            try{
                in.close();
                out.close();
                if (!client.isClosed()){
                    client.close();
                }
            }catch (IOException e)   {
                //ignore
            }
        }
    }

    public static void main(String[] args){
        Server server = new Server();
        server.run();
    }
}
