package com.bloccrew.plugin.taxes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Taxes extends JavaPlugin{
	
	public static Economy econ = null;
	File configFile;
    FileConfiguration config;
    int interval;
    double lowRate, midRate, highRate, lowBal, midBal;
    String recipient, nextCollection;

	public void onEnable(){
		if (!setupEconomy() ) {
			getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		Plugin plugin = Bukkit.getPluginManager().getPlugin("BlocTaxes");
		configFile = new File(plugin.getDataFolder(), "config.yml");
		
		try {
	        firstRun();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    config = new YamlConfiguration();
	    loadYamls();
		
		getLogger().info("[BlocTaxes] BlocTaxes has been enabled");
	}
	
	public void onDisable(){
		getLogger().info("[BlocTaxes] BlocTaxes has been disabled");
	}
	
	private void firstRun(){
		if(!configFile.exists()){
	        configFile.getParentFile().mkdirs();
	        copy(getResource("config.yml"), configFile);
	    }
	}
	
	private void copy(InputStream in, File file) {
	    try {
	        OutputStream out = new FileOutputStream(file);
	        byte[] buf = new byte[1024];
	        int len;
	        while((len=in.read(buf))>0){
	            out.write(buf,0,len);
	        }
	        out.close();
	        in.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	public void loadYamls() {
	    try {
	        config.load(configFile);
			lowRate = config.getDouble("taxes.lowtax.rate");
			lowBal = config.getDouble("taxes.lowtax.bal");
			
			midRate = config.getDouble("taxes.midtax.rate");
			midBal = config.getDouble("taxes.midtax.bal");
			
			highRate = config.getDouble("taxes.hightax.rate");
			
			interval = config.getInt("interval");
			nextCollection = config.getString("nextcollection");
			recipient = config.getString("recipient");
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}
	/*
	public String getBracket(OfflinePlayer p){
		double bal = econ.getBalance(p);
		String bracket = "";
		
		if(bal >= taxes[2][1]){
			bracket = "High";
		}else if(bal >= taxes[1][1]){
			bracket = "Middle";
		}else if(bal >= taxes[0][1]){
			bracket = "Low";
		}
		
		return bracket;
	}
	
	public double getTaxRate(OfflinePlayer p){
		double bal = econ.getBalance(p), taxRate = 0;

		if(bal >= taxes[2][1]){
			taxRate = taxes[2][0];
		}else if(bal >= taxes[1][1]){
			taxRate = taxes[1][0];
		}else if(bal >= taxes[0][1]){
			taxRate = taxes[0][0];
		}

		return taxRate;
	}
	*/
	
	public double calcTax(OfflinePlayer p){
		double bal = econ.getBalance(p), tax = 0;
		
		if(bal <= lowBal){
			tax = (bal * lowRate);
		}else if(bal <= midBal){
			tax = (lowBal * lowRate) + ((bal - lowBal) * midRate);
		}else{
			tax = (lowBal * lowRate) + ((midBal - lowBal) * midRate) + ((bal - midBal) * highRate);
		}
		
		return tax;
	}
	
	public double calcTaxes(){
		double totalTax = 0;
		for(OfflinePlayer p : Bukkit.getServer().getOfflinePlayers()){
			double tax = calcTax(p);
			totalTax += tax;
		}
		return totalTax;
	}
	
	public double collectTaxes(){
		double totalTax = 0;
		for(OfflinePlayer p : Bukkit.getServer().getOfflinePlayers()){
			double tax = calcTax(p);
			totalTax += tax;
			econ.withdrawPlayer(p, tax);
			econ.bankDeposit(recipient, tax);
		}
		return totalTax;
	}
	
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("tax")){
			if(args.length > 0){
				if(args[0].equalsIgnoreCase("amount")){
					if(sender.hasPermission("tax.collect")){
						sender.sendMessage(econ.format(calcTaxes()) + " could be collected in taxes.");
						return true;
					}
				}else if(args[0].equalsIgnoreCase("collect")){
					if(sender.hasPermission("tax.collect")){
						sender.sendMessage("How dare you evil person!");
						sender.sendMessage(econ.format(collectTaxes()) + " has been collected in taxes.");
						return true;
					}
				}else{
					if(sender.hasPermission("tax.info.other")){
						for(OfflinePlayer p : Bukkit.getServer().getOfflinePlayers()){
							if(p.getName().equalsIgnoreCase(args[0])){
								sender.sendMessage(ChatColor.UNDERLINE + p.getName() + ChatColor.RESET + 
										"\nBalance: " + econ.format(econ.getBalance(p)) +
										"\nTax: " + econ.format(calcTax(p)));
								return true;
							}
						}
					}
				}
			}else{
				if(sender instanceof Player){
					if(sender.hasPermission("tax.info")){
						OfflinePlayer p = ((Player) sender).getPlayer();
						sender.sendMessage(ChatColor.UNDERLINE + sender.getName() + ChatColor.RESET + 
								"\nBalance: " + econ.format(econ.getBalance(p)) +
								"\nTax: " + econ.format(calcTax(p)));
						return true;
					}
				}
			}
		}
		return false;
	}
}
