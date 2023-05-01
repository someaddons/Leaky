package com.leaky.config;

import com.google.gson.JsonObject;

public class CommonConfiguration
{
    public int reportInterval = 60 * 3;
    public String chatnotification = "PLAYER";
    public boolean highlightitems = true;
    public int reportThreshold = 80;
    public int autoremovethreshold = 120;

    public CommonConfiguration()
    {

    }

    public JsonObject serialize()
    {
        final JsonObject root = new JsonObject();

        final JsonObject entry3 = new JsonObject();
        entry3.addProperty("desc:", "Set the amount of seconds between repeated notifications: default: 180");
        entry3.addProperty("reportInterval", reportInterval);
        root.add("reportInterval", entry3);

        final JsonObject entry4 = new JsonObject();
        entry4.addProperty("desc:", "Set the chat notification type, one of these: PLAYER(closest player), EVERYONE(all players), NONE. default: PLAYER");
        entry4.addProperty("chatnotification", chatnotification);
        root.add("chatnotification", entry4);

        final JsonObject entry5 = new JsonObject();
        entry5.addProperty("desc:", "Choose if leaking item entity should be glowing, default: true");
        entry5.addProperty("highlightitems", highlightitems);
        root.add("highlightitems", entry5);

        final JsonObject entry6 = new JsonObject();
        entry6.addProperty("desc:", "Set the min amount of stacked items being reported, default: 80");
        entry6.addProperty("reportThreshold", reportThreshold);
        root.add("reportThreshold", entry6);

        final JsonObject entry7 = new JsonObject();
        entry7.addProperty("desc:", "Set the amount of stacked items being automatically removed, default: 120");
        entry7.addProperty("autoremovethreshold", autoremovethreshold);
        root.add("autoremovethreshold", entry7);

        return root;
    }

    public void deserialize(JsonObject data)
    {
        if (data == null)
        {
            com.leaky.Leaky.LOGGER.error("Config file was empty!");
            return;
        }

        reportInterval = data.get("reportInterval").getAsJsonObject().get("reportInterval").getAsInt();
        chatnotification = data.get("chatnotification").getAsJsonObject().get("chatnotification").getAsString();
        highlightitems = data.get("highlightitems").getAsJsonObject().get("highlightitems").getAsBoolean();
        reportThreshold = data.get("reportThreshold").getAsJsonObject().get("reportThreshold").getAsInt();
        autoremovethreshold = data.get("autoremovethreshold").getAsJsonObject().get("autoremovethreshold").getAsInt();
    }
}
