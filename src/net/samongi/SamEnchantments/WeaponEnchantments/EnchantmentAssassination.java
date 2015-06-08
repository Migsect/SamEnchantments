package net.samongi.SamEnchantments.WeaponEnchantments;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnEntityDamageEntity;
import net.samongi.LoreEnchantments.Util.StringUtil;
import net.samongi.SamEnchantments.SamEnchantments;
import net.samongi.SamongiLib.Effects.EffectUtil;

public class EnchantmentAssassination extends LoreEnchantment implements OnEntityDamageEntity
{
  @SuppressWarnings("unused")
  private JavaPlugin plugin;
  private int max_level;
  private String exp;
  private double arc;

  public EnchantmentAssassination(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin);
    this.plugin = plugin;
    
    this.max_level = plugin.getConfig().getInt("enchantments." + config_key + ".max-level", 10);
    this.exp = plugin.getConfig().getString("enchantments."+config_key+".exp","B * (1 + (0.5 * L))");
    this.exp = this.exp.toLowerCase().replace("pow", "Math.pow");
    this.arc = plugin.getConfig().getDouble("enchantments." + config_key + ".arc", 45);
    
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
  public void onEntityDamageEntity(EntityDamageByEntityEvent event, LoreEnchantment ench, String[] data)
  {
    if(!(event.getDamager() instanceof LivingEntity)) return;
    if(!(event.getEntity() instanceof LivingEntity)) return;
    LivingEntity damager = (LivingEntity)event.getDamager();
    LivingEntity entity = (LivingEntity)event.getEntity();
    
    // Extracting the needed information from the data
    if(data.length < 1) return;
    String power = data[0];
    int ench_level = 0;
    try{ench_level = Integer.parseInt(power);} catch(NumberFormatException e){}
    if(ench_level == 0) ench_level = StringUtil.numeralToInt(power);
    if(ench_level == 0) return;
    SamEnchantments.debugLog("Enchantment Assassination found level to be: " + ench_level);
    if(ench_level > this.max_level) ench_level = this.max_level;
    SamEnchantments.debugLog("Enchantment Assassination found 'true' level to be: " + ench_level);
    
    // Time to check and see if the player's vectors are within acceptable bounds to allow the assassination.
    Vector d_direction = damager.getLocation().getDirection();
    Vector e_direction = entity.getLocation().getDirection();
    double found_arc = Math.toDegrees(d_direction.angle(e_direction));
    SamEnchantments.debugLog("Enchantment Assassination found arc to be: " + found_arc);
    if(found_arc > this.arc) return;
    
    
    SamEnchantments.debugLog("Enchantment Assassination base damage: " + event.getDamage());
    double base_damage = event.getDamage();
    
    double new_damage = 0;
    ScriptEngine eng = SamEnchantments.getJavaScriptEngine();
    try
    {
      String var_exp = exp.replace("b", ""+base_damage).replace("l", ""+ench_level);
      
      Object ret = eng.eval(var_exp);
      
      double value = 0; 
      if(ret instanceof Integer) value = ((Integer)ret).intValue();
      else if(ret instanceof Double) value = ((Double)ret).doubleValue();
      new_damage = value;
    }
    catch (ScriptException e){new_damage = 0;}
    // Actually doing something.
    if(new_damage < base_damage) new_damage = base_damage;
    SamEnchantments.debugLog("Assassination Enchantment set damage: " + new_damage);
    event.setDamage(new_damage);
    
    EffectUtil.displayDustSphereCloud(entity.getEyeLocation(), 255, 0, 0, 30, 1);
  }

}
