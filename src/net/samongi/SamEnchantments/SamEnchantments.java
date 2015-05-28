package net.samongi.SamEnchantments;

import java.util.logging.Logger;

import net.samongi.LoreEnchantments.LoreEnchantmentsAPI;
import net.samongi.LoreEnchantments.LoreEnchantments;
import net.samongi.SamEnchantments.BowEnchantments.EnchantmentVolley;
import net.samongi.SamEnchantments.PotionEnchantments.EnchantmentCoated;

import org.bukkit.plugin.java.JavaPlugin;

public class SamEnchantments extends JavaPlugin
{
  static private Logger logger;
  static private boolean debug = false;
  
  public void onEnable()
  {
 // Getting the plugin's logger
    logger = this.getLogger();
    
    // config handling.
    this.getConfig().options().copyDefaults(true);
    this.saveConfig();
    debug = this.getConfig().getBoolean("debug"); // Grabbing the debug state.
    
    // Registering all the enchantments
    LoreEnchantmentsAPI api = LoreEnchantments.getAPI(this);
    api.registerEnchantment(new EnchantmentVolley(this), this);
    api.registerEnchantment(new EnchantmentCoated(this), this);
    
  }
  
  static final public void log(String to_log)
  {
    logger.info(to_log);
  }
  static final public void debugLog(String to_log)
  {
    if(debug == true) logger.info(to_log);
  }
  static final public boolean debug(){return debug;}
  
}
