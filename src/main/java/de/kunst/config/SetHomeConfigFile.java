package de.kunst.config;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public final class SetHomeConfigFile extends File{
    private final YamlConfiguration config = YamlConfiguration.loadConfiguration(this);
    private final Logger logger;
    private final HashMap<UUID, Location> invitations = new HashMap<>();

    public SetHomeConfigFile(String parent, String child, Logger logger) {
        super(parent, child);
        this.logger = logger;
    }

    public boolean teleportPlayerToHome(Player player, String homeName){
        final Location loc = getHome(player.getUniqueId(), homeName);
        if (loc == null) {
            player.sendMessage(ChatColor.RED + "Could not find a home named " + homeName);
            return false;
        }

        final String locAsString = locToString(loc);
        final String message = ChatColor.GOLD + "Successfully teleported you to " + locAsString;


        if (player.getVehicle() == null) {
            if(player.teleport(loc, PlayerTeleportEvent.TeleportCause.COMMAND)) {
                player.sendMessage(message);
                logger.info(String.format("SETHOME: Successfully teleported %s to %s", player.getName(), locAsString));
                return true;
            }
            player.sendMessage("Teleportation failed.");
            return false;
        }

        for (Entity e : player.getVehicle().getPassengers()) {
            if (e.teleport(loc, PlayerTeleportEvent.TeleportCause.COMMAND)) {
                e.sendMessage(message);
                logger.info(String.format("SETHOME: Successfully teleported %s to %s", e.getName(), locAsString));
            }
        }
        return true;
    }

    public boolean teleportPlayerToHomeAndInviteAnother(Player player, String homeName, String player2){
        teleportPlayerToHome(player, homeName);

        final Location loc = getHome(player.getUniqueId(), homeName);
        final Player playerFound = player.getServer().getPlayer(player2);

        if (playerFound == null) {
            player.sendMessage(String.format("%sCould not find an online player named %s", ChatColor.RED, player2));
            return false;
        }

        if (!getPlayerAllowsInvitations(player)) {
            player.sendMessage(ChatColor.RED + "This player doesn't want to receive invitations.");
            return true;
        }

        if (invitations.containsKey(playerFound.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "This player already received an invitation.",
                    "Please try again after 10 seconds.");
            return true;
        }

        playerFound.sendMessage(
                String.format("%s%s%s has sent you an invitation to teleport to %s.",
                        ChatColor.LIGHT_PURPLE, player.getName(), ChatColor.WHITE, locToString(loc))
                ,"You have 10 seconds to accept this via /ahi"
        );

        invitations.put(playerFound.getUniqueId(), loc);

        //Remove invitation after 200 Ticks (~10 seconds on default)
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("SetHomePlugin"),
                () -> {
                    invitations.remove(playerFound.getUniqueId(), loc);
                    playerFound.sendMessage(ChatColor.RED + "The invitation has expired.");
                }, 200);
        return true;
    }

    public boolean acceptHomeInvitation(Player player){
        final Location locOfInvitation = invitations.remove(player.getUniqueId());
        if (locOfInvitation == null) {
            player.sendMessage(ChatColor.RED + "Couldn't find an invitation.");
            return false;
        }
        final String locAsString = locToString(locOfInvitation);
        player.sendMessage(ChatColor.GOLD + "You accepted the invitation to " + locAsString);
        if(player.teleport(locOfInvitation)){
            logger.info("Successfully teleported " + player.getName() + " to " + locAsString);
            return true;
        }
        player.sendMessage(ChatColor.RED + "Teleportation failed.");
        return false;
    }

    public boolean deleteHome(Player player, String homeName) throws IOException {
        if (config.getLocation(player.getUniqueId() + ".homes." + homeName) == null) {
            player.sendMessage(String.format("%s You haven't set a home called %s yet.", ChatColor.RED, homeName));
            return false;
        }

        setVal(player.getUniqueId() + ".homes." + homeName, null);
        player.sendMessage(String.format("%sSuccessfully deleted home %s", ChatColor.GOLD, homeName));
        return true;
    }

    public boolean displayHomesForPlayer(Player player){
        final List<String> homes = getHomesOfPlayer(player.getUniqueId());

        if (homes.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No homes set yet.");
            return true;
        }

        homes.forEach(
                home -> {
                    final Location loc = getHome(player.getUniqueId(), home);
                    if (loc != null) player.sendMessage(String.format("%s in %s", home, locToString(loc)));
                }
        );
        return true;
    }

    public int getMaxHomes(){
        return config.getInt("options.maxHomesPerPlayer");
    }

    public boolean setMaxHomes(int val) throws IOException {
        setVal("options.maxHomesPerPlayer", val);
        return true;
    }

    public boolean setHome(Player player, String homeName) throws IOException {
        final List<String> homesOfThisPlayer = getHomesOfPlayer(player.getUniqueId());

        //if home is being overwritten, don't add 1
        final int homesSet = homesOfThisPlayer.size() + (homesOfThisPlayer.contains(homeName) ? 0 : 1);
        final int maxHomes = getMaxHomes();

        if ((homesSet > maxHomes) && (maxHomes != 0)) {
            player.sendMessage(String.format("%sYou can't set %s", ChatColor.RED, (maxHomes > 0 ? ("more homes than " +
                    maxHomes) : "any homes.")));
            return false;
        }

        setVal(player.getUniqueId() + ".homes." + homeName, player.getLocation());

        player.sendMessage(String.format("%sSuccessfully set home %s at %s", ChatColor.GOLD, homeName, locToString(player.getLocation())));
        player.sendMessage(String.format("You have %d homes.", homesSet));
        return true;
    }

    public Location getHome(UUID playerID, String homeName){
        return config.getLocation(playerID + ".homes." + homeName );
    }

    public List<String> getHomesOfPlayer(UUID uuid){
        final String playerID = String.valueOf(uuid);
        final List<String> res = new ArrayList<>();
        for(String s : config.getKeys(true)){
            String[] splittedString = s.split("\\.");
            if((splittedString.length <= 2) || (!splittedString[0].equals(playerID))) continue;
            res.add(splittedString[splittedString.length-1]);
        }
        return res;
    }

    public boolean setPlayerAllowsInvitations(Player player, boolean allowsInvitations) throws IOException {
        setVal(player.getUniqueId() + ".allowsInvitations", allowsInvitations);
        player.sendMessage(ChatColor.GOLD + (allowsInvitations ? "You now accept invitations." : "You won't receive invitations anymore."));
        return true;
    }

    public boolean getPlayerAllowsInvitations(Player player){
        return config.getBoolean(player.getUniqueId() + ".allowsInvitations");
    }

    private void setVal(String path, Object val) throws IOException {
        config.set(path, val);
        config.save(this);
        logger.info(String.format("Value %s set to %s", path, val));
    }

    private String locToString(Location loc){
        return String.format("%sX: %d Y: %d Z: %d %sin%s %s",
                ChatColor.GOLD, (int)loc.getX(), (int)loc.getY(),
                (int)loc.getZ(), ChatColor.WHITE, ChatColor.DARK_GREEN,
                (loc.getWorld() != null ? loc.getWorld().getName() : "undefined"));
    }
}
