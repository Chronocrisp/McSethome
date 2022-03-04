package de.kunst;

import de.kunst.commands.CommandTabExecutor;
import de.kunst.config.SetHomeConfigFile;
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
    private final Logger logger = getLogger();

    @Override
    public void onEnable() {
        final File dir = getDataFolder();

        try {
            checkForUnloadedWorlds();
        } catch (IOException e) {
            logger.severe("Couldn't check for unloaded worlds: " + e);
        }

        final SetHomeConfigFile configFile = new SetHomeConfigFile(getDataFolder().getAbsolutePath(), "config.yml", logger);

        if(!configFile.exists()){
            try {
                logger.info("Trying to create a new config File...");
                if(!((dir.mkdirs() || dir.exists()) || configFile.createNewFile())){
                    logger.severe("Couldn't create config.yml in " + configFile.getAbsolutePath());
                    getPluginLoader().disablePlugin(this);
                }
            } catch (IOException e){
                logger.severe("Couldn't create configFile " + e);
                logger.severe("Shutting down.");
                getPluginLoader().disablePlugin(this);
                return;
            }
        }

        logger.info("Successfully creted configuration file. Registering commands...");
        registerCommands(configFile); //has to be here as it's dependent on YAMLconfig checked earlier
        logger.info("Init success.");
    }

    private void registerCommands(SetHomeConfigFile configFile){
        final String[] commands = {"setHome", "home", "deleteHome", "setMaxHomes", "homes", "acceptHomeInvitation", "setAllowInvitations"};
        final CommandTabExecutor tabExe = new CommandTabExecutor(configFile, logger);

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
    //load unloaded worlds storing homes in config.yml to avoid an exception on startup
    public void checkForUnloadedWorlds() throws IOException{
        final File file = new File(getDataFolder().getAbsolutePath(), "config.yml");
        file.setReadOnly();

        if(!file.exists()){
            logger.info("Config File hasn't been creted yet. Skipping world check.");
            return;
        }

        final BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;

        logger.info("--------Looking for unloaded worlds containing a home--------");
        while((line = reader.readLine()) != null){
            if(line.contains("world: ")){
                final String worldName = line.split(":")[1].trim();
                if(Bukkit.getWorld(worldName) == null){
                    logger.warning("WORLD TO LOAD: " + worldName);
                    new WorldCreator(worldName).createWorld();
                }
            }
        }
        reader.close();
        file.setWritable(true);
        logger.info("--------All worlds storing homes loaded--------");
    }
}
