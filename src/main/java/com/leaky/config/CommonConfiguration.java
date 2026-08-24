package com.leaky.config;

import com.cupboard.config.CupboardConfig;
import com.cupboard.config.ICommonConfig;
import com.google.gson.JsonObject;

public class CommonConfiguration implements ICommonConfig
{
    public static CupboardConfig<CommonConfiguration> config = new CupboardConfig<>("leaky", new CommonConfiguration());

    public String  reportNotification = "NEAREST_PLAYER_LIMITED, ALL_OP";
    public boolean highlightitems     = true;
    public int     reportThreshold              = 200;
    public int     detectionThreshold           = 32;
    public int     autoremovethreshold          = 400;
    public int     autoremovenodelaythreshold   = 1500;
    public long    autoRemoveDelay              = 20 * 30;
    public boolean improveItemPerformance    = true;
    public int     entitySectionLogThreshold = 150;
    public double  wastePercent              = 0.6;

    public CommonConfiguration()
    {

    }

    public JsonObject serialize()
    {
        final JsonObject root = new JsonObject();

        // Detection settings & notification settings

        final JsonObject entryDetection = new JsonObject();
        root.add("itemsettings", entryDetection);
        entryDetection.addProperty("desc1:", "This section contains all settings for item leak detection. An item cluster is a gathering of item entities, and cluster size refers to the number of item entities in it. Item entities are the items you see floating in the world. For example, 64 cobblestone in one dropped stack counts as one item entity.");

        entryDetection.addProperty("desc4:", "Set the minimum number of item entities at which Leaky starts tracking data for an item cluster. Default: 32");
        entryDetection.addProperty("detectionThreshold", detectionThreshold);

        entryDetection.addProperty("desc3:", "Set the minimum size of an item cluster at which notifications can be sent. Default: 200");
        entryDetection.addProperty("reportThreshold", reportThreshold);

        entryDetection.addProperty("desc6:",
            "Set the chat notification type. Available options are: EVERYONE (all players), NEAREST_PLAYER (closest player), NEAREST_PLAYER_LIMITED (closest player within 200 blocks), NEAREST_OP (closest operator),ALL_OP (all operators). Multiple options can be combined, or leave empty for no notifications. Default: \"NEAREST_PLAYER_LIMITED, ALL_OP\"");
        entryDetection.addProperty("reportNotification", reportNotification);

        entryDetection.addProperty("desc2:", "Enable to make item clusters glow once they reach the reportThreshold. Default: true");
        entryDetection.addProperty("highlightitems", highlightitems);

        entryDetection.addProperty("desc5:",
            "Sets the percentage of despawns compared to newly added items at which a wasteful item cluster warning can be generated. This only applies when the threshold is exceeded over a longer period of time, with a minimum of 8 minutes. Default: 60");
        entryDetection.addProperty("wasteFactor", (int) (wastePercent * 100));

        final JsonObject subEntryRemoval = new JsonObject();
        entryDetection.add("autoremoval", subEntryRemoval);
        subEntryRemoval.addProperty("desc0:","This section contains settings for the automatic removal of oversized item clusters. Deleted clusters can be restored via command, using /leaky deletionHistory");
        subEntryRemoval.addProperty("desc1:",
            "Set the minimum cluster size at which an item cluster can be automatically removed. A cluster is observed for at least 120 seconds before it can reach this state. default: 400");
        subEntryRemoval.addProperty("autoremovethreshold", autoremovethreshold);

        subEntryRemoval.addProperty("desc2:",
            "Set the delay after which a cluster that reached the autoremovethreshold can be deleted. Default: 30 seconds");
        subEntryRemoval.addProperty("autoRemoveDelay", autoRemoveDelay / 20);

        subEntryRemoval.addProperty("desc3:",
            "Set the minimum cluster size at which an item cluster is immediately deleted. This is intended as a very high threshold to help prevent server crashes caused by extreme item entity concentrations. default: 1500");
        subEntryRemoval.addProperty("autoremovenodelaythreshold", autoremovenodelaythreshold);

        // Item performance // TODO: MOre configs?
        final JsonObject entryItemPerf = new JsonObject();
        root.add("performance", entryItemPerf);
        entryItemPerf.addProperty("desc:", "Improves item entity performance, directly reducing their potential to cause lag. default: true");
        entryItemPerf.addProperty("improveItemPerformance", improveItemPerformance);

        // Nonitementity section reporting
        final JsonObject entryEntitySection = new JsonObject();
        root.add("entitySection", entryEntitySection);
        entryEntitySection.addProperty("desc:",
            "Sets the threshold for logging concentrations of general non-item entities when more than this amount are found within a chunk section (16x16x16 blocks). Default: 150");
        entryEntitySection.addProperty("entitySectionLogThreshold", entitySectionLogThreshold);

        return root;
    }

    public void deserialize(JsonObject data)
    {
        reportNotification = data.get("itemsettings").getAsJsonObject().get("reportNotification").getAsString();
        highlightitems = data.get("itemsettings").getAsJsonObject().get("highlightitems").getAsBoolean();
        improveItemPerformance = data.get("performance").getAsJsonObject().get("improveItemPerformance").getAsBoolean();
        reportThreshold = data.get("itemsettings").getAsJsonObject().get("reportThreshold").getAsInt();
        autoremovethreshold = data.get("itemsettings").getAsJsonObject().get("autoremoval").getAsJsonObject().get("autoremovethreshold").getAsInt();
        autoremovenodelaythreshold = data.get("itemsettings").getAsJsonObject().get("autoremoval").getAsJsonObject().get("autoremovenodelaythreshold").getAsInt();
        autoRemoveDelay = data.get("itemsettings").getAsJsonObject().get("autoremoval").getAsJsonObject().get("autoRemoveDelay").getAsInt() * 20;
        detectionThreshold = data.get("itemsettings").getAsJsonObject().get("detectionThreshold").getAsInt();
        entitySectionLogThreshold = data.get("entitySection").getAsJsonObject().get("entitySectionLogThreshold").getAsInt();
        wastePercent = data.get("itemsettings").getAsJsonObject().get("wasteFactor").getAsDouble() / 100.0;
    }
}
