package net.samongi.SamEnchantments.Abilities;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnPlayerInteract;
import net.samongi.LoreEnchantments.Util.StringUtil;
import net.samongi.LoreEnchantments.Util.ActionUtil.ActionType;
import net.samongi.SamEnchantments.SamEnchantments;

public class EnchantmentExplode extends LoreEnchantment implements OnPlayerInteract
{
  private int max_level;
  
  private String power_exp;
  private boolean destroy_blocks;
  private boolean set_fire;
  
  private ActionType action_type;
  
  
  
  public EnchantmentExplode(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin);
    this.max_level = plugin.getConfig().getInt("enchantments." + config_key + ".max-level", 10);
    
    this.power_exp = plugin.getConfig().getString("enchantments."+config_key+".power-exp","4 * L");
    this.power_exp = this.power_exp.toLowerCase().replace("pow", "Math.pow");
    this.destroy_blocks = plugin.getConfig().getBoolean("enchantments."+config_key+".destroy-blocks", false);
    this.set_fire = plugin.getConfig().getBoolean("enchantments."+config_key+".set-fire", false);
    
    String action_type_str = plugin.getConfig().getString("enchantments."+config_key+".action-type");
    if(action_type_str == null) this.action_type = null;
    else this.action_type = ActionType.getByString(action_type_str);
    
    // Testing the expressions:
    ScriptEngine eng = SamEnchantments.getJavaScriptEngine();
    
    SamEnchantments.debugLog("Testing expression: '" + this.power_exp + "'");
    String var_exp1 = power_exp.replace("b", ""+1).replace("l", ""+1);
    try{eng.eval(var_exp1);}
    catch (ScriptException e1)
    {
      SamEnchantments.log("Expression: '" + this.power_exp + "' under key '" + config_key + "' did not parse correctly");
      this.power_exp = "4 * L";
      return;
    }
    
  }

  @Override
  public void onPlayerInteract(PlayerInteractEvent event, LoreEnchantment ench,
      String[] data)
  {
    ActionType action = null;
    if(data.length > 1) action = ActionType.getByString(data[1]);
    if(action == null) action = this.action_type;
    if(action == null) return;
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found action to be: " + action);
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found event-action to be: " + ActionType.getActionType(event));
    if(!action.isSimilar(ActionType.getActionType(event))) return;
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found actions to be similar");
    
    if(data.length < 1) return;
    
    // Extracting the needed information from the data
    String power = data[0];
    int ench_level = 0;
    try{ench_level = Integer.parseInt(power);} catch(NumberFormatException e){}
    if(ench_level == 0) ench_level = StringUtil.numeralToInt(power);
    if(ench_level == 0) return;
    SamEnchantments.debugLog("Enchantment Shadow Step found level to be: " + ench_level);
    if(ench_level > this.max_level) ench_level = this.max_level;
    SamEnchantments.debugLog("Enchantment Shadow Step found 'true' level to be: " + ench_level);
    

    ScriptEngine eng = SamEnchantments.getJavaScriptEngine();
    // Getting the max distance
    double explosion_power = 0;
    try
    {
      String var_exp = this.power_exp.replace("l", ""+ench_level);
      
      Object ret = eng.eval(var_exp);
      
      double value = 0; 
      if(ret instanceof Integer) value = ((Integer)ret).intValue();
      else if(ret instanceof Double) value = ((Double)ret).doubleValue();
      explosion_power = value;
    }
    catch (ScriptException e){explosion_power = 0;}
    SamEnchantments.debugLog("Enchantment Explode found explosive power to be " + explosion_power);
    
    
    
    // Do specific enchantment stuff here"
    Player player = event.getPlayer();
    Location loc = player.getLocation();
    player.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), (float) explosion_power, this.set_fire, this.destroy_blocks);
    
  }

}
