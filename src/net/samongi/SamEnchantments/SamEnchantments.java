package net.samongi.SamEnchantments;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import net.samongi.LoreEnchantments.LoreEnchantmentsAPI;
import net.samongi.LoreEnchantments.LoreEnchantments;
import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.SamEnchantments.Abilities.EnchantmentShadowStep;
import net.samongi.SamEnchantments.BowEnchantments.EnchantmentCelerity;
import net.samongi.SamEnchantments.BowEnchantments.EnchantmentVolley;
import net.samongi.SamEnchantments.PotionEnchantments.EnchantmentCoated;
import net.samongi.SamEnchantments.PotionEnchantments.EnchantmentTipped;
import net.samongi.SamEnchantments.WeaponEnchantments.EnchantmentAssassination;
import net.samongi.SamEnchantments.WeaponEnchantments.EnchantmentFireCritical;
import net.samongi.SamEnchantments.WeaponEnchantments.EnchantmentFlourish;

import org.bukkit.plugin.java.JavaPlugin;

public class SamEnchantments extends JavaPlugin
{
  static private Logger logger;
  static private boolean debug = false;
  static private ScriptEngine js_eng;
  
  private List<LoreEnchantment> registred_enchantments = new ArrayList<>();
  
  public void onEnable()
  {
 // Getting the plugin's logger
    logger = this.getLogger();
    
    // initial config handling.
    File config_file = new File(this.getDataFolder(),"config.yml");
    if(!config_file.exists())
    {
      SamEnchantments.log("Found no config file, copying over defaults...");
      this.getConfig().options().copyDefaults(true);
      this.saveConfig();
    }
    debug = this.getConfig().getBoolean("debug",true); // Grabbing the debug state.
    
    // Registering all the enchantments
    registerEnchantments();
    
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
  
  private final void registerEnchantments()
  {
    LoreEnchantmentsAPI api = LoreEnchantments.getAPI(this);
    if(this.getConfig().getConfigurationSection("enchantments") == null)
    {
      SamEnchantments.log("Found no Enchantments configured. There will be no function if you do not configure any enchantments.");
      return;
    }
    Set<String> keys = this.getConfig().getConfigurationSection("enchantments").getKeys(false);
    for(String k : keys)
    {
      String name = this.getConfig().getString("enchantments."+k+".name");
      String type = this.getConfig().getString("enchantments."+k+".type");
      LoreEnchantment new_ench = null;
      switch(type.toUpperCase())
      {
        case "VOLLEY":          new_ench = new EnchantmentVolley(this, name, k); break;
        case "TIPPED":          new_ench = new EnchantmentTipped(this, name, k); break;
        case "COATED":          new_ench = new EnchantmentCoated(this, name, k); break;
        case "FLOURISH":        new_ench = new EnchantmentFlourish(this, name, k); break;
        case "ASSASSINATION":   new_ench = new EnchantmentAssassination(this, name, k); break;
        case "CELERITY":        new_ench = new EnchantmentCelerity(this, name, k); break;
        case "FIRE_CRITICAL":   new_ench = new EnchantmentFireCritical(this, name, k); break;
        case "SHADOW_STEP":     new_ench = new EnchantmentShadowStep(this, name, k); break;
        default: continue;
      }
      api.registerEnchantment(new_ench, this);
    }
  }
  // To be used with commands for reloading
  @SuppressWarnings("unused")
  private final void deregisterEnchantments()
  {
    LoreEnchantmentsAPI api = LoreEnchantments.getAPI(this);
    for(int i = registred_enchantments.size()-1 ; i <= 0; i--)
    {
      api.deregisterEnchantment(registred_enchantments.get(i));
      registred_enchantments.remove(i);
    }
  }
  static final public ScriptEngine getJavaScriptEngine()
  {
    if(js_eng == null)
    {
      ScriptEngineManager mgr = new ScriptEngineManager();
      js_eng = mgr.getEngineByName("JavaScript");
    }
    return js_eng;
  }
}
