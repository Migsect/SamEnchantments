package net.samongi.SamEnchantments.WeaponEnchantments;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnEntityDamageEntity;
import net.samongi.LoreEnchantments.Util.StringUtil;
import net.samongi.SamEnchantments.SamEnchantments;
import net.samongi.SamongiLib.Effects.EffectUtil;

public class EnchantmentFlourish extends LoreEnchantment implements OnEntityDamageEntity
{
  @SuppressWarnings("unused")
  private JavaPlugin plugin;
  private int max_level;
  private String exp;
  
  public EnchantmentFlourish(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin);
    this.plugin = plugin;
    
    this.max_level = plugin.getConfig().getInt("enchantments." + config_key + ".max-level", 10);
    this.exp = plugin.getConfig().getString("enchantments."+config_key+".exp","B * D * 0.25 * L");
    this.exp = this.exp.toLowerCase().replace("pow", "Math.pow");
    
    // Testing the expression:
    SamEnchantments.debugLog("Testing expression: '" + this.exp + "'");
    ScriptEngine eng = SamEnchantments.getJavaScriptEngine();
    String var_exp = exp.replace("b", ""+1).replace("l", ""+1).replace("d", ""+1);
    try{eng.eval(var_exp);}
    catch (ScriptException e1)
    {
      SamEnchantments.log("Expression: '" + this.exp + "' under key '" + config_key + "' did not parse correctly");
      this.exp = "B * D * 0.25 * L";
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
    SamEnchantments.debugLog("Enchantment Flourish found level to be: " + ench_level);
    if(ench_level > this.max_level) ench_level = this.max_level;
    SamEnchantments.debugLog("Enchantment Flourish found 'true' level to be: " + ench_level);
   
    // Base variables
    SamEnchantments.debugLog("Enchantment Flourish fall distance: " + damager.getFallDistance());
    float distance = damager.getFallDistance();
    SamEnchantments.debugLog("Enchantment Flourish base damage: " + event.getDamage());
    double base_damage = event.getDamage();
    // int ench_level set prior.
    
    double new_damage = 0;
    ScriptEngine eng = SamEnchantments.getJavaScriptEngine();
    try
    {
      String var_exp = exp.replace("b", ""+base_damage).replace("l", ""+ench_level).replace("d", ""+distance);
      Object ret = eng.eval(var_exp);
      double value = 0; 
      if(ret instanceof Integer) value = ((Integer)ret).intValue();
      else if(ret instanceof Double) value = ((Double)ret).doubleValue();
      new_damage = value;
    }
    catch (ScriptException e){new_damage = 0;}
    // Actually doing something.
    if(new_damage < base_damage) new_damage = base_damage;
    if(new_damage >= base_damage) EffectUtil.displayDustSphereCloud(entity.getEyeLocation(), 255, 215, 0,  (int) (30*new_damage/base_damage), 1.5);
    SamEnchantments.debugLog("Flourish Enchantment set damage: " + new_damage);
    event.setDamage(new_damage);
    
  }

}
