package de.kunst.config;

import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class SetHomeConfigFile extends File{
    private final YamlConfiguration config = YamlConfiguration.loadConfiguration(this);
    final Logger logger;

    public SetHomeConfigFile(String parent, String child, Logger logger) {
        super(parent, child);
        this.logger = logger;
    }

    public void deleteHome(String homeName, String playerName) throws IOException, InvalidConfigurationException{
        final BufferedReader reader = new BufferedReader(new FileReader(this));

        final StringBuilder stringBuilder = new StringBuilder();
        String line;

        logger.info(String.format("Sethome: Deleting home %s of %s", homeName, playerName));
        while ((line = reader.readLine()) != null) {
            //skip the keys containing the home to delete
            if (line.contains(homeName)) {
                while (((line = reader.readLine()) != null) && (!line.contains("yaw: ")));
                continue; //skip the last line of the location string to exclude (yaw: ...)
            }
            //copy file contents
            stringBuilder.append(line);
            stringBuilder.append('\n');
        }
        reader.close();

        logger.info("SETHOME: Pasting remainders into file");

        final FileOutputStream stream = new FileOutputStream(this);
        stream.write(stringBuilder.toString().getBytes()); //paste remaining homes into file
        stream.close();

        config.load(this);

        logger.info(String.format("Successfully deleted home %s of %s", homeName, playerName));
    }

    public List<String> findHomesOfPlayer(String playerName){
        final List<String> res = new ArrayList<>();
        for(String s : config.getKeys(true)){
            String[] splittedString = s.split("\\.");
            if((splittedString.length <= 2) || (!splittedString[0].equals(playerName))) continue;
            res.add(splittedString[splittedString.length-1]);
        }
        return res;
    }

    public void setVal(String path, Object val) throws IOException {
        config.set(path, val);
        config.save(this);
        logger.info(String.format("Value %s set to %s", path, val));
    }

    public int getInt(String path){
        return config.getInt(path);
    }

    public boolean getBoolean(String path){
        return config.getBoolean(path);
    }

    public boolean isBoolean(String path){
        return config.isBoolean(path);
    }

    public Location getLocation(String path){
        return config.getLocation(path);
    }
}
