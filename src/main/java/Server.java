import arc.struct.Array;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
import mindustry.net.Packets;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static mindustry.Vars.netServer;
import static mindustry.Vars.playerGroup;
import static org.hjson.JsonValue.readJSON;

public class Server implements Runnable {
    public Array<Service> list = new Array<>();
    public ServerSocket serverSocket;

    @Override
    public void run() {
        Config config = new Config();
        try {
            this.serverSocket = new ServerSocket(config.port);
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                Service service = new Service(socket);
                service.start();
                list.add(service);
            }
        }  catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class Service extends Thread{
        public BufferedReader in;
        public DataOutputStream os;
        public Socket socket;

        String ip;

        public Service(Socket socket) throws IOException{
            this.socket = socket;
            ip = socket.getInetAddress().toString();
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            os = new DataOutputStream(socket.getOutputStream());
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String data = in.readLine();
                    JsonObject obj = readJSON(data).asObject();

                    Mode type = Mode.valueOf(obj.get("type").asString());
                    String ip = obj.get("ip").asString();
                    String uuid = obj.get("uuid").asString();

                    switch (type) {
                        case ban:
                            if(!uuid.equals("<unknown>")) netServer.admins.banPlayerID(uuid);
                            if(!ip.equals("<unknown>")) netServer.admins.banPlayerIP(ip);
                            break;
                        case unban:
                            if(!uuid.equals("<unknown>")) netServer.admins.unbanPlayerID(uuid);
                            if(!ip.equals("<unknown>")) netServer.admins.unbanPlayerIP(ip);
                            break;
                    }

                    for(Player p : playerGroup.all()){
                        if(netServer.admins.isIDBanned(p.uuid) || netServer.admins.isIPBanned(p.con.address)){
                            Call.onKick(p.con, Packets.KickReason.banned);
                        }
                    }

                    for (Service s : list) {
                        s.os.writeBytes(data+"\n");
                        s.os.flush();
                    }
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }

        public void shutdown() {
            try {
                os.close();
                in.close();
                socket.close();
                list.remove(this);
            } catch (Exception ignored) {
            }
        }
    }

    public void shutdown(){
        try {
            for(Service s : list) {
                s.shutdown();
            }

            if (serverSocket != null) {
                Thread.currentThread().interrupt();
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void share(Mode type, String ip, String uuid) {
        JsonObject obj = new JsonObject();
        obj.add("type", type.toString());
        obj.add("ip", ip);
        obj.add("uuid", uuid);

        for(Service s : list){
            try {
                s.os.writeBytes(obj.toString()+"\n");
                s.os.flush();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}
