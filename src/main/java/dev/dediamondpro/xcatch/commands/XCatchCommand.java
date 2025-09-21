/*
 * This file is part of XCatch.
 *
 * XCatch is free software: you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * XCatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see
 * <https://www.gnu.org/licenses/>.
 */
package dev.dediamondpro.xcatch.commands;

import dev.dediamondpro.xcatch.XCatch;
import dev.dediamondpro.xcatch.data.PersistentData;
import dev.dediamondpro.xcatch.gui.ViewGui;
import dev.dediamondpro.xcatch.listeners.OnBlockBreak;
import dev.dediamondpro.xcatch.utils.FlagHandler;
import dev.dediamondpro.xcatch.utils.Utils;

import org.bukkit.Bukkit;               // <-- added
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.UUID;

public class XCatchCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0
                || args[0].equals("help")) {
            sender.sendMessage(new String[]
                    {
                            "§8[§cXCatch§8] version " + XCatch.INSTANCE.getDescription().getVersion(),
                            "§7/xcatch help, show this help menu.",
                            "§7/xcatch view [<player>], view a player's recent flags and/or bans.",
                            "§7/xcatch clear <player>, clear a player's flags.",
                            "§7/xcatch info, get some statics about XCatch on your server.",
                            "§7/xcatch reload, reload XCatch's config.",
                            "§7/xcatch test <player>, add a flag to a player to test things",
                            "§7/xcatch tp <world|uuid> <x> <y> <z>, teleport to specific coordinates", // <-- updated help
                            "§7/xcatch debug <player>, give debug statistics of a player."
                    });
            return true;
        }

        switch (args[0]) {
            case "debug":
                if (args.length >= 2) {
                    Player player = XCatch.INSTANCE.getServer().getPlayer(args[1]);
                    if (player == null) {
                        sender.sendMessage("§8[§cXCatch§8] §cPlayer not found.");
                        return false;
                    }
                    UUID uuid = player.getUniqueId();
                    sender.sendMessage("§8[§cXCatch§8] §7Debug info for §c" + player.getDisplayName());
                    if (FlagHandler.flags.containsKey(uuid) && XCatch.config.getInt("ban-flags") != 0)
                        sender.sendMessage("§7Flags: §c" + FlagHandler.flags.get(uuid).flags + "/" + XCatch.config.getInt("ban-flags"));
                    else if (FlagHandler.flags.containsKey(uuid))
                        sender.sendMessage("§7Flags: §c" + FlagHandler.flags.get(uuid).flags);
                    else if (XCatch.config.getInt("ban-flags") != 0)
                        sender.sendMessage("§7Flags: §c0/" + XCatch.config.getInt("ban-flags"));
                    else
                        sender.sendMessage("§7Flags: §c0");
                    if (OnBlockBreak.getData().containsKey(uuid))
                        sender.sendMessage("§7Changes: §c" + OnBlockBreak.getData().get(uuid).changes + "/" + XCatch.config.getInt("changes-for-flag"));
                    else
                        sender.sendMessage("§7Changes: §c0" + "/" + XCatch.config.getInt("changes-for-flag"));
                    if (OnBlockBreak.getPendingChanges().containsKey(uuid))
                        sender.sendMessage("§7Pending Changes: §c" + OnBlockBreak.getPendingChanges().get(uuid).size());
                    if (OnBlockBreak.getBlocksMined().containsKey(uuid)) {
                        for (Material material : OnBlockBreak.getBlocksMined().get(uuid).keySet()) {
                            if (OnBlockBreak.getBlocksMined().get(uuid).get(material) == 0) continue;
                            sender.sendMessage("§7" + Utils.capitalize(material.name().toLowerCase().replace("_", " ")) +
                                    ": §c" + OnBlockBreak.getBlocksMined().get(uuid).get(material) + "/" + XCatch.rareOres.get(material));
                        }
                    }
                    return true;
                }
                sender.sendMessage("§8[§cXCatch§8] §cMissing argument <player>.");
                return false;

            case "view":
                if (sender instanceof Player) {
                    if (args.length == 1) {
                        ViewGui.openAllGui((Player) sender, 1);
                    } else {
                        UUID uuid = Utils.getOfflineUUID(args[1]);
                        if (uuid == null
                                || !PersistentData.data.actions.containsKey(uuid)) {
                            sender.sendMessage("§8[§cXCatch§8] §cPlayer not found or no data available for player.");
                            return false;
                        }
                        ViewGui.openPlayerGui((Player) sender, uuid, args[1], 1);
                    }
                } else {
                    sender.sendMessage("§8[§cXCatch§8] §cSince this command uses a gui, it can only be used from in-game.");
                }
                return true;

            case "reload":
                XCatch.INSTANCE.reloadConfig();
                XCatch.config = XCatch.INSTANCE.getConfig();
                XCatch.loadConfigParts();
                sender.sendMessage("§8[§cXCatch§8] §7Config reloaded.");
                return true;

            case "info":
                sender.sendMessage(new String[]
                        {
                                "§8[§cXCatch§8] version " + XCatch.INSTANCE.getDescription().getVersion(),
                                "§7Total X-Ray Flags: §c" + PersistentData.data.totalFlags,
                                "§7Total X-Ray Bans: §c" + PersistentData.data.totalBans
                        });
                return true;

            case "clear":
                if (args.length < 2) {
                    sender.sendMessage("§8[§cXCatch§8] §cMissing argument <player>.");
                    return false;
                }
                UUID uuid = Utils.getOfflineUUID(args[1]);
                if (uuid == null
                        || !PersistentData.data.actions.containsKey(uuid)) {
                    sender.sendMessage("§8[§cXCatch§8] §cPlayer not found or no data available for player.");
                    return false;
                }
                PersistentData.data.actions.remove(uuid);
                sender.sendMessage("§8[§cXCatch§8] §cFlags of " + args[1] + " has been cleared.");
                return true;

            case "test":
                if (args.length < 2) {
                    sender.sendMessage("§8[§cXCatch§8] §cMissing argument <player>.");
                    return false;
                }
                Player player = XCatch.INSTANCE.getServer().getPlayer(args[1]);
                if (player == null) {
                    sender.sendMessage("§8[§cXCatch§8] §cPlayer not found.");
                    return false;
                }
                FlagHandler.addFlag(new BlockBreakEvent(null, player), true);
                return true;

            case "tp": {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("§8[§cXCatch§8] §cSince this command teleports you, it can only be used from in-game.");
                    return true;
                }
                if (args.length < 5) {
                    sender.sendMessage("§8[§cXCatch§8] §cMissing argument <world|uuid> <x> <y> <z>.");
                    return false;
                }

                // Resolve world by name, then by UUID
                World world = XCatch.INSTANCE.getServer().getWorld(args[1]);
                if (world == null) {
                    try {
                        UUID wid = UUID.fromString(args[1]);
                        world = XCatch.INSTANCE.getServer().getWorld(wid);
                        // Alternatively: world = Bukkit.getWorld(wid);
                    } catch (IllegalArgumentException ignore) { /* not a UUID */ }
                }
                if (world == null) {
                    sender.sendMessage("§8[§cXCatch§8] §cWorld not found: §7" + args[1]);
                    return false;
                }

                double x, y, z;
                try {
                    x = Double.parseDouble(args[2]);
                    y = Double.parseDouble(args[3]);
                    z = Double.parseDouble(args[4]);
                } catch (NumberFormatException ignored) {
                    sender.sendMessage("§8[§cXCatch§8] §cCoordinates contain invalid argument.");
                    return false;
                }

                // Clamp Y to world's legal range (1.18+ worlds can customize heights)
                final int minY = world.getMinHeight();
                final int maxY = world.getMaxHeight() - 1;
                if (y < minY) y = minY;
                if (y > maxY) y = maxY;

                // Center X/Z for safety; preserve current yaw/pitch
                final Location from = p.getLocation();
                final Location to = new Location(world, x + 0.5, y, z + 0.5, from.getYaw(), from.getPitch());

                // Prefer Paper's teleportAsync if present; fall back to sync TP on Spigot
                try {
                    java.lang.reflect.Method m = p.getClass().getMethod("teleportAsync", Location.class);
                    @SuppressWarnings("unchecked")
                    java.util.concurrent.CompletableFuture<Boolean> fut =
                            (java.util.concurrent.CompletableFuture<Boolean>) m.invoke(p, to);

                    // Never block on the future on the main thread; use callback
                    fut.thenAccept(success -> {
                        if (Boolean.TRUE.equals(success)) {
                            p.sendMessage("§8[§cXCatch§8] §7Teleported to §c" + world.getName()
                                    + " §7@ §c" + (int) x + " " + (int) y + " " + (int) z);
                        } else {
                            p.sendMessage("§8[§cXCatch§8] §cTeleport failed (chunk not loaded or blocked).");
                        }
                    });
                } catch (NoSuchMethodException nsme) {
                    // Spigot (no teleportAsync): synchronous teleport
                    boolean ok = p.teleport(to);
                    p.sendMessage(ok
                            ? "§8[§cXCatch§8] §7Teleported to §c" + world.getName()
                            + " §7@ §c" + (int) x + " " + (int) y + " " + (int) z
                            : "§8[§cXCatch§8] §cTeleport failed.");
                } catch (ReflectiveOperationException roe) {
                    p.sendMessage("§8[§cXCatch§8] §cTeleport failed (runtime error).");
                }
                return true;
            }
        }

        sender.sendMessage("§8[§cXCatch§8] §cUnknown sub-command.");
        return false;
    }
}
