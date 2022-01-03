package de.kunst;

import de.kunst.commands.CommandTabExecutor;
import org.bukkit.Bukkit;
import org.bukkit.WorldCreator;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

public final class Main extends JavaPlugin {
    private final File configFile = new File(getDataFolder().getAbsolutePath(), "config.yml");
    private final Logger logger = getLogger();

    @Override
    public void onEnable() {
        final String[] commands = {"setHome", "home", "deleteHome", "setMaxHomes", "homes", "acceptHomeInvitation", "setAllowInvitations"};
        final File dir = getDataFolder();

        try {
            if(dir.mkdirs() || dir.exists()){
                if(configFile.createNewFile()){
                    logger.info("Sethome: Config.yml newly created, skipping iteration.");
                    registerCommands(commands, configFile); //YAMLconfig doesn't need to be checked (freshly created)
                    return;
                }
            }
        } catch (IOException e) {
            logger.severe("Couldn't create configFile: " + e.toString());
        }

        if(!configFile.exists()){
            logger.severe("Couldn't create config.yml in " + configFile.getAbsolutePath());
            getPluginLoader().disablePlugin(this);
            return;
        }

        //load unloaded worlds storing homes in config.yml to avoid an exception on startup
        try(final BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                String line;
                logger.info("--------SETHOME: Looking for unloaded worlds containing a home--------");
                while((line = reader.readLine()) != null){
                    if(line.contains("world: ")){
                        final String worldName = line.split(":")[1].trim();
                        if(Bukkit.getWorld(worldName) == null){
                            logger.warning("WORLD TO LOAD: " + worldName);
                            new WorldCreator(worldName).createWorld();
                        }
                    }
                }
                logger.info("--------SETHOME: All worlds storing homes loaded--------");
            } catch (IOException e) {
                logger.severe("Couldn't read plugin configuration file. Shutting Sethome down: " + e.toString());
                getPluginLoader().disablePlugin(this);
            }

        registerCommands(commands, configFile); //has to be here as it's dependent on YAMLconfig checked earlier
    }

    private void registerCommands(String[] commands, File configFile){
        final CommandTabExecutor tabExe = new CommandTabExecutor(configFile);
        for(String s : commands){
            final PluginCommand c = getCommand(s);
            if(c == null) {
                logger.warning("Sethome: Unimplemented command: " + s);
                continue;
            }
            c.setExecutor(tabExe);
            c.setTabCompleter(tabExe);
        }
    }
}
