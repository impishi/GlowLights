package impishi.imsag.command;

import impishi.imsag.GlowLights;
import impishi.imsag.config.GlowConfig;
import impishi.imsag.light.LightManager;
import impishi.imsag.util.Scheduler;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GlowCommand implements CommandExecutor, TabCompleter {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final String P = "&6ɢʟᴏᴡ&eʟɪɢʜᴛꜱ &f» &r";

    private final GlowLights plugin;
    private final LightManager lights;
    private final GlowConfig cfg;

    public GlowCommand(GlowLights plugin, LightManager lights, GlowConfig cfg) {
        this.plugin = plugin;
        this.lights = lights;
        this.cfg = cfg;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "toggle" -> {
                if (!(sender instanceof Player p)) {
                    msg(sender, "&cᴘʟᴀʏᴇʀꜱ ᴏɴʟʏ");
                    return true;
                }
                if (!p.hasPermission("glowlights.toggle")) {
                    msg(sender, "&cɴᴏ ᴘᴇʀᴍɪꜱꜱɪᴏɴ");
                    return true;
                }
                lights.toggle(p);
                msg(p, lights.isOff(p) ? "&cʟɪɢʜᴛꜱ ᴏꜰꜰ" : "&aʟɪɢʜᴛꜱ ᴏɴ");
            }
            case "reload" -> {
                if (!sender.hasPermission("glowlights.reload")) {
                    msg(sender, "&cɴᴏ ᴘᴇʀᴍɪꜱꜱɪᴏɴ");
                    return true;
                }
                plugin.reloadAll();
                msg(sender, "&aʀᴇʟᴏᴀᴅᴇᴅ");
            }
            case "status" -> {
                if (!sender.hasPermission("glowlights.status")) {
                    msg(sender, "&cɴᴏ ᴘᴇʀᴍɪꜱꜱɪᴏɴ");
                    return true;
                }
                msg(sender, "&7ꜱᴛᴀᴛᴜꜱ");
                msg(sender, "&7ᴇɴᴀʙʟᴇᴅ: &f" + cfg.enabled());
                msg(sender, "&7ᴇᴍɪᴛᴛᴇʀꜱ: &f" + lights.emitterCount());
                msg(sender, "&7ᴠɪᴇᴡᴇʀꜱ: &f" + lights.viewerCount());
                msg(sender, "&7ʟɪɢʜᴛꜱ: &f" + lights.lightCount());
                msg(sender, "&7ꜰᴏʟɪᴀ: &f" + Scheduler.isFolia());
            }
            default -> msg(sender, "&cᴜɴᴋɴᴏᴡɴ ꜱᴜʙᴄᴏᴍᴍᴀɴᴅ");
        }
        return true;
    }

    private void help(CommandSender sender) {
        msg(sender, "&7/ɢʟ ᴛᴏɢɢʟᴇ &8- &fʏᴏᴜʀ ʟɪɢʜᴛꜱ");
        msg(sender, "&7/ɢʟ ʀᴇʟᴏᴀᴅ &8- &fᴄᴏɴꜰɪɢ");
        msg(sender, "&7/ɢʟ ꜱᴛᴀᴛᴜꜱ &8- &fɪɴꜰᴏ");
    }

    private void msg(CommandSender sender, String text) {
        sender.sendMessage(LEGACY.deserialize(P + text));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        List<String> out = new ArrayList<>();
        String start = args[0].toLowerCase(Locale.ROOT);
        for (String option : List.of("toggle", "reload", "status")) {
            if (option.startsWith(start)) out.add(option);
        }
        return out;
    }
}
