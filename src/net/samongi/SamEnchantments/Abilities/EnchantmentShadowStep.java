package net.samongi.SamEnchantments.Abilities;

import java.util.HashSet;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import net.minecraft.server.v1_8_R2.Material;
import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnPlayerInteract;
import net.samongi.LoreEnchantments.Interfaces.OnPlayerInteractEntity;
import net.samongi.LoreEnchantments.Util.EntityUtil;
import net.samongi.LoreEnchantments.Util.Recharging;
import net.samongi.LoreEnchantments.Util.StringUtil;
import net.samongi.SamEnchantments.SamEnchantments;
import net.samongi.SamongiLib.Effects.EffectUtil;

public class EnchantmentShadowStep extends LoreEnchantment implements OnPlayerInteract, OnPlayerInteractEntity, Recharging
{
  private JavaPlugin plugin;
  private int max_level;
  private String max_distance_exp;
  private String min_distance_exp;
  private String recharge_exp;
  private double behind_distance;
  
  private Set<ItemStack> recharging_items = new HashSet<>();
  private Set<RechargeLater> recharge_tasks = new HashSet<>();
  
  public EnchantmentShadowStep(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin);
    this.plugin = plugin;
    
    this.max_level = plugin.getConfig().getInt("enchantments." + config_key + ".max-level", 10);
    this.max_distance_exp = plugin.getConfig().getString("enchantments."+config_key+".max-dist-exp","5 * L");
    this.max_distance_exp = this.max_distance_exp.toLowerCase().replace("pow", "Math.pow");
    this.min_distance_exp = plugin.getConfig().getString("enchantments."+config_key+".min-dist-exp","5");
    this.min_distance_exp = this.min_distance_exp.toLowerCase().replace("pow", "Math.pow");
    this.recharge_exp = plugin.getConfig().getString("enchantments."+config_key+".recharge-exp","2.5 * L + 10");
    this.recharge_exp = this.recharge_exp.toLowerCase().replace("pow", "Math.pow");
    this.behind_distance = plugin.getConfig().getDouble("enchantments." + config_key + ".behind-distance", 1.5);
    
    // Testing the expressions:
    SamEnchantments.debugLog("Testing expression: '" + this.max_distance_exp + "'");
    ScriptEngine eng = SamEnchantments.getJavaScriptEngine();
    String var_exp1 = max_distance_exp.replace("b", ""+1).replace("l", ""+1);
    try{eng.eval(var_exp1);}
    catch (ScriptException e1)
    {
      SamEnchantments.log("Expression: '" + this.max_distance_exp + "' under key '" + config_key + "' did not parse correctly");
      this.max_distance_exp = "5 * L";
      return;
    }
    
    SamEnchantments.debugLog("Testing expression: '" + this.min_distance_exp + "'");
    String var_exp2 = max_distance_exp.replace("b", ""+1).replace("l", ""+1);
    try{eng.eval(var_exp2);}
    catch (ScriptException e1)
    {
      SamEnchantments.log("Expression: '" + this.min_distance_exp + "' under key '" + config_key + "' did not parse correctly");
      this.min_distance_exp = "5 * L";
      return;
    }
    
    SamEnchantments.debugLog("Testing expression: '" + this.recharge_exp + "'");
    String var_exp3 = max_distance_exp.replace("b", ""+1).replace("l", ""+1);
    try{eng.eval(var_exp3);}
    catch (ScriptException e1)
    {
      SamEnchantments.log("Expression: '" + this.recharge_exp + "' under key '" + config_key + "' did not parse correctly");
      this.recharge_exp = "5 * L";
      return;
    }
  }

  @Override
  public void onPlayerInteract(PlayerInteractEvent event, LoreEnchantment ench, String[] data)
  {
    if(!event.getAction().equals(Action.RIGHT_CLICK_AIR)) return;
    
    if(data.length < 1) return;
    
    // Checking if the item is still recharging:
    if(this.isRecharging(event.getItem()))
    {
      // Play a sound to indicate it was a failure.
      SamEnchantments.debugLog("Enchantment Shadow Step canceled as item was still recharging");
      event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ANVIL_LAND, .1F, .5F);
      return;
    }
    
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
    double max_distance = 0;
    try
    {
      String var_exp = this.max_distance_exp.replace("l", ""+ench_level);
      
      Object ret = eng.eval(var_exp);
      
      double value = 0; 
      if(ret instanceof Integer) value = ((Integer)ret).intValue();
      else if(ret instanceof Double) value = ((Double)ret).doubleValue();
      max_distance = value;
    }
    catch (ScriptException e){max_distance = 0;}
    
    // Getting the min distance
    double min_distance = 0;
    try
    {
      String var_exp = this.min_distance_exp.replace("l", ""+ench_level);
      
      Object ret = eng.eval(var_exp);
      
      double value = 0; 
      if(ret instanceof Integer) value = ((Integer)ret).intValue();
      else if(ret instanceof Double) value = ((Double)ret).doubleValue();
      min_distance = value;
    }
    catch (ScriptException e){min_distance = 0;}
    if(min_distance >= max_distance) return;
    
    // Getting recharge time 
    double recharge_time = 0;
    try
    {
      String var_exp = this.recharge_exp.replace("l", ""+ench_level);
      
      Object ret = eng.eval(var_exp);
      
      double value = 0; 
      if(ret instanceof Integer) value = ((Integer)ret).intValue();
      else if(ret instanceof Double) value = ((Double)ret).doubleValue();
      recharge_time = value;
    }
    catch (ScriptException e){recharge_time = 0;}
    
    Player player = event.getPlayer();
    LivingEntity entity = EntityUtil.getLookedAtEntity(player, ench_level * max_distance, 1);
    if(entity == null) return;
    
    Vector e_dir = entity.getLocation().getDirection().multiply(-behind_distance);
    double step_x = entity.getLocation().getX() + e_dir.getX();
    double step_y = entity.getLocation().getY();
    double step_z = entity.getLocation().getZ() + e_dir.getZ();
    Location step_loc = new Location(entity.getWorld(), step_x, step_y, step_z);
    step_loc.setDirection(entity.getLocation().getDirection());
    if(!step_loc.getBlock().getType().equals(Material.AIR)) return; // No teleport
    player.getWorld().playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1.0F, 1.0F);
    EffectUtil.displayDustCylinderCloud(player.getEyeLocation(), 0, 0, 0, 100, 1, 2);
    player.teleport(step_loc);
    player.getWorld().playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1.0F, 1.0F);
    EffectUtil.displayDustCylinderCloud(player.getEyeLocation(), 0, 0, 0, 100, 1, 2);
    
    // Setting up recharge time.
    this.setRecharging(event.getItem(), (int)Math.floor(recharge_time * 20));
  }

  @Override
  public void onPlayerInteractEntity(PlayerInteractEntityEvent event,
      LoreEnchantment ench, String[] data)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Set<RechargeLater> getAllRechargingTasks(){return this.recharge_tasks;}

  @Override
  public Set<ItemStack> getAllRechargingItems(){return this.recharging_items;}

  @Override
  public JavaPlugin getPlugin(){return this.plugin;}

  @Override
  public void onItemRecharge(Player player)
  {
    player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0F, 2);
  }

}
