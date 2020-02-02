package net.thirdshift.tokens;

import net.milkbowl.vault.economy.Economy;
import net.thirdshift.tokens.commands.CommandTokensReload;
import net.thirdshift.tokens.database.mysql.MySQLHandler;
import net.thirdshift.tokens.item.TokenItemStack;
import net.thirdshift.tokens.util.BStats;
import net.thirdshift.tokens.util.TokensSpigotUpdater;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class Tokens extends JavaPlugin {

    private final Logger log = this.getLogger();

    public boolean mysqlEnabled = false;
    private MySQLHandler mysql;
    //private Database sqllite;
    public boolean hasFactions;
    public boolean factionsEnabled = false;
    public int tokenToFactionPower;
    public boolean hasMCMMO;
    public boolean mcmmoEnabled = false;
    public int tokensToMCMMOLevels;
    public boolean hasCombatLogX;
    public boolean combatLogXEnabled;
    public boolean combatLogXBlockTokens = false;
    public boolean hasVault;
    public boolean vaultEnabled;
    public boolean vaultBuy;
    public boolean vaultSell = false;
    public double vaultBuyPrice;
    public double vaultSellPrice = 0.0D;
    public boolean useEnder = true;
    public static Economy vaultEcon = null;
    public TokenItemStack tokenHandler = new TokenItemStack();
    //public TokenMenus tokenMenus;
    private TokensSpigotUpdater updater = new TokensSpigotUpdater(this, 71941);

    @Override
    public void onEnable() {
        int bstatsID=5849;
        BStats bStats = new BStats(this, bstatsID);

        this.checkUpdates();

        this.saveDefaultConfig();
        this.reloadConfig();

        this.addCommands();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if(this.mysqlEnabled){
            mysql.stopSQLConnection();//Cut off any loose bois
        }
    }

    public void addCommands(){
        this.getCommand("tokens").setExecutor(new CommandTokensReload(this));
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        this.useEnder = this.getConfig().getBoolean("use-enderchest");
        this.mysqlEnabled = this.getConfig().getBoolean("MySQL.Enabled");
        this.vaultEnabled = this.getConfig().getBoolean("VaultEco.Enabled");
        this.vaultEnabled = this.getConfig().getBoolean("VaultEco.Enabled");
        this.vaultBuy = this.getConfig().getBoolean("VaultEco.Buy-Tokens");
        this.vaultBuyPrice = this.getConfig().getDouble("VaultEco.Buy-Price");
        this.vaultSell = this.getConfig().getBoolean("VaultEco.Sell-Tokens");
        this.vaultSellPrice = this.getConfig().getDouble("VaultEco.Sell-Price");
        this.factionsEnabled = this.getConfig().getBoolean("Factions.Enabled");
        this.tokenToFactionPower = this.getConfig().getInt("Factions.Tokens-To-Power");
        this.combatLogXEnabled = this.getConfig().getBoolean("CombatLogX.Enabled");
        this.combatLogXBlockTokens = this.getConfig().getBoolean("CombatLogX.Block-Tokens");
        this.mcmmoEnabled = this.getConfig().getBoolean("mcMMO.Enabled");
        this.tokensToMCMMOLevels = this.getConfig().getInt("mcMMO.Tokens-To-Levels");
        if (this.mysqlEnabled) {
            this.mysql = new MySQLHandler(this);
            this.mysql.username = this.getConfig().getString("MySQL.Username");
            this.mysql.password = this.getConfig().getString("MySQL.Password");
            this.mysql.dbName = this.getConfig().getString("MySQL.Database-Name");
            this.mysql.dbPORT = this.getConfig().getString("MySQL.Server.Port");
            this.mysql.dbAddress = this.getConfig().getString("MySQL.Server.Address");
            this.mysql.dbSSL = this.getConfig().getString("MySQL.Server.SSL");
            this.mysql.startSQLConnection();
            this.getLogger().info("Using MySQL");
        }/*
        TODO: Re-add SQLLite
        else {
            this.sqllite = new SQLite(this);
            this.sqllite.load();
            this.getLogger().info("Using SQLlite");
        }*/

        // Factions Check
        Plugin factionsPlug = this.getServer().getPluginManager().getPlugin("Factions");
        if(factionsPlug!=null && factionsPlug.isEnabled()){
            this.hasFactions=true;
        }else if (factionsPlug==null && this.factionsEnabled){
            this.getLogger().warning("Factions addon is enabled but Factions is not installed on the server!");
        }

        // Vault Check
        Plugin vaultPlug = this.getServer().getPluginManager().getPlugin("Vault");
        if (vaultPlug != null && vaultPlug.isEnabled()) {
            this.hasVault=true;
        }else if (vaultPlug==null && this.vaultEnabled){
            this.getLogger().warning("Vault addon is enabled but Vault is not installed on the server!");
        }

        // CombatLogX Check
        Plugin combPlug = this.getServer().getPluginManager().getPlugin("CombatLogX");
        if(combPlug!=null && combPlug.isEnabled()){
            this.hasCombatLogX=true;
        }else if (combPlug==null && this.combatLogXEnabled){
            this.getLogger().warning("CombatLogX addon is enabled but CombatLogX is not installed on the server!");
        }

        // mcMMO Check
        Plugin mcmmoPlug = this.getServer().getPluginManager().getPlugin("mcMMO");
        if(mcmmoPlug!=null&&mcmmoPlug.isEnabled()){
            this.hasMCMMO=true;
        }else if (mcmmoPlug==null&&this.mcmmoEnabled){
            this.getLogger().warning("mcMMO addon is enabled but mcMMO is not installed on the server!");
        }

        // Prevents people like https://www.spigotmc.org/members/jcv.510317/ saying the plugin is broken <3
        if (!vaultEnabled && !factionsEnabled && !mcmmoEnabled) {
            this.getLogger().warning("You don't have any supported plugins enabled.");
        }
        initializeTokensAddons();
    }

    public void initializeTokensAddons(){
        if(this.vaultEnabled && this.hasVault){
            vaultIntegration();
        }
        if(this.factionsEnabled && this.hasFactions){
            factionsIntegration();
        }
        if(this.combatLogXEnabled && this.hasCombatLogX){
            combatLogXIntegration();
        }
        if(this.mcmmoEnabled && this.hasMCMMO){
            mcmmoIntegration();
        }
    }

    public void vaultIntegration(){
        if(vaultEcon == null){
            if(!setupEconomy()){
                this.getLogger().warning("No Vault economy is present but the addon is enabled!");
            }
        }
        this.vaultBuy = this.getConfig().getBoolean("VaultEco.Buy-Tokens");
        this.vaultBuyPrice = this.getConfig().getDouble("VaultEco.Buy-Price");
        this.vaultSell = this.getConfig().getBoolean("VaultEco.Sell-Tokens");
        this.vaultSellPrice = this.getConfig().getDouble("VaultEco.Sell-Price");
    }

    public void factionsIntegration(){
        this.getLogger().info("Hooked into Factions");
        this.hasFactions = true;
        this.factionsEnabled = this.getConfig().getBoolean("Factions.Enabled");
        this.tokenToFactionPower = this.getConfig().getInt("Factions.Tokens-To-Power");
    }

    public void mcmmoIntegration(){
        this.getLogger().info("Hooked into McMMO");
    }

    public void combatLogXIntegration(){
        this.getLogger().info("Hooked into CombatLogX");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        vaultEcon = rsp.getProvider();
        return true;
    }

    public static Economy getEconomy() {
        return vaultEcon;
    }

    private void checkUpdates(){
        try {
            if (updater.checkForUpdates()) {
                this.getLogger().info("An update was found! New version: " + updater.getLatestVersion() + " download link: " + updater.getResourceURL());
            } else {
                this.getLogger().info("No newer version available.");
            }
        } catch (Exception var2) {
            this.getLogger().warning("Could not check for updates! Stacktrace:");
            var2.printStackTrace();
        }
    }

}