package net.samongi.SamEnchantments.Abilities;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.bukkit.Sound;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnPlayerInteract;
import net.samongi.LoreEnchantments.Util.StringUtil;
import net.samongi.LoreEnchantments.Util.ActionUtil.ActionType;
import net.samongi.SamEnchantments.SamEnchantments;

public class EnchantmentLeap extends LoreEnchantment implements OnPlayerInteract
{
  public int max_level;
  
  private ActionType action_type;
  
  private String leap_sound;
  
  private String vert_exp;
  private String para_exp;
  private String aim_exp;
  
  private boolean reset_verticle;
  private boolean reset_parallel;
  
  public EnchantmentLeap(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin); 
    
    this.max_level = plugin.getConfig().getInt("enchantments."+config_key+".max-level",10);

    String action_type_str = plugin.getConfig().getString("enchantments."+config_key+".action-type");
    if(action_type_str == null) this.action_type = null;
    else this.action_type = ActionType.getByString(action_type_str);
    
    this.vert_exp = plugin.getConfig().getString("enchantments."+config_key+".vertical-exp","0");
    vert_exp = vert_exp.toLowerCase().replace("pow", "Math.pow");
    this.para_exp = plugin.getConfig().getString("enchantments."+config_key+".parallel-exp","0");
    para_exp = para_exp.toLowerCase().replace("pow", "Math.pow");
    this.aim_exp = plugin.getConfig().getString("enchantments."+config_key+".aim-exp","0");
    aim_exp = aim_exp.toLowerCase().replace("pow", "Math.pow");
    
    this.reset_verticle = plugin.getConfig().getBoolean("enchantments."+config_key+".reset-vertical", false);
    this.reset_parallel = plugin.getConfig().getBoolean("enchantments."+config_key+".reset-parallel", false);
    
    this.leap_sound = plugin.getConfig().getString("enchantments."+config_key+".sound.leap","MAGMACUBE_JUMP");
    
    // We are going to use a script engine to evaluate the expressions in the config.
    ScriptEngine eng = SamEnchantments.getJavaScriptEngine();
    // testing the expressions first:
    SamEnchantments.debugLog("Testing expression for vertical velocity: '" + vert_exp + "'");
    try{eng.eval(vert_exp.replace("l", ""+ 0).replace("b", ""+0.0));}
    catch (ScriptException e1)
    {
      SamEnchantments.log("Expression: '" + vert_exp + "' under key '" + config_key + "' did not parse correctly");
      return;
    }
    
    SamEnchantments.debugLog("Testing expression for parallel velocity: '" + para_exp + "'");
    try{eng.eval(para_exp.replace("l", ""+ 0).replace("b", ""+0.0));}
    catch (ScriptException e1)
    {
      SamEnchantments.log("Expression: '" + para_exp + "' under key '" + config_key + "' did not parse correctly");
      return;
    }
    
