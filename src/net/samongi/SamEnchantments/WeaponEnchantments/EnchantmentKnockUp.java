package net.samongi.SamEnchantments.WeaponEnchantments;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnEntityDamageEntity;
import net.samongi.LoreEnchantments.Util.StringUtil;
import net.samongi.SamEnchantments.SamEnchantments;

public class EnchantmentKnockUp extends LoreEnchantment implements OnEntityDamageEntity
{
  private int max_level;
  private String velocity_exp;
  
  public EnchantmentKnockUp(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin); 
    
    this.max_level = plugin.getConfig().getInt("enchantments."+config_key+".max-level",10);
    SamEnchantments.debugLog("Found max-level to be for KnockUp: '" + max_level + "'");
    this.velocity_exp = plugin.getConfig().getString("enchantments."+config_key+".velocity-exp","0.5 * L");
    velocity_exp = velocity_exp.toLowerCase().replace("pow", "Math.pow");
    
    // We are going to use a script engine to evaluate the expressions in the config.
    ScriptEngine eng = SamEnchantments.getJavaScriptEngine();
    // testing the expressions first:
    SamEnchantments.debugLog("Testing expression for distance damage: '" + velocity_exp + "'");
    try{eng.eval(velocity_exp.replace("l", ""+ 0));}
    catch (ScriptException e1)
    {
      SamEnchantments.log("Expression: '" + velocity_exp + "' under key '" + config_key + "' did not parse correctly");
      return;
    }
  }

  @Override
  public void onEntityDamageEntity(EntityDamageByEntityEvent event, LoreEnchantment ench, String[] data)
  {
    // Extracting the needed information from the data
    if(data.length < 1) return;
    String power = data[0];
    int ench_level = 0;
    try{ench_level = Integer.parseInt(power);} catch(NumberFormatException e){}
    if(ench_level == 0) ench_level = StringUtil.numeralToInt(power);
    if(ench_level == 0) return;
    SamEnchantments.debugLog("Enchantment KnockUp found level to be: " + ench_level);
    if(ench_level > this.max_level) ench_level = this.max_level;
    SamEnchantments.debugLog("Enchantment KnockUp found 'true' level to be: " + ench_level);
    
    // Getting the damage
    double velocity = 0;
    ScriptEngine eng = SamEnchantments.getJavaScriptEngine();
    try
    {
      String var_exp = velocity_exp.replace("l", "" + ench_level);
      
      Object ret = eng.eval(var_exp);
      
      double value = 0; 
      if(ret instanceof Integer) value = ((Integer)ret).intValue();
      else if(ret instanceof Double) value = ((Double)ret).doubleValue();
      velocity = value;
    }
    catch (ScriptException e){velocity = 0;}
    SamEnchantments.debugLog("Enchantment KnockUp found added velocity to be: " + velocity);
    
    final double passed_velocity = velocity;
    BukkitRunnable task = new BukkitRunnable()
    {
      @Override
      public void run()
      {
        SamEnchantments.debugLog("Enchantment KnockUp found current velocity: " + event.getEntity().getVelocity().length());
        event.getEntity().setVelocity(event.getEntity().getVelocity().add(new Vector(0,passed_velocity,0)));
        SamEnchantments.debugLog("Enchantment KnockUp found new velocity: " + event.getEntity().getVelocity().length());
      }
    };
    task.runTaskLater(this.getOwningPlugin(), 1);
    //event.getDamager().setVelocity(event.getDamager().getVelocity().add(new Vector(0,velocity,0)));
  }
}
