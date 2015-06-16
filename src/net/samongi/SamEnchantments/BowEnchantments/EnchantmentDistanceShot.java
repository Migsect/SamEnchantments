package net.samongi.SamEnchantments.BowEnchantments;

import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnEntityArrowHitEntity;
import net.samongi.LoreEnchantments.Interfaces.OnEntityShootBow;
import net.samongi.LoreEnchantments.Util.StringUtil;
import net.samongi.SamEnchantments.SamEnchantments;

public class EnchantmentDistanceShot extends LoreEnchantment implements OnEntityShootBow, OnEntityArrowHitEntity
{
  private int max_level;
  private String damage_exp;
  private boolean top_cap_damage;
  private boolean bot_cap_damage;
  private final int max_ticks = 1200;
  private Map<Integer, Location> shoot_locations = new HashMap<>();
  
  public EnchantmentDistanceShot(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin); 
    
    this.max_level = plugin.getConfig().getInt("enchantments."+config_key+".max-level",10);
    SamEnchantments.debugLog("Found max-level to be for Distance Shot: '" + max_level + "'");
    this.damage_exp = plugin.getConfig().getString("enchantments."+config_key+".damage-exp","B * (1 + 0.025 * D * L) - 9");
    damage_exp = damage_exp.toLowerCase().replace("pow", "Math.pow");
    this.top_cap_damage = plugin.getConfig().getBoolean("enchantments."+config_key+".top-cap", false);
    this.bot_cap_damage = plugin.getConfig().getBoolean("enchantments."+config_key+".bot-cap", false);
    
    // We are going to use a script engine to evaluate the expressions in the config.
    ScriptEngine eng = SamEnchantments.getJavaScriptEngine();
    // testing the expressions first:
    SamEnchantments.debugLog("Testing expression for distance damage: '" + damage_exp + "'");
    try{eng.eval(damage_exp.replace("l", ""+ 0).replace("b", ""+0.0).replace("d", ""+0.0));}
    catch (ScriptException e1)
    {
      SamEnchantments.log("Expression: '" + damage_exp + "' under key '" + config_key + "' did not parse correctly");
      return;
    }
  }

  @Override
  public void onEntityArrowHitEntity(EntityDamageByEntityEvent event, LoreEnchantment ench, String[] data)
  {
    // Extracting the needed information from the data
    if(data.length < 1) return;
    String power = data[0];
    int ench_level = 0;
    try{ench_level = Integer.parseInt(power);} catch(NumberFormatException e){}
    if(ench_level == 0) ench_level = StringUtil.numeralToInt(power);
    if(ench_level == 0) return;
    SamEnchantments.debugLog("Enchantment Distance Shot found level to be: " + ench_level);
    if(ench_level > this.max_level) ench_level = this.max_level;
    SamEnchantments.debugLog("Enchantment Distance Shot found 'true' level to be: " + ench_level);
    
    Arrow arrow = (Arrow)event.getDamager();
    double base_damage = event.getDamage();
    Location loc = arrow.getLocation();
    SamEnchantments.debugLog("Enchantment Distance Shot checking arrow id: " + event.getDamager().getEntityId());
    this.printShootLocations();
    if(!shoot_locations.containsKey(event.getDamager().getEntityId())) return;
    
    double distance = shoot_locations.get(event.getDamager().getEntityId()).distance(loc);
    SamEnchantments.debugLog("Enchantment Distance Shot found base damage to be: " + base_damage);
    SamEnchantments.debugLog("Enchantment Distance Shot found distance to be: " + distance);
    
    // Getting the damage
    double damage = 0;
    ScriptEngine eng = SamEnchantments.getJavaScriptEngine();
    try
    {
      String var_exp = damage_exp.replace("l", "" + ench_level).replace("b", "" + base_damage).replace("d", "" + distance);
      
      Object ret = eng.eval(var_exp);
      
      double value = 0; 
      if(ret instanceof Integer) value = ((Integer)ret).intValue();
      else if(ret instanceof Double) value = ((Double)ret).doubleValue();
      damage = value;
    }
    catch (ScriptException e){damage = 0;}
    SamEnchantments.debugLog("Enchantment Distance Shot found new damage to be: " + damage);
    
    // Damamge capping
    if(damage <= 0) damage = 0;
    if(top_cap_damage && damage > base_damage) damage = base_damage;
    if(bot_cap_damage && damage < base_damage) damage = base_damage;
    SamEnchantments.debugLog("Enchantment Distance Shot found finalized damage to be: " + damage);
    event.setDamage(damage);
    shoot_locations.remove(event.getDamager().getEntityId());
    
    // Particle effects
    /*
    Vector arrow_vel = arrow.getVelocity();
    ParticleRenderer renderer = new DustRenderer(255, 0, 0);
    double severity = 6; //Math.ceil(damage/base_damage);
    SamEnchantments.debugLog("Enchantment Distance Shot found severity to be: " + severity);
    int rays = (int) (3 * ((severity) - 1));
    double deviation = 6;
    Random rand = new Random();
    for(int i = 0 ; i < rays ; i++)
    {
      double x_rotate = (-deviation/2) + (deviation * rand.nextDouble());
      double y_rotate = (-deviation/2) + (deviation * rand.nextDouble());
      double z_rotate = (-deviation/2) + (deviation * rand.nextDouble());
      Vector new_ray = VectorUtil.rotateX(VectorUtil.rotateY(VectorUtil.rotateZ(arrow_vel, z_rotate), y_rotate), x_rotate);
      EffectVector effect_vector = new EffectVector(event.getDamager().getLocation(), new_ray, renderer);
      effect_vector.renderRandomly((int) (severity * 40));
    }
    */
  }

  @Override
  public void onEntityShootBow(EntityShootBowEvent event, LoreEnchantment ench, String[] data)
  {
    SamEnchantments.debugLog("Enchantment Distance Shot set Launched Projectile: " + event.getProjectile().getEntityId());
    this.shoot_locations.put(event.getProjectile().getEntityId(), event.getProjectile().getLocation());
    this.printShootLocations();
    
    CleanUpShoots task = new CleanUpShoots(event.getProjectile().getEntityId());
    task.runTaskLater(this.getOwningPlugin(), this.max_ticks);
  }

  private class CleanUpShoots extends BukkitRunnable
  {
    final int entity_id;
    CleanUpShoots(int entity_id)
    {
      this.entity_id = entity_id;
    }
    @Override
    public void run()
    {
      if(shoot_locations.containsKey(entity_id)) shoot_locations.remove(entity_id);
      
    }
    
  }
  
  private void printShootLocations()
  {
    SamEnchantments.debugLog("Printing out tracked arrow locations:");
    for(Integer i : this.shoot_locations.keySet())
    {
      SamEnchantments.debugLog("  " + i + ": " + shoot_locations.get(i).toString());
    }
  }
}