    SamEnchantments.debugLog("Testing expression for aim velocity: '" + aim_exp + "'");
    try{eng.eval(aim_exp.replace("l", ""+ 0).replace("b", ""+0.0));}
    catch (ScriptException e1)
    {
      SamEnchantments.log("Expression: '" + aim_exp + "' under key '" + config_key + "' did not parse correctly");
      return;
    }
  }

  @Override
  public void onPlayerInteract(PlayerInteractEvent event, LoreEnchantment ench, String[] data)
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
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found level to be: " + ench_level);
    if(ench_level > this.max_level) ench_level = this.max_level;
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found 'true' level to be: " + ench_level);
    
    Vector base_vel = event.getPlayer().getLocation().getDirection();
    double base_x = base_vel.getX();
    double base_y = base_vel.getY();
    double base_z = base_vel.getZ();

    SamEnchantments.debugLog("Enchantment " + this.getName() + " found base to be [" + base_x + ", " + base_y + ", " + base_z + "]");
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found aim-exp to be: '" + this.aim_exp + "'");
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found para-exp to be: '" + this.para_exp + "'");
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found vert-exp to be: '" + this.vert_exp + "'");
    
    ScriptEngine eng = SamEnchantments.getJavaScriptEngine();

    // Getting the aim_velocity_portion
    double new_x_aim = 0;
    double new_y_aim = 0;
    double new_z_aim = 0;
    try
    {
      String var_exp_x = this.aim_exp.replace("l", ""+ench_level).replace("b", ""+ base_x);
      String var_exp_y = this.aim_exp.replace("l", ""+ench_level).replace("b", ""+ base_y);
      String var_exp_z = this.aim_exp.replace("l", ""+ench_level).replace("b", ""+ base_z);
      
      SamEnchantments.debugLog("Enchantment " + this.getName() + " found aim-exp for x to be: '" + var_exp_x + "'");
      SamEnchantments.debugLog("Enchantment " + this.getName() + " found aim-exp for y to be: '" + var_exp_y + "'");
      SamEnchantments.debugLog("Enchantment " + this.getName() + " found aim-exp for z to be: '" + var_exp_z + "'");
      
      Object ret_x = eng.eval(var_exp_x);
      Object ret_y = eng.eval(var_exp_y);
      Object ret_z = eng.eval(var_exp_z);
      
      double value_x = 0; 
      if(ret_x instanceof Integer) value_x = ((Integer)ret_x).intValue();
      else if(ret_x instanceof Double) value_x = ((Double)ret_x).doubleValue();
      new_x_aim = value_x;
      
      double value_y = 0; 
      if(ret_y instanceof Integer) value_y = ((Integer)ret_y).intValue();
      else if(ret_y instanceof Double) value_y = ((Double)ret_y).doubleValue();
      new_y_aim = value_y;
      
      double value_z = 0; 
      if(ret_z instanceof Integer) value_z = ((Integer)ret_z).intValue();
      else if(ret_z instanceof Double) value_z = ((Double)ret_z).doubleValue();
      new_z_aim = value_z;
    }
    catch (ScriptException e)
    {
      SamEnchantments.debugLog("Enchantment " + this.getName() + " found script exception with aim-exp");
      new_x_aim = 0;
      new_y_aim = 0;
      new_z_aim = 0;
    }
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found aim addition to be [" + new_x_aim + ", " + new_y_aim + ", " + new_z_aim + "]");
    
    // Getting the para_velocity_portion
    double new_x_para = 0;
    double new_z_para = 0;
    try
    {
      String var_exp_x = this.para_exp.replace("l", ""+ench_level).replace("b", ""+ base_x / Math.sqrt(Math.pow(base_x, 2) + Math.pow(base_z, 2)));
      String var_exp_z = this.para_exp.replace("l", ""+ench_level).replace("b", ""+ base_z / Math.sqrt(Math.pow(base_x, 2) + Math.pow(base_z, 2)));
      
      SamEnchantments.debugLog("Enchantment " + this.getName() + " found para-exp for x to be: '" + var_exp_x + "'");
      SamEnchantments.debugLog("Enchantment " + this.getName() + " found para-exp for z to be: '" + var_exp_z + "'");
      
      Object ret_x = eng.eval(var_exp_x);
      Object ret_z = eng.eval(var_exp_z);
      
      double value_x = 0; 
      if(ret_x instanceof Integer) value_x = ((Integer)ret_x).intValue();
      else if(ret_x instanceof Double) value_x = ((Double)ret_x).doubleValue();
      new_x_para = value_x;
      
      double value_z = 0; 
      if(ret_z instanceof Integer) value_z = ((Integer)ret_z).intValue();
      else if(ret_z instanceof Double) value_z = ((Double)ret_z).doubleValue();
      new_z_para = value_z;
    }
    catch (ScriptException e)
    {
      SamEnchantments.debugLog("Enchantment " + this.getName() + " found script exception with para-exp");
      new_x_para = 0;
      new_z_para = 0;
    }
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found para addition to be [" + new_x_para + ", " + 0 + ", " + new_z_para + "]");
    
    // Getting the vert_velocity_portion
    double new_y_vert = 0;
    try
    {
      String var_exp_y = this.vert_exp.replace("l", ""+ench_level).replace("b", ""+ 1);
      
      SamEnchantments.debugLog("Enchantment " + this.getName() + " found vert-exp for y to be: '" + var_exp_y + "'");
      
      Object ret_y = eng.eval(var_exp_y);
      
      double value_y = 0; 
      if(ret_y instanceof Integer) value_y = ((Integer)ret_y).intValue();
      else if(ret_y instanceof Double) value_y = ((Double)ret_y).doubleValue();
      new_y_vert = value_y;
    }
    catch (ScriptException e)
    {
      SamEnchantments.debugLog("Enchantment " + this.getName() + " found script exception with vert-exp");
      new_y_vert = 0;
    }
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found vert addtion to be [" + 0 + ", " + new_y_vert + ", " + 0 + "]");
    
    Vector aim_addition = new Vector(new_x_aim, new_y_aim, new_z_aim);
    Vector para_addition = new Vector(new_x_para, 0, new_z_para);
    Vector vert_addition = new Vector(0, new_y_vert, 0);
    
    Vector new_velocity = null;
    // base_vel
    if(this.reset_verticle) base_vel.setY(0);
    if(this.reset_parallel) base_vel.setX(0);
    if(this.reset_parallel) base_vel.setZ(0);
    new_velocity = base_vel.add(aim_addition).add(para_addition).add(vert_addition);
  
    event.getPlayer().setVelocity(new_velocity);
    
    Sound leap_sound = Sound.valueOf(this.leap_sound);
    if(leap_sound != null) event.getPlayer().getWorld().playSound(event.getPlayer().getLocation(), leap_sound, 1.0F, 1.0F);
  }
}
