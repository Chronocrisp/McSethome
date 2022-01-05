package de.kunst.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CommandTabExecutor implements TabExecutor {
    private final File configFile;
    private final YamlConfiguration configs;

    private final Logger logger = Bukkit.getLogger();
    private final HashMap<UUID, Location> invitations = new HashMap<>();

    public CommandTabExecutor(File fileIn){
        configFile = fileIn;
        configs = YamlConfiguration.loadConfiguration(fileIn);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final int maxHomes = configs.getInt("options.maxHomesPerPlayer");
        final Player player = sender.getServer().getPlayer(sender.getName());

        if(!command.getName().equals("setMaxHomes") && !command.getName().equals("homes")){
            if(player == null){
                sender.sendMessage(ChatColor.RED + "This command is to be used by players only.");
                return true;
            }
        }

        try {
            switch (command.getName()) {
                case "setAllowInvitations": {
                    final boolean allowsInvitations = Boolean.parseBoolean(args[0]);
                    configs.set(sender.getName() + ".allowsInvitations", allowsInvitations);
                    configs.save(configFile);
                    sender.sendMessage(ChatColor.GOLD + (allowsInvitations ? "You now accept invitations." : "You won't receive invitations anymore."));
                    return true;
                }

                case "setMaxHomes": {
                    final int homesIn = Integer.parseInt(args[0]);
                    configs.set("options.maxHomesPerPlayer", homesIn);
                    configs.save(configFile);
                    sender.sendMessage(ChatColor.GOLD + "Maximum amount of homes set to " + homesIn);
                    logger.info("SETHOME: Maximum amount of homes set to " + homesIn);
                    return true;
                }

                case "acceptHomeInvitation": {
                    final Location locOfInvitation = invitations.remove(player.getUniqueId());
                    if (locOfInvitation == null) {
                        sender.sendMessage(ChatColor.RED + "Couldn't find an invitation.");
                        return false;
                    }
                    final String locAsString = locToString(locOfInvitation);
                    sender.sendMessage(ChatColor.GOLD + "You accepted the invitation to " + locAsString);
                    if(player.teleport(locOfInvitation)){
                        logger.info("SETHOME: Successfully teleported " + sender.getName() + " to " + locAsString);
                        return true;
                    }
                    sender.sendMessage(ChatColor.RED + "Teleportation failed.");
                    return false;
                }

                case "homes": {
                    final String playerName = (args.length > 0 && sender.isOp()) ? args[0] : sender.getName();
                    final List<String> homes = findHomesOfPlayer(playerName);

                    if (homes.isEmpty()) {
                        sender.sendMessage(ChatColor.RED + "No homes set yet.");
                        return false;
                    }
                    sender.sendMessage(ChatColor.GOLD + "Homes of " + playerName);
                    for (String home : homes) {
                        final Location loc = configs.getLocation(playerName + ".homes." + home);
                        if (loc != null) sender.sendMessage(String.format("%s in %s", home, locToString(loc)));
                    }
                    return true;
                }

                case "home": {
                    final Location loc = configs.getLocation(sender.getName() + ".homes." + args[0]);
                    if (loc == null) {
                        sender.sendMessage(ChatColor.RED + "Could not find a home named " + args[0]);
                        return false;
                    }

                    final String locAsString = locToString(loc);
                    final String message = ChatColor.GOLD + "Successfully teleported you to " + locAsString;


                    if (args.length == 1) {
                        if (player.getVehicle() == null) {
                            if(player.teleport(loc, PlayerTeleportEvent.TeleportCause.COMMAND)) {
                                sender.sendMessage(message);
                                logger.info(String.format("SETHOME: Successfully teleported %s to %s", sender.getName(), locAsString));
                                return true;
                            }
                            sender.sendMessage("Teleportation failed.");
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

                    final Player playerFound = sender.getServer().getPlayer(args[1]);
                    if (playerFound == null) {
                        sender.sendMessage(String.format("%sCould not find an online player named %s", ChatColor.RED, args[1]));
                        return false;
                    }

                    final String pathToAcceptsInvitations = playerFound.getName() + ".allowsInvitations";
                    if (configs.isBoolean(pathToAcceptsInvitations) && !configs.getBoolean(pathToAcceptsInvitations)) {
                        sender.sendMessage(ChatColor.RED + "This player doesn't want to receive invitations.");
                        return true;
                    }

                    if (invitations.containsKey(playerFound.getUniqueId())) {
                        sender.sendMessage(ChatColor.RED + "This player already received an invitation.",
                                "Please try again after 10 seconds.");
                        return true;
                    }

                    playerFound.sendMessage(
                            String.format("%s%s%s has sent you an invitation to teleport to %s.",
                                    ChatColor.LIGHT_PURPLE, sender.getName(), ChatColor.WHITE, locAsString)
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

                case "setHome": {
                    final List<String> homesOfThisPlayer = findHomesOfPlayer(sender.getName());
                    //if home is being overwritten, don't add 1
                    final int homesSet = homesOfThisPlayer.size() + (homesOfThisPlayer.contains(args[0]) ? 0 : 1);

                    if ((homesSet > maxHomes) && (maxHomes != 0)) {
                        sender.sendMessage(String.format("%sYou can't set %s", ChatColor.RED, (maxHomes > 0 ? ("more homes than " +
                                maxHomes) : "any homes.")));
                        return true;
                    }

                    configs.set(sender.getName() + ".homes." + args[0], player.getLocation());
                    configs.save(configFile);

                    sender.sendMessage(String.format("%sSuccessfully set home %s at %s", ChatColor.GOLD, args[0], locToString(player.getLocation())));
                    sender.sendMessage(String.format("You have %d homes.", homesSet));
                    return true;
                }

                case "deleteHome": {
                    if (configs.getLocation(sender.getName() + ".homes." + args[0]) == null) {
                        sender.sendMessage(String.format("%s You haven't set a home called %s yet.", ChatColor.RED, args[0]));
                        return false;
                    }
                    try (final BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                        final StringBuilder stringBuilder = new StringBuilder();
                        String line;

                        logger.info(String.format("Sethome: Deleting home %s of %s", args[0], sender.getName()));
                        while ((line = reader.readLine()) != null) {
                            //skip the keys containing the home to delete
                            if (line.contains(args[0])) {
                                while (((line = reader.readLine()) != null) && (!line.contains("yaw: "))) ;
                                continue; //skip the last line of the location string to exclude (yaw: ...)
                            }
                            //copy file contents
                            stringBuilder.append(line);
                            stringBuilder.append('\n');
                        }
                        reader.close();

                        logger.info("SETHOME: Pasting remainders into file");
                        try (final FileOutputStream stream = new FileOutputStream(configFile)) {
                            stream.write(stringBuilder.toString().getBytes()); //paste remaining homes into file
                            stream.flush();
                        } catch (IOException e) {
                            sender.sendMessage(ChatColor.RED + "Failed to write to config.yml.");
                            logger.severe("Failed to write remainders into config.yml: " + e.toString());
                            return false;
                        }

                        configs.load(configFile); //accept changes

                        logger.info(String.format("Sethome: Successfully deleted home %s of %s", args[0], sender.getName()));
                        sender.sendMessage(ChatColor.GOLD + "Successfully deleted home " + args[0]);
                        return true;
                    } catch (IOException e) {
                        sender.sendMessage(ChatColor.RED + "Failed to read config.yml.");
                        logger.severe("Failed to read config.yml: " + e.toString());
                    } catch (InvalidConfigurationException e) {
                        sender.sendMessage(ChatColor.RED + "Failed to reload configuration File.");
                        logger.severe("Invalid configuration loaded: " + e.toString());
                    }
                    return false;
                }
            }
        } catch (ArrayIndexOutOfBoundsException e){
            sender.sendMessage(ChatColor.RED + "Please provide an argument.");
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "Couldn't read/write to config.yml");
            logger.warning("Could not access configFile: " + e.toString());
        } catch (NumberFormatException e){
            sender.sendMessage(ChatColor.RED + "Please provide an integer argument.");
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        final List<String> empty = Collections.singletonList(" ");
        final ArrayList<String> homesOfPlayer = new ArrayList<>(findHomesOfPlayer(sender.getName()));
        final List<String> onlinePlayers = sender.getServer().getOnlinePlayers().stream().map(HumanEntity::getName).collect(Collectors.toList());

        switch (command.getName()){
            case "homes":
                return sender.isOp() && args.length == 1 ? onlinePlayers : empty;

            case "setAllowInvitations":
                return Arrays.asList("True", "False");

            case "setMaxHomes":
                return Arrays.asList("50", "100");

            case "deleteHome":
                return args.length == 1 ? homesOfPlayer : empty;

            case "home":
                if(args.length > 2) return empty;
                return args.length == 1 ? homesOfPlayer : onlinePlayers;
        }
        return empty;
    }

    private List<String> findHomesOfPlayer(String playerName){
        final List<String> res = new ArrayList<>();
        for(String s : configs.getKeys(true)){
            String[] splittedString = s.split("\\.");
            if((splittedString.length <= 2) || (!splittedString[0].equals(playerName))) continue;
            res.add(splittedString[splittedString.length-1]);
        }
        return res;
    }

    private String locToString(Location loc){
        return String.format("%sX: %d Y: %d Z: %d %sin%s %s",
                ChatColor.GOLD, (int)loc.getX(), (int)loc.getY(),
                (int)loc.getZ(), ChatColor.WHITE, ChatColor.DARK_GREEN,
                (loc.getWorld() != null ? loc.getWorld().getName() : "undefined"));
    }
}
