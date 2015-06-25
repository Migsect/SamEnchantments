package net.samongi.SamEnchantments.BowEnchantments;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnEntityArrowHitEntity;
import net.samongi.LoreEnchantments.Util.StringUtil;
import net.samongi.SamEnchantments.SamEnchantments;
import net.samongi.SamongiLib.Effects.EffectUtil;

public class EnchantmentHeadShot extends LoreEnchantment implements OnEntityArrowHitEntity
{
  private int max_level;
  private String exp;
  private double head_size;
  
  public EnchantmentHeadShot(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin);
    this.max_level = plugin.getConfig().getInt("enchantments." + config_key + ".max-level", 10);
    
    this.exp = plugin.getConfig().getString("enchantments."+config_key+".exp","B * (1 + (0.5 * L))");
    this.exp = this.exp.toLowerCase().replace("pow", "Math.pow");
    
    this.head_size = plugin.getConfig().getDouble("enchantments." + config_key + ".radius", 0.3);
    
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
  public void onEntityArrowHitEntity(EntityDamageByEntityEvent event, LoreEnchantment ench, String[] data)
  {
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

    if(!(event.getEntity() instanceof LivingEntity)) return;
    LivingEntity entity = (LivingEntity)event.getEntity();
    Arrow arrow = (Arrow)event.getDamager();
    
    Location entity_loc = entity.getLocation();
    Vector arrow_direction = event.getDamager().getVelocity();
    Location head_loc = new Location(entity_loc.getWorld(), entity_loc.getX(), entity_loc.getY() + entity.getEyeHeight(), entity_loc.getZ());
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found 'entity_loc' to be: ["+ entity_loc.getX() + ", " + entity_loc.getY() + ", " + entity_loc.getZ() + "]");
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found 'head_loc' to be: ["+ head_loc.getX() + ", " + head_loc.getY() + ", " + head_loc.getZ() + "]");
    
    // This is the degress of difference needed to indicate a headshot
    double radius_sqr = arrow.getLocation().distanceSquared(head_loc);
    double degree_sqr = (Math.pow(head_size,2)) / radius_sqr;
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found 'degree_sqr' to be: " + degree_sqr);
    
    // Getting the vector from the arrow to the player's head.
    double x = head_loc.getX() - arrow.getLocation().getX();
    double y = head_loc.getY() - arrow.getLocation().getY();
    double z = head_loc.getZ() - arrow.getLocation().getZ();
    
    Vector v = new Vector(x, y, z);
    double degree_comp = Math.pow(arrow_direction.angle(v), 2);
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found 'degree_comp' to be: " + degree_comp);
    if(degree_comp >= degree_sqr) return;
    
    SamEnchantments.debugLog("Enchantment " + this.getName() + " base damage: " + event.getDamage());
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
    SamEnchantments.debugLog("" + this.getName() + " Enchantment set damage: " + new_damage);
    event.setDamage(new_damage);
    
    EffectUtil.displayDustSphereCloud(entity.getEyeLocation(), 255, 0, 0, 30 * ench_level, 1);
  }

}
