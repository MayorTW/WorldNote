package tw.mayortw.worldnote;
/*
 * Written by R26
 */

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.World;

import com.onarandombox.MultiverseCore.api.MVPlugin;
import com.onarandombox.MultiverseCore.api.MVWorldManager;

import java.io.File;
import java.io.IOException;

public class WorldNotePlugin extends JavaPlugin implements Listener {

    private MVWorldManager mvWorldManager;
    private FileConfiguration config = new YamlConfiguration();
    private File configFile;

    @Override
    public void onEnable() {

        // Set up events and multiverse API
        PluginManager pluginManager = getServer().getPluginManager();

        MVPlugin mvPlugin = (MVPlugin) pluginManager.getPlugin("Multiverse-Core");
        if(mvPlugin == null)
            getLogger().warning("Multiverse-Core not available, alias won't be updated");
        else
            mvWorldManager = mvPlugin.getCore().getMVWorldManager();
        
        pluginManager.registerEvents(this, this);

        // Load file
        configFile = new File(getDataFolder(), "worlds.yml");

        try {
            config.load(configFile);
        } catch(IOException | InvalidConfigurationException e) {
            getLogger().warning("Cannot load file: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {

        // Save world notes
        getServer().getWorlds().stream().forEach(this::saveWorldNotes);

        // Save to file
        try {
            config.save(configFile);
            getLogger().info("File saved to " + configFile.getPath());
        } catch(IOException e) {
            getLogger().severe("Cannot save file: " + e.getMessage());
        }
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent eve) {
        saveWorldNotes(eve.getWorld());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if(args.length < 1)
            return false;

        World world = getServer().getWorld(args[0]);
        if(world == null) {
            sender.sendMessage("World not found");
            return true;
        }
        ConfigurationSection section = getCreateSection(config, world.getUID().toString());

        if(args.length == 1) {
            String note = section.getString("note");
            if(note == null)
                sender.sendMessage("No note available");
            else
                sender.sendMessage(note);
        } else { // length > 1
            if(args[1].equals("-d")) {
                section.set("note", null);
                sender.sendMessage("Deleted note");
            } else {
                String note = "";
                for(int i = 1; i < args.length; i++)
                    note += args[i] + " ";
                section.set("note", note.trim());
                sender.sendMessage("Created note");
            }
        }

        return true;
    }


    private void saveWorldNotes(World world) {
        ConfigurationSection section = getCreateSection(config, world.getUID().toString());
        if(mvWorldManager != null)
            section.set("alias", mvWorldManager.getMVWorld(world).getAlias());
        section.set("name", world.getName());
    }

    private ConfigurationSection getCreateSection(ConfigurationSection section, String path) {
        ConfigurationSection created = config.getConfigurationSection(path);
        if(created == null)
            created = section.createSection(path);
        return created;
    }
}
