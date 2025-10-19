package com.user404_.infinitehomes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class InfiniteHomes extends JavaPlugin implements TabCompleter {

    private Map<UUID, Map<String, Location>> homes;
    private Map<String, Location> globalHomes;
    private Map<String, PlayerHomeData> playerHomes;
    private Map<UUID, Long> cooldowns;
    private FileConfiguration homesConfig;
    private File homesFile;
    private FileConfiguration globalHomesConfig;
    private File globalHomesFile;
    private FileConfiguration playerHomesConfig;
    private File playerHomesFile;
    private Map<String, FileConfiguration> translations;
    private File translationsDir;

    @Override
    public void onEnable() {
        homes = new HashMap<>();
        globalHomes = new HashMap<>();
        playerHomes = new HashMap<>();
        cooldowns = new HashMap<>();
        translations = new HashMap<>();

        setupHomesConfig();
        setupGlobalHomesConfig();
        setupPlayerHomesConfig();
        loadHomesFromConfig();
        loadGlobalHomesFromConfig();
        loadPlayerHomesFromConfig();
        setupTranslations();

        // Standardkonfiguration erstellen, falls nicht vorhanden
        getConfig().addDefault("max-homes", -1);
        getConfig().addDefault("home-cooldown", -1);
        getConfig().options().copyDefaults(true);
        saveConfig();

        // TabCompleter registrieren
        getCommand("home").setTabCompleter(this);
        getCommand("delhome").setTabCompleter(this);
        getCommand("globalhome").setTabCompleter(this);
        getCommand("delglobalhome").setTabCompleter(this);
        getCommand("playerhome").setTabCompleter(this);
        getCommand("delplayerhome").setTabCompleter(this);

        getLogger().info("InfiniteHomes plugin by User404 enabled!");
    }

    @Override
    public void onDisable() {
        saveHomesToConfig();
        saveGlobalHomesToConfig();
        savePlayerHomesToConfig();
        getLogger().info("InfiniteHomes plugin by User404 disabled!");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // Nur für Spieler und für die relevanten Befehle
        if (!(sender instanceof Player) || (!command.getName().equalsIgnoreCase("home") &&
                !command.getName().equalsIgnoreCase("delhome") &&
                !command.getName().equalsIgnoreCase("globalhome") &&
                !command.getName().equalsIgnoreCase("delglobalhome") &&
                !command.getName().equalsIgnoreCase("playerhome") &&
                !command.getName().equalsIgnoreCase("delplayerhome"))) {
            return completions;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("home") || command.getName().equalsIgnoreCase("delhome")) {
            // Home-Namen des Spielers holen
            UUID playerUuid = player.getUniqueId();

            if (!homes.containsKey(playerUuid) || homes.get(playerUuid).isEmpty()) {
                return completions;
            }

            Set<String> homeNames = homes.get(playerUuid).keySet();

            if (args.length == 0 || args[0].isEmpty()) {
                completions.addAll(homeNames);
            } else {
                String input = args[0].toLowerCase();
                for (String home : homeNames) {
                    if (home.toLowerCase().startsWith(input)) {
                        completions.add(home);
                    }
                }
            }
        } else if (command.getName().equalsIgnoreCase("globalhome") || command.getName().equalsIgnoreCase("delglobalhome")) {
            // Globale Home-Namen holen
            if (globalHomes.isEmpty()) {
                return completions;
            }

            Set<String> globalHomeNames = globalHomes.keySet();

            if (args.length == 0 || args[0].isEmpty()) {
                completions.addAll(globalHomeNames);
            } else {
                String input = args[0].toLowerCase();
                for (String home : globalHomeNames) {
                    if (home.toLowerCase().startsWith(input)) {
                        completions.add(home);
                    }
                }
            }
        } else if (command.getName().equalsIgnoreCase("playerhome") || command.getName().equalsIgnoreCase("delplayerhome")) {
            // PlayerHome-Namen holen
            if (playerHomes.isEmpty()) {
                return completions;
            }

            Set<String> playerHomeNames = playerHomes.keySet();

            if (args.length == 0 || args[0].isEmpty()) {
                completions.addAll(playerHomeNames);
            } else {
                String input = args[0].toLowerCase();
                for (String home : playerHomeNames) {
                    if (home.toLowerCase().startsWith(input)) {
                        completions.add(home);
                    }
                }
            }
        }

        return completions;
    }

    private void setupHomesConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        homesFile = new File(getDataFolder(), "homes.yml");

        if (!homesFile.exists()) {
            try {
                homesFile.createNewFile();
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not create homes.yml", e);
            }
        }

        homesConfig = YamlConfiguration.loadConfiguration(homesFile);
    }

    private void setupGlobalHomesConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        globalHomesFile = new File(getDataFolder(), "globalhomes.yml");

        if (!globalHomesFile.exists()) {
            try {
                globalHomesFile.createNewFile();
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not create globalhomes.yml", e);
            }
        }

        globalHomesConfig = YamlConfiguration.loadConfiguration(globalHomesFile);
    }

    private void setupPlayerHomesConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        playerHomesFile = new File(getDataFolder(), "playerhomes.yml");

        if (!playerHomesFile.exists()) {
            try {
                playerHomesFile.createNewFile();
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not create playerhomes.yml", e);
            }
        }

        playerHomesConfig = YamlConfiguration.loadConfiguration(playerHomesFile);
    }

    private void setupTranslations() {
        translationsDir = new File(getDataFolder(), "translations");
        if (!translationsDir.exists()) {
            translationsDir.mkdirs();
        }

        // Standard-Übersetzung (Englisch) aus Ressourcen laden
        saveResource("translations/texts_en.yml", false);
        saveResource("translations/texts_de.yml", false);

        // Verfügbare Übersetzungen laden
        loadTranslations();
    }

    private void loadTranslations() {
        translations.clear();

        File[] translationFiles = translationsDir.listFiles((dir, name) ->
                name.startsWith("texts_") && name.endsWith(".yml"));

        if (translationFiles != null) {
            for (File file : translationFiles) {
                try {
                    String locale = file.getName().replace("texts_", "").replace(".yml", "");
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    translations.put(locale, config);
                    getLogger().info("Loaded translation: " + locale);
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error loading translation file: " + file.getName(), e);
                }
            }
        }

        // Fallback: Englische Übersetzung aus Ressourcen laden
        if (!translations.containsKey("en")) {
            try {
                InputStream stream = getResource("translations/texts_en.yml");
                if (stream != null) {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(
                            new InputStreamReader(stream, StandardCharsets.UTF_8));
                    translations.put("en", config);
                }
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error loading default English translation", e);
            }
        }
        if (!translations.containsKey("de")) {
            try {
                InputStream stream = getResource("translations/texts_de.yml");
                if (stream != null) {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(
                            new InputStreamReader(stream, StandardCharsets.UTF_8));
                    translations.put("en", config);
                }
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error loading default German translation", e);
            }
        }
    }

    private String getMessage(Player player, String key) {
        // Sprache des Clients ermitteln
        String clientLanguage = player.getLocale().toLowerCase();

        // Sprache auf Standardformat normalisieren (z.B. "de_de" -> "de")
        String languageCode = clientLanguage.split("_")[0];

        // Passende Übersetzung finden
        FileConfiguration translation = translations.get(languageCode);
        if (translation == null) {
            // Fallback: Englisch
            translation = translations.get("en");
            if (translation == null) {
                return "Translation not found for key: " + key;
            }
        }

        // Nachricht aus der Übersetzung holen
        String message = translation.getString(key);
        if (message == null || message.isEmpty()) {
            // Fallback: Englisch
            FileConfiguration enTranslation = translations.get("en");
            if (enTranslation != null) {
                message = enTranslation.getString(key);
            }

            if (message == null || message.isEmpty()) {
                return "Message not found: " + key;
            }
        }

        return message;
    }

    private void loadHomesFromConfig() {
        try {
            for (String playerUuidString : homesConfig.getKeys(false)) {
                UUID playerUuid = UUID.fromString(playerUuidString);
                Map<String, Location> playerHomes = new HashMap<>();

                for (String homeName : homesConfig.getConfigurationSection(playerUuidString).getKeys(false)) {
                    Location location = (Location) homesConfig.get(playerUuidString + "." + homeName);
                    playerHomes.put(homeName, location);
                }

                homes.put(playerUuid, playerHomes);
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error loading homes from config", e);
        }
    }

    private void saveHomesToConfig() {
        try {
            // Clear existing data
            for (String key : homesConfig.getKeys(false)) {
                homesConfig.set(key, null);
            }

            // Save all homes
            for (Map.Entry<UUID, Map<String, Location>> playerEntry : homes.entrySet()) {
                String playerUuidString = playerEntry.getKey().toString();

                for (Map.Entry<String, Location> homeEntry : playerEntry.getValue().entrySet()) {
                    String path = playerUuidString + "." + homeEntry.getKey();
                    homesConfig.set(path, homeEntry.getValue());
                }
            }

            homesConfig.save(homesFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save homes to config", e);
        }
    }

    private void loadGlobalHomesFromConfig() {
        try {
            globalHomes.clear();
            for (String homeName : globalHomesConfig.getKeys(false)) {
                Location location = (Location) globalHomesConfig.get(homeName);
                if (location != null) {
                    globalHomes.put(homeName, location);
                }
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error loading global homes from config", e);
        }
    }

    private void saveGlobalHomesToConfig() {
        try {
            // Clear existing data
            for (String key : globalHomesConfig.getKeys(false)) {
                globalHomesConfig.set(key, null);
            }

            // Save all global homes
            for (Map.Entry<String, Location> entry : globalHomes.entrySet()) {
                globalHomesConfig.set(entry.getKey(), entry.getValue());
            }

            globalHomesConfig.save(globalHomesFile);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Could not save global homes to config", e);
        }
    }

    private void loadPlayerHomesFromConfig() {
        try {
            playerHomes.clear();
            for (String homeName : playerHomesConfig.getKeys(false)) {
                ConfigurationSection section = playerHomesConfig.getConfigurationSection(homeName);
                if (section != null) {
                    UUID owner = UUID.fromString(section.getString("owner"));
                    Location location = section.getLocation("location");
                    if (location != null) {
                        playerHomes.put(homeName, new PlayerHomeData(owner, location));
                    }
                }
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error loading player homes from config", e);
        }
    }

    private void savePlayerHomesToConfig() {
        try {
            // Clear existing data
            for (String key : playerHomesConfig.getKeys(false)) {
                playerHomesConfig.set(key, null);
            }

            // Save all player homes
            for (Map.Entry<String, PlayerHomeData> entry : playerHomes.entrySet()) {
                String homeName = entry.getKey();
                PlayerHomeData data = entry.getValue();

                playerHomesConfig.set(homeName + ".owner", data.getOwner().toString());
                playerHomesConfig.set(homeName + ".location", data.getLocation());
            }

            playerHomesConfig.save(playerHomesFile);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Could not save player homes to config", e);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerUuid = player.getUniqueId();

        if (cmd.getName().equalsIgnoreCase("sethome")) {
            if (args.length != 1) {
                player.sendMessage(getMessage(player, "usage.sethome"));
                return true;
            }

            // Check home limit
            int maxHomes = getConfig().getInt("max-homes", -1);
            if (maxHomes != -1) {
                int currentHomes = homes.containsKey(playerUuid) ? homes.get(playerUuid).size() : 0;
                if (currentHomes >= maxHomes) {
                    player.sendMessage(getMessage(player, "homes.limit.reached").replace("{max}", String.valueOf(maxHomes)));
                    return true;
                }
            }

            String homeName = args[0].toLowerCase();
            if (!homes.containsKey(playerUuid)) {
                homes.put(playerUuid, new HashMap<>());
            }

            homes.get(playerUuid).put(homeName, player.getLocation());
            saveHomesToConfig();
            player.sendMessage(getMessage(player, "home.set").replace("{home}", homeName));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("delhome")) {
            if (args.length != 1) {
                player.sendMessage(getMessage(player, "usage.delhome"));
                return true;
            }

            String homeName = args[0].toLowerCase();
            if (homes.containsKey(playerUuid) && homes.get(playerUuid).containsKey(homeName)) {
                homes.get(playerUuid).remove(homeName);
                saveHomesToConfig();
                player.sendMessage(getMessage(player, "home.deleted").replace("{home}", homeName));
            } else {
                player.sendMessage(getMessage(player, "home.not_exist").replace("{home}", homeName));
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("home")) {
            if (args.length != 1) {
                player.sendMessage(getMessage(player, "usage.home"));
                return true;
            }

            // Check cooldown
            int cooldown = getConfig().getInt("home-cooldown", -1);
            if (cooldown > 0) {
                long currentTime = System.currentTimeMillis();
                if (cooldowns.containsKey(playerUuid)) {
                    long lastUsed = cooldowns.get(playerUuid);
                    long timeLeft = ((lastUsed / 1000) + cooldown) - (currentTime / 1000);

                    if (timeLeft > 0) {
                        player.sendMessage(getMessage(player, "home.cooldown").replace("{time}", String.valueOf(timeLeft)));
                        return true;
                    }
                }

                // Set cooldown
                cooldowns.put(playerUuid, currentTime);
            }

            String homeName = args[0].toLowerCase();
            if (homes.containsKey(playerUuid) && homes.get(playerUuid).containsKey(homeName)) {
                player.teleport(homes.get(playerUuid).get(homeName));
                player.sendMessage(getMessage(player, "home.teleport").replace("{home}", homeName));
            } else {
                player.sendMessage(getMessage(player, "home.not_exist").replace("{home}", homeName));
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("homes")) {
            // List all homes of the player
            if (!homes.containsKey(playerUuid) || homes.get(playerUuid).isEmpty()) {
                player.sendMessage(getMessage(player, "homes.none"));
                return true;
            }

            Set<String> homeNames = homes.get(playerUuid).keySet();
            int maxHomes = getConfig().getInt("max-homes", -1);
            int currentHomes = homeNames.size();

            String limitText = (maxHomes == -1) ? getMessage(player, "homes.unlimited") : String.valueOf(maxHomes);
            player.sendMessage(getMessage(player, "homes.list.header")
                    .replace("{current}", String.valueOf(currentHomes))
                    .replace("{max}", limitText));

            // List all home names
            StringBuilder homesList = new StringBuilder();
            for (String home : homeNames) {
                if (homesList.length() > 0) {
                    homesList.append(", ");
                }
                homesList.append(home);
            }

            player.sendMessage(getMessage(player, "homes.list.items").replace("{homes}", homesList.toString()));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("homecount")) {
            if (!player.isOp()) {
                player.sendMessage(getMessage(player, "no_permission"));
                return true;
            }

            if (args.length != 1) {
                player.sendMessage(getMessage(player, "usage.homecount"));
                return true;
            }

            try {
                int newMax = Integer.parseInt(args[0]);
                getConfig().set("max-homes", newMax);
                saveConfig();
                player.sendMessage(getMessage(player, "homes.limit.set").replace("{max}", String.valueOf(newMax)));
            } catch (NumberFormatException e) {
                player.sendMessage(getMessage(player, "invalid_number"));
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("homecooldown")) {
            if (!player.isOp()) {
                player.sendMessage(getMessage(player, "no_permission"));
                return true;
            }

            if (args.length != 1) {
                player.sendMessage(getMessage(player, "usage.homecooldown"));
                return true;
            }

            try {
                int cooldown = Integer.parseInt(args[0]);
                if (cooldown < -1 || cooldown > 60) {
                    player.sendMessage(getMessage(player, "cooldown.range"));
                    return true;
                }

                getConfig().set("home-cooldown", cooldown);
                saveConfig();

                if (cooldown == -1) {
                    player.sendMessage(getMessage(player, "cooldown.disabled"));
                } else {
                    player.sendMessage(getMessage(player, "cooldown.set").replace("{time}", String.valueOf(cooldown)));
                }
            } catch (NumberFormatException e) {
                player.sendMessage(getMessage(player, "invalid_number"));
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("setglobalhome")) {
            if (!player.isOp()) {
                player.sendMessage(getMessage(player, "no_permission"));
                return true;
            }

            if (args.length != 1) {
                player.sendMessage(getMessage(player, "usage.setglobalhome"));
                return true;
            }

            String homeName = args[0].toLowerCase();
            globalHomes.put(homeName, player.getLocation());
            saveGlobalHomesToConfig(); // Sofort speichern
            player.sendMessage(getMessage(player, "globalhome.set").replace("{home}", homeName));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("globalhome")) {
            if (args.length != 1) {
                player.sendMessage(getMessage(player, "usage.globalhome"));
                return true;
            }

            String homeName = args[0].toLowerCase();
            if (globalHomes.containsKey(homeName)) {
                player.teleport(globalHomes.get(homeName));
                player.sendMessage(getMessage(player, "globalhome.teleport").replace("{home}", homeName));
            } else {
                player.sendMessage(getMessage(player, "globalhome.not_exist").replace("{home}", homeName));
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("globalhomes")) {
            if (globalHomes.isEmpty()) {
                player.sendMessage(getMessage(player, "globalhomes.none"));
                return true;
            }

            Set<String> homeNames = globalHomes.keySet();
            player.sendMessage(getMessage(player, "globalhomes.list.header"));

            StringBuilder homesList = new StringBuilder();
            for (String home : homeNames) {
                if (homesList.length() > 0) {
                    homesList.append(", ");
                }
                homesList.append(home);
            }

            player.sendMessage(getMessage(player, "globalhomes.list.items").replace("{homes}", homesList.toString()));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("delglobalhome") || cmd.getName().equalsIgnoreCase("dgh")) {
            if (!player.isOp()) {
                player.sendMessage(getMessage(player, "no_permission"));
                return true;
            }

            if (args.length != 1) {
                player.sendMessage(getMessage(player, "usage.delglobalhome"));
                return true;
            }

            String homeName = args[0].toLowerCase();
            if (globalHomes.containsKey(homeName)) {
                globalHomes.remove(homeName);
                saveGlobalHomesToConfig(); // Sofort speichern
                player.sendMessage(getMessage(player, "globalhome.deleted").replace("{home}", homeName));
            } else {
                player.sendMessage(getMessage(player, "globalhome.not_exist").replace("{home}", homeName));
            }
            return true;
        }

        // PlayerHome Commands
        if (cmd.getName().equalsIgnoreCase("setplayerhome") || cmd.getName().equalsIgnoreCase("sph")) {
            if (!player.hasPermission("infinitehomes.playerhomes.allowed")) {
                player.sendMessage(getMessage(player, "no_permission"));
                return true;
            }

            if (args.length != 1) {
                player.sendMessage(getMessage(player, "usage.setplayerhome"));
                return true;
            }

            String homeName = args[0].toLowerCase();

            // Check if player home name already exists
            if (playerHomes.containsKey(homeName)) {
                player.sendMessage(getMessage(player, "playerhome.already_exists").replace("{home}", homeName));
                return true;
            }

            playerHomes.put(homeName, new PlayerHomeData(playerUuid, player.getLocation()));
            savePlayerHomesToConfig();
            player.sendMessage(getMessage(player, "playerhome.set").replace("{home}", homeName));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("playerhome") || cmd.getName().equalsIgnoreCase("ph")) {
            if (args.length != 1) {
                player.sendMessage(getMessage(player, "usage.playerhome"));
                return true;
            }

            String homeName = args[0].toLowerCase();
            if (playerHomes.containsKey(homeName)) {
                player.teleport(playerHomes.get(homeName).getLocation());
                player.sendMessage(getMessage(player, "playerhome.teleport").replace("{home}", homeName));
            } else {
                player.sendMessage(getMessage(player, "playerhome.not_exist").replace("{home}", homeName));
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("playerhomes") || cmd.getName().equalsIgnoreCase("phs")) {
            if (playerHomes.isEmpty()) {
                player.sendMessage(getMessage(player, "playerhomes.none"));
                return true;
            }

            Set<String> homeNames = playerHomes.keySet();
            player.sendMessage(getMessage(player, "playerhomes.list.header"));

            StringBuilder homesList = new StringBuilder();
            for (String home : homeNames) {
                if (homesList.length() > 0) {
                    homesList.append(", ");
                }
                homesList.append(home);
            }

            player.sendMessage(getMessage(player, "playerhomes.list.items").replace("{homes}", homesList.toString()));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("delplayerhome") || cmd.getName().equalsIgnoreCase("dph")) {
            if (args.length != 1) {
                player.sendMessage(getMessage(player, "usage.delplayerhome"));
                return true;
            }

            String homeName = args[0].toLowerCase();
            if (playerHomes.containsKey(homeName)) {
                // Nur Spieler mit der Delete-Permission können PlayerHomes löschen
                if (player.hasPermission("infinitehomes.playerhomes.delete")) {
                    playerHomes.remove(homeName);
                    savePlayerHomesToConfig();
                    player.sendMessage(getMessage(player, "playerhome.deleted").replace("{home}", homeName));
                } else {
                    player.sendMessage(getMessage(player, "playerhome.contact_server_team"));
                }
            } else {
                player.sendMessage(getMessage(player, "playerhome.not_exist").replace("{home}", homeName));
            }
            return true;
        }

        return false;
    }

    // Innere Klasse für PlayerHome-Daten
    private static class PlayerHomeData {
        private UUID owner;
        private Location location;

        public PlayerHomeData(UUID owner, Location location) {
            this.owner = owner;
            this.location = location;
        }

        public UUID getOwner() {
            return owner;
        }

        public Location getLocation() {
            return location;
        }
    }
}