import mindustry.entities.type.Player;
import mindustry.gen.Call;
import mindustry.net.Packets;
import org.hjson.JsonObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import static mindustry.Vars.netServer;
import static mindustry.Vars.playerGroup;
import static org.hjson.JsonValue.readJSON;

public class Client extends Thread {
    public Socket socket;
    public BufferedReader is;
    public DataOutputStream os;

    public void shutdown() {
        try {
            Thread.currentThread().interrupt();
            os.close();
            is.close();
            socket.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void run() {
        Config config = new Config();

        try {
            InetAddress address = InetAddress.getByName(config.address + ":" + config.port);
            socket = new Socket(address, config.port);
            socket.setSoTimeout(2000);

            while ((!Thread.currentThread().isInterrupted())) {
                String data = is.readLine();
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
        try {
            os.writeBytes(obj.toString()+"\n");
            os.flush();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
