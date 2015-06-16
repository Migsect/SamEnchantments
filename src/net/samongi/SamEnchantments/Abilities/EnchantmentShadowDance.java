package net.samongi.SamEnchantments.Abilities;

import java.util.List;
import java.util.Random;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnPlayerInteract;
import net.samongi.LoreEnchantments.Util.EntityUtil;
import net.samongi.LoreEnchantments.Util.StringUtil;
import net.samongi.LoreEnchantments.Util.ActionUtil.ActionType;
import net.samongi.SamEnchantments.SamEnchantments;
import net.samongi.SamongiLib.Effects.EffectUtil;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class EnchantmentShadowDance extends LoreEnchantment implements OnPlayerInteract
{
  private int max_level;
  
  private String max_distance_exp;
  private String min_distance_exp;
  private double behind_distance;
  
  private ActionType action_type;
  
  private String teleport_sound;
  
  public EnchantmentShadowDance(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin);
    
    this.max_level = plugin.getConfig().getInt("enchantments." + config_key + ".max-level", 10);
    
    this.max_distance_exp = plugin.getConfig().getString("enchantments."+config_key+".max-dist-exp","5 * L");
    this.max_distance_exp = this.max_distance_exp.toLowerCase().replace("pow", "Math.pow");
    this.min_distance_exp = plugin.getConfig().getString("enchantments."+config_key+".min-dist-exp","5");
    this.min_distance_exp = this.min_distance_exp.toLowerCase().replace("pow", "Math.pow");
    this.behind_distance = plugin.getConfig().getDouble("enchantments." + config_key + ".behind-distance", 1);
    
    this.action_type = ActionType.valueOf(plugin.getConfig().getString("enchantments."+config_key+".action-type","RIGHT_CLICK_AIR"));
    
    this.teleport_sound = plugin.getConfig().getString("enchantments."+config_key+".sound.teleport","ENDERMAN_TELEPORT");
    
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
    
  }
  
  @Override
  public void onPlayerInteract(PlayerInteractEvent event, LoreEnchantment ench, String[] data)
  {
    if(!this.action_type.isSimilar(ActionType.getActionType(event))) return;
    
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
    SamEnchantments.debugLog("Enchantment Shadow Dance found max-distance to be " + max_distance);
    
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
    SamEnchantments.debugLog("Enchantment Shadow Dance found min-distance to be " + min_distance);
    if(min_distance >= max_distance) return;
    
    // Start enchantment math
    Player player = event.getPlayer();
    List<LivingEntity> entities = EntityUtil.getNearbyLivingEntities(player, min_distance, max_distance);
    if(entities.size() == 0) 
    { 
      SamEnchantments.debugLog("No target entities found, returning.");
      return;
    }
    LivingEntity entity = entities.get((new Random()).nextInt(entities.size()));
    Vector e_dir = entity.getLocation().getDirection().multiply(-1);
    double x_y_dist = Math.sqrt(Math.pow(e_dir.getX(), 2) + Math.pow(e_dir.getZ(), 2));
    
    double step_x = entity.getLocation().getX() + e_dir.getX() * behind_distance / x_y_dist;
    double step_y = entity.getLocation().getY();
    double step_z = entity.getLocation().getZ() + e_dir.getZ() * behind_distance / x_y_dist;
    
    double step_h_x = entity.getEyeLocation().getX() + e_dir.getX() * behind_distance / x_y_dist;
    double step_h_y = entity.getEyeLocation().getY();
    double step_h_z = entity.getEyeLocation().getZ() + e_dir.getZ() * behind_distance / x_y_dist;
    
    Location step_loc = new Location(entity.getWorld(), step_x, step_y, step_z);
    Location step_h_loc = new Location(entity.getWorld(), step_h_x, step_h_y, step_h_z);
    step_loc.setDirection(entity.getLocation().getDirection());
    
    // Tests to ensure you can actually go there.
    if(step_h_loc.getBlock().getType().isSolid()) 
    {
      SamEnchantments.debugLog("Teleport to head block is not air, returning.");
      return; // No teleport if head is not there
    }
    
    Sound teleport_sound = Sound.valueOf(this.teleport_sound);
    if(teleport_sound != null) player.getWorld().playSound(player.getLocation(), teleport_sound, 1.0F, 1.0F);
    
    EffectUtil.displayDustCylinderCloud(player.getEyeLocation(), 0, 0, 0, 100, 1, 2);
    player.teleport(step_loc);
    if(teleport_sound != null) player.getWorld().playSound(player.getLocation(), teleport_sound, 1.0F, 1.0F);
    EffectUtil.displayDustCylinderCloud(player.getEyeLocation(), 0, 0, 0, 100, 1, 2);
    
  }

}
