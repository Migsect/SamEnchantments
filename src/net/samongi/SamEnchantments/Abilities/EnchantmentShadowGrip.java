package net.samongi.SamEnchantments.Abilities;

import java.util.HashSet;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnPlayerInteract;
import net.samongi.LoreEnchantments.Util.EntityUtil;
import net.samongi.LoreEnchantments.Util.Recharging;
import net.samongi.LoreEnchantments.Util.StringUtil;
import net.samongi.LoreEnchantments.Util.ActionUtil.ActionType;
import net.samongi.SamEnchantments.SamEnchantments;
import net.samongi.SamongiLib.Effects.EffectUtil;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class EnchantmentShadowGrip extends LoreEnchantment implements OnPlayerInteract, Recharging
{
private int max_level;
  
  private String max_distance_exp;
  private String min_distance_exp;
  private double front_distance;
  
  private String recharge_exp;
  private boolean show_on_durability;
  
  private ActionType action_type;
  
  private String teleport_sound;
  private String cooldown_complete_sound;
  private String cooldown_pending_sound;
  
  private Set<ItemStack> recharging_items = new HashSet<>();
  private Set<RechargeLater> recharge_tasks = new HashSet<>();
  
  public EnchantmentShadowGrip(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin);
    
    this.max_level = plugin.getConfig().getInt("enchantments." + config_key + ".max-level", 10);
    
    this.max_distance_exp = plugin.getConfig().getString("enchantments."+config_key+".max-dist-exp","5 * L");
    this.max_distance_exp = this.max_distance_exp.toLowerCase().replace("pow", "Math.pow");
    this.min_distance_exp = plugin.getConfig().getString("enchantments."+config_key+".min-dist-exp","5");
    this.min_distance_exp = this.min_distance_exp.toLowerCase().replace("pow", "Math.pow");
    this.front_distance = plugin.getConfig().getDouble("enchantments." + config_key + ".front-distance", 1);
    
    this.recharge_exp = plugin.getConfig().getString("enchantments."+config_key+".recharge-exp","2.5 * L + 2.5");
    this.recharge_exp = this.recharge_exp.toLowerCase().replace("pow", "Math.pow");
    this.show_on_durability = plugin.getConfig().getBoolean("enchantments."+config_key+".recharge-durability", false);
    
    this.action_type = ActionType.valueOf(plugin.getConfig().getString("enchantments."+config_key+".action-type","RIGHT_CLICK_AIR"));
    
    this.teleport_sound = plugin.getConfig().getString("enchantments."+config_key+".sound.teleport","ENDERMAN_TELEPORT");
    this.cooldown_complete_sound = plugin.getConfig().getString("enchantments."+config_key+".sound.recharge-complete","LEVEL_UP");
    this.cooldown_pending_sound = plugin.getConfig().getString("enchantments."+config_key+".sound.recahrge-pending","ANVIL_LAND");
    
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
    String var_exp3 = recharge_exp.replace("b", ""+1).replace("l", ""+1);
    try{eng.eval(var_exp3);}
    catch (ScriptException e1)
    {
      SamEnchantments.log("Expression: '" + this.recharge_exp + "' under key '" + config_key + "' did not parse correctly");
      this.recharge_exp = "5 * L";
      return;
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public void onPlayerInteract(PlayerInteractEvent event, LoreEnchantment ench, String[] data)
  {
    if(!ActionType.getActionType(event).equals(action_type)) return;
    
    if(data.length < 1) return;
    // Checking if the item is still recharging:
    if(this.isRecharging(event.getItem()))
    {
      // Play a sound to indicate it was a failure.
      SamEnchantments.debugLog("Enchantment Shadow Step canceled as item was still recharging");
      Sound cooldown_pending_sound = Sound.valueOf(this.cooldown_pending_sound);
      if(cooldown_pending_sound != null) event.getPlayer().playSound(event.getPlayer().getLocation(), cooldown_pending_sound, .1F, .5F);
      else event.getPlayer().playSound(event.getPlayer().getLocation(), this.cooldown_pending_sound, .1F, .5F);
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
    SamEnchantments.debugLog("Enchantment Shadow Grip found max-distance to be " + max_distance);
    
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
    SamEnchantments.debugLog("Enchantment Shadow Grip found min-distance to be " + min_distance);
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
    SamEnchantments.debugLog("Enchantment Shadow Grip found recharge time to be " + recharge_time);
    
    // Start enchantment math
    Player player = event.getPlayer();
    LivingEntity entity = EntityUtil.getLookedAtEntity(player, ench_level * max_distance, 1);
    if(entity == null) 
    { 
      SamEnchantments.debugLog("No target entity found, returning.");
      return;
    }
    
    Vector e_dir = player.getLocation().getDirection();
    double x_y_dist = Math.sqrt(Math.pow(e_dir.getX(), 2) + Math.pow(e_dir.getZ(), 2));
    
    double step_x = player.getLocation().getX() + e_dir.getX() * front_distance / x_y_dist;
    double step_y = player.getLocation().getY();
    double step_z = player.getLocation().getZ() + e_dir.getZ() * front_distance / x_y_dist;
    
    double step_h_x = player.getEyeLocation().getX() + e_dir.getX() * front_distance / x_y_dist;
    double step_h_y = player.getEyeLocation().getY();
    double step_h_z = player.getEyeLocation().getZ() + e_dir.getZ() * front_distance / x_y_dist;
    
    Location step_loc = new Location(player.getWorld(), step_x, step_y, step_z);
    Location step_h_loc = new Location(player.getWorld(), step_h_x, step_h_y, step_h_z);
    step_loc.setDirection(player.getLocation().getDirection().multiply(-1));
    
    // Tests to ensure you can actually go there.
    if(step_h_loc.getBlock().getType().isSolid()) 
    {
      SamEnchantments.debugLog("Teleport to head block is not air, returning.");
      return; // No teleport if head is not there
    }
    
    Sound teleport_sound = Sound.valueOf(this.teleport_sound);
    if(teleport_sound != null) entity.getWorld().playSound(player.getLocation(), teleport_sound, 1.0F, 1.0F);
    
    EffectUtil.displayDustCylinderCloud(entity.getEyeLocation(), 0, 0, 0, 100, 1, 2);
    entity.teleport(step_loc);
    if(teleport_sound != null) entity.getWorld().playSound(player.getLocation(), teleport_sound, 1.0F, 1.0F);
    EffectUtil.displayDustCylinderCloud(entity.getEyeLocation(), 0, 0, 0, 100, 1, 2);
    
    // Setting up recharge time.
    this.setRecharging(event.getItem(), (int)Math.floor(recharge_time * 20), this.show_on_durability, 20);
  }

  @Override
  public Set<RechargeLater> getAllRechargingTasks(){return this.recharge_tasks;}

  @Override
  public Set<ItemStack> getAllRechargingItems(){return this.recharging_items;}

  @Override
  public JavaPlugin getPlugin(){return this.getOwningPlugin();}

  @SuppressWarnings("deprecation")
  @Override
  public void onItemRecharge(Player player)
  {
    Sound cooldown_complete_sound = Sound.valueOf(this.cooldown_complete_sound);
    if(cooldown_complete_sound != null) player.playSound(player.getLocation(), cooldown_complete_sound, .1F, .5F);
    else player.playSound(player.getLocation(), this.cooldown_complete_sound, .1F, .5F);
    player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0F, 2);
  }

}
