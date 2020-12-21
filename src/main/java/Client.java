import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Playerc;
import mindustry.net.Packets;
import org.hjson.JsonObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static mindustry.Vars.netServer;
import static org.hjson.JsonValue.readJSON;

public class Client extends Thread {
    public Socket socket;
    public BufferedReader is;
    public DataOutputStream os;

    public void shutdown(Exception e) {
        try {
            Thread.currentThread().interrupt();
            if(os != null) os.close();
            if(is != null) is.close();
            if(socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void run() {
        Config config = new Config();

        try {
            InetAddress address = InetAddress.getByName(config.address);
            socket = new Socket(address, config.port);
            is = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            os = new DataOutputStream(socket.getOutputStream());

            while ((!Thread.currentThread().isInterrupted())) {
                String data = is.readLine();
                Main.active = true;
                JsonObject obj = readJSON(data).asObject();

                Mode type = Mode.valueOf(obj.get("type").asString());
                String ip = obj.get("ip").asString();
                String uuid = obj.get("uuid").asString();

                switch (type) {
                    case ban -> {
                        if (!uuid.equals("<unknown>")) netServer.admins.banPlayerID(uuid);
                        if (!ip.equals("<unknown>")) netServer.admins.banPlayerIP(ip);
                    }
                    case unban -> {
                        if (!uuid.equals("<unknown>")) netServer.admins.unbanPlayerID(uuid);
                        if (!ip.equals("<unknown>")) netServer.admins.unbanPlayerIP(ip);
                    }
                }

                for(Playerc p : Groups.player){
                    if(netServer.admins.isIDBanned(p.uuid()) || netServer.admins.isIPBanned(p.con().address)){
                        Call.kick(p.con(), Packets.KickReason.banned);
                    }
                }

                Main.active = false;
            }
        } catch (IOException e) {
            shutdown(e);
        }
    }

    public void share(Mode type, String ip, String uuid) {
        JsonObject obj = new JsonObject();
        obj.add("type", type.toString());
        obj.add("ip", ip != null ? ip : "<unknown>");
        obj.add("uuid", uuid != null ? uuid : "<unknown>");
        try {
            os.writeBytes(obj.toString()+"\n");
            os.flush();
        } catch (IOException e){
            shutdown(e);
        }
    }
}
