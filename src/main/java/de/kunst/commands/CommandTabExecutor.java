package de.kunst.commands;

import de.kunst.config.SetHomeConfigFile;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CommandTabExecutor implements TabExecutor {
    private final SetHomeConfigFile configFile;
    private final Logger logger;

    public CommandTabExecutor(SetHomeConfigFile fileIn, Logger logger){
        configFile = fileIn;
        this.logger = logger;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final Player player = sender.getServer().getPlayer(sender.getName());

        try {
            if(command.getName().equals("setMaxHomes")) return configFile.setMaxHomes(Integer.parseInt(args[0]));

            if(player == null){
                sender.sendMessage(ChatColor.RED + "This command is to be used by players only.");
                return false;
            }

            switch (command.getName()) {
                case "setHome": return configFile.setHome(player, args[0]);
                case "home": return args.length > 1 ? configFile.teleportPlayerToHomeAndInviteAnother(player, args[0], args[1]) : configFile.teleportPlayerToHome(player, args[0]);
                case "deleteHome": return configFile.deleteHome(player, args[0]);
                case "homes": return configFile.displayHomesForPlayer(player);
                case "setAllowInvitations": return configFile.setPlayerAllowsInvitations(player, Boolean.parseBoolean(args[0]));
                case "acceptHomeInvitation": return configFile.acceptHomeInvitation(player);
            }
        } catch (ArrayIndexOutOfBoundsException e){
            sender.sendMessage(ChatColor.RED + "Please provide an argument.");
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "Couldn't read/write to config.yml");
            logger.warning("Could not access configFile: " + e);
        } catch (NumberFormatException e){
            sender.sendMessage(ChatColor.RED + "Please provide an integer argument.");
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        final List<String> empty = Collections.singletonList(" ");
        final ArrayList<String> homesOfPlayer = new ArrayList<>(configFile.getHomesOfPlayer(((Player)(sender)).getUniqueId()));
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
}
