import arc.ApplicationListener;
import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.game.EventType.*;
import mindustry.net.Administration;
import mindustry.plugin.Plugin;
import org.hjson.JsonObject;
import org.hjson.Stringify;

import java.io.IOException;
import java.util.concurrent.*;

import static mindustry.Vars.mods;
import static mindustry.Vars.netServer;

public class Main extends Plugin {
    public final static Fi pluginRoot = Core.settings.getDataDirectory().child("mods/BanLink/");
    public final Config config;
    public final Server server = new Server();
    public final Client client = new Client();

    public static final ExecutorService mainThread = Executors.newFixedThreadPool(3);
    public static boolean active = false;

    public Main(){
        if(!pluginRoot.child("config.hjson").exists()){
            JsonObject obj = new JsonObject();
            obj.add("mode",false, "false is set to server mode and true to client mode.");
            obj.add("address", "127.0.0.1", "Set ban sharing server/client address.");
            obj.add("port", 6871, "Set ban sharing server/client port");
            pluginRoot.child("config.hjson").writeString(obj.toString(Stringify.HJSON_COMMENTS));
        }

        Events.on(ServerLoadEvent.class, () -> {
            /*for (int a = 0; a < mods.list().size; a++) {
                if (mods.list().get(a).meta.name.equals("Essentials")) {
                    Log.err("Essentials plugin detected! This can cause conflicts due to the same function. Use one of the two.");
                    Core.app.dispose();
                    Core.app.exit();
                }
            }*/
        });

        config = new Config();
        mainThread.submit(config.mode ? client : server);
        Log.info("[BanLink] "+(config.mode ? "client" : "server")+" mode activated. "+(config.mode ? "Address: " + config.address+":"+config.port : "Port: "+config.port));

        Events.on(PlayerBanEvent.class, e -> {
            if(!active && e.player != null) {
                if (config.mode) {
                    client.share(Mode.ban, e.player.con.address, e.player.uuid);
                } else {
                    server.share(Mode.ban, e.player.con.address, e.player.uuid);
                }
            }
        });

        Events.on(PlayerIpBanEvent.class, e -> {
            if(!active) {
                Administration.PlayerInfo data = netServer.admins.findByIP(e.ip);
                if (config.mode) {
                    client.share(Mode.ban, e.ip, data != null ? data.id : "<unknown>");
                } else {
                    server.share(Mode.ban, e.ip, data != null ? data.id : "<unknown>");
                }
            }
        });

        Events.on(PlayerUnbanEvent.class, e -> {
            if(!active && e.player != null) {
                if (config.mode) {
                    client.share(Mode.unban, e.player.con.address, e.player.uuid);
                } else {
                    server.share(Mode.unban, e.player.con.address, e.player.uuid);
                }
            }
        });

        Events.on(PlayerIpUnbanEvent.class, e -> {
            if(!active) {
                Administration.PlayerInfo data = netServer.admins.findByIP(e.ip);
                if (config.mode) {
                    client.share(Mode.unban, e.ip, data != null ? data.id : "<unknown>");
                } else {
                    server.share(Mode.unban, e.ip, data != null ? data.id : "<unknown>");
                }
            }
        });

        Core.app.addListener(new ApplicationListener() {
            @Override
            public void dispose() {
                server.shutdown();
                client.shutdown();
                mainThread.shutdownNow();
            }
        });
    }
}
