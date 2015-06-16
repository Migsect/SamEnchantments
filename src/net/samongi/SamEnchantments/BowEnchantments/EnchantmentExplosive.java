package net.samongi.SamEnchantments.BowEnchantments;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnArrowHit;
import net.samongi.LoreEnchantments.Util.StringUtil;
import net.samongi.SamEnchantments.SamEnchantments;

public class EnchantmentExplosive extends LoreEnchantment implements OnArrowHit
{
  private int max_level;
  
  private String power_exp;
  private boolean destroy_blocks;
  private boolean set_fire;
  
  public EnchantmentExplosive(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin);
    this.max_level = plugin.getConfig().getInt("enchantments." + config_key + ".max-level", 10);

   
    this.power_exp = plugin.getConfig().getString("enchantments."+config_key+".power-exp","4 * L");
    this.power_exp = this.power_exp.toLowerCase().replace("pow", "Math.pow");
    this.destroy_blocks = plugin.getConfig().getBoolean("enchantments."+config_key+".destroy-blocks", false);
    this.set_fire = plugin.getConfig().getBoolean("enchantments."+config_key+".set-fire", false);
    
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
  public void onArrowHit(ProjectileHitEvent event, LoreEnchantment ench, String[] data)
  {
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
    
    Arrow arrow = (Arrow) event.getEntity();
    
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
    SamEnchantments.debugLog("Enchantment Explosive found explosive power to be " + explosion_power);

    final double passed_explosion_power = explosion_power;
    Location loc = arrow.getLocation();
    BukkitRunnable task = new BukkitRunnable()
    {
      @Override
      public void run()
      {
        arrow.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), (float) passed_explosion_power, set_fire, destroy_blocks);
      }
    };
    task.runTaskLater(this.getOwningPlugin(), 1);
    
  }
}
