import arc.ApplicationListener;
import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.game.EventType.*;
import mindustry.plugin.Plugin;
import org.hjson.JsonObject;

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

    public Main(){
        if(!pluginRoot.child("config.hjson").exists()){
            JsonObject obj = new JsonObject();
            obj.add("mode",true, "false is set to server mode and true to client mode.");
            obj.add("address", "127.0.0.1", "Set ban sharing server/client address.");
            obj.add("port", 6871, "Set ban sharing server/client port");
        }

        Events.on(ServerLoadEvent.class, () -> {
            for (int a = 0; a < mods.list().size; a++) {
                if (mods.list().get(a).meta.name.equals("Essentials")) {
                    Log.err("Essentials plugin detected! This can cause conflicts due to the same function. Use one of the two.");
                    Core.app.dispose();
                    Core.app.exit();
                }
            }
        });

        config = new Config();
        mainThread.submit(config.mode ? server : client);

        Events.on(PlayerBanEvent.class, e -> {
            if(config.mode){
                server.share(Mode.ban, e.player.con.address, e.player.uuid);
            } else {
                client.share(Mode.ban, e.player.con.address, e.player.uuid);
            }
        });

        Events.on(PlayerIpBanEvent.class, e -> {
            if(config.mode){
                server.share(Mode.ban, e.ip, netServer.admins.findByIP(e.ip).id);
            } else {
                client.share(Mode.ban, e.ip, netServer.admins.findByIP(e.ip).id);
            }
        });

        Events.on(PlayerUnbanEvent.class, e -> {
            if(config.mode){
                server.share(Mode.unban, e.player.con.address, e.player.uuid);
            } else {
                client.share(Mode.unban, e.player.con.address, e.player.uuid);
            }
        });

        Events.on(PlayerIpUnbanEvent.class, e -> {
            if(config.mode){
                server.share(Mode.unban, e.ip, netServer.admins.findByIP(e.ip).id);
            } else {
                client.share(Mode.unban, e.ip, netServer.admins.findByIP(e.ip).id);
            }
        });

        Core.app.addListener(new ApplicationListener() {
            @Override
            public void dispose() {
                server.shutdown();
                client.shutdown();
            }
        });
    }
}
