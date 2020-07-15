import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.nio.charset.StandardCharsets;

public class Config {
    public final boolean mode;
    public final String address;
    public final int port;

    public Config(){
        JsonObject obj = JsonValue.readHjson(Main.pluginRoot.child("config.hjson").readString("UTF-8")).asObject();
        mode = obj.getBoolean("mode", true);
        address = obj.getString("address", "127.0.0.1");
        port = obj.getInt("port", 6871);
    }
}
