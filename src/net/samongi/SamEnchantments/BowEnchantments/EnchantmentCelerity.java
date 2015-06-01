package net.samongi.SamEnchantments.BowEnchantments;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.bukkit.entity.Arrow;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnEntityShootBow;
import net.samongi.LoreEnchantments.Utilities.StringUtilities;
import net.samongi.SamEnchantments.SamEnchantments;

public class EnchantmentCelerity extends LoreEnchantment implements OnEntityShootBow
{
  @SuppressWarnings("unused")
  private final JavaPlugin plugin;
  private int max_level;
  private String exp;
  
  public EnchantmentCelerity(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin);
    this.plugin = plugin;
    
    this.max_level = plugin.getConfig().getInt("enchantments."+config_key+".max-level",10);
    this.exp = plugin.getConfig().getString("enchantments."+config_key+".exp","B * (1 + (0.5 * L))");
    this.exp = this.exp.toLowerCase().replace("pow", "Math.pow");
    
    // Testing the expression:
    SamEnchantments.debugLog("Testing expression: '" + this.exp + "'");
    ScriptEngine eng = SamEnchantments.getJavaScriptEngine();
    String var_exp = exp.replace("b", ""+1).replace("l", ""+1);
    try{eng.eval(var_exp);}
    catch (ScriptException e1)
    {
      SamEnchantments.log("Expression: '" + this.exp + "' under key '" + config_key + "' did not parse correctly");
      this.exp = "B * (1 + (0.5 * L))";
      return;
    }
  }

  @Override
  public void onEntityShootBow(EntityShootBowEvent event, LoreEnchantment ench, String[] data)
  {
    Arrow arrow = (Arrow)event.getProjectile();
   
    // Extracting the needed information from the data
    if(data.length < 1) return;
    String power = data[0];
    int ench_level = 0;
    try{ench_level = Integer.parseInt(power);} catch(NumberFormatException e){}
    if(ench_level == 0) ench_level = StringUtilities.numeralToInt(power);
    if(ench_level == 0) return;
    SamEnchantments.debugLog("Enchantment Celerity found level to be: " + ench_level);
    if(ench_level > this.max_level) ench_level = this.max_level;
    SamEnchantments.debugLog("Enchantment Celerity found 'true' level to be: " + ench_level);
    
    Vector base_velocity = arrow.getVelocity();
    double base_x = base_velocity.getX();
    double base_y = base_velocity.getY();
    double base_z = base_velocity.getZ();
    
    SamEnchantments.debugLog("Enchantment Celerity found base x-component to be: " + base_x);
    SamEnchantments.debugLog("Enchantment Celerity found base y-component to be: " + base_y);
    SamEnchantments.debugLog("Enchantment Celerity found base z-component to be: " + base_z);
    
    
    double new_x = 0;
    double new_y = 0;
    double new_z = 0;
    ScriptEngine eng = SamEnchantments.getJavaScriptEngine();
    try
    {
      String var_exp_x = exp.replace("b", ""+base_x).replace("l", ""+ench_level);
      String var_exp_y = exp.replace("b", ""+base_y).replace("l", ""+ench_level);
      String var_exp_z = exp.replace("b", ""+base_z).replace("l", ""+ench_level);
      
      Object ret_x = eng.eval(var_exp_x);
      Object ret_y = eng.eval(var_exp_y);
      Object ret_z = eng.eval(var_exp_z);
      
      double value_x = 0; 
      if(ret_x instanceof Integer) value_x = ((Integer)ret_x).intValue();
      else if(ret_x instanceof Double) value_x = ((Double)ret_x).doubleValue();
      new_x = value_x;
      
      double value_y = 0; 
      if(ret_y instanceof Integer) value_y = ((Integer)ret_y).intValue();
      else if(ret_y instanceof Double) value_y = ((Double)ret_y).doubleValue();
      new_y = value_y;
      
      double value_z = 0; 
      if(ret_z instanceof Integer) value_z = ((Integer)ret_z).intValue();
      else if(ret_z instanceof Double) value_z = ((Double)ret_z).doubleValue();
      new_z = value_z;
    }
    catch (ScriptException e){new_x = 0; new_y = 0; new_z = 0;}
    
    SamEnchantments.debugLog("Enchantment Celerity found new x-component to be: " + new_x);
    SamEnchantments.debugLog("Enchantment Celerity found new y-component to be: " + new_y);
    SamEnchantments.debugLog("Enchantment Celerity found new z-component to be: " + new_z);
    
    Vector new_velocity = new Vector(new_x, new_y, new_z);
    // if(new_velocity.lengthSquared() < base_velocity.lengthSquared()) new_velocity = base_velocity;
    
    SamEnchantments.debugLog("Enchantment Celerity found final x-component to be: " + new_velocity.getX());
    SamEnchantments.debugLog("Enchantment Celerity found final y-component to be: " + new_velocity.getY());
    SamEnchantments.debugLog("Enchantment Celerity found final z-component to be: " + new_velocity.getZ());
    
    arrow.setVelocity(new_velocity);
    event.setProjectile(arrow);
  }

}
