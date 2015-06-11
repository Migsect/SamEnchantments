package net.samongi.SamEnchantments.Abilities;

import java.util.HashSet;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnPlayerInteract;
import net.samongi.LoreEnchantments.Util.Recharging;
import net.samongi.LoreEnchantments.Util.StringUtil;
import net.samongi.LoreEnchantments.Util.ActionUtil.ActionType;
import net.samongi.SamEnchantments.SamEnchantments;

public class EnchantmentExplode extends LoreEnchantment implements OnPlayerInteract, Recharging
{
  private int max_level;
  
  private String power_exp;
  private boolean destroy_blocks;
  private boolean set_fire;
  
  private String recharge_exp;
  private boolean show_on_durability;
  
  private ActionType action_type;
  
  private String cooldown_complete_sound;
  private String cooldown_pending_sound;
  
  private Set<ItemStack> recharging_items = new HashSet<>();
  private Set<RechargeLater> recharge_tasks = new HashSet<>();
  
  public EnchantmentExplode(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin);
    this.max_level = plugin.getConfig().getInt("enchantments." + config_key + ".max-level", 10);
    
    this.power_exp = plugin.getConfig().getString("enchantments."+config_key+".power-exp","4 * L");
    this.power_exp = this.power_exp.toLowerCase().replace("pow", "Math.pow");
    this.destroy_blocks = plugin.getConfig().getBoolean("enchantments."+config_key+".destroy-blocks", false);
    this.set_fire = plugin.getConfig().getBoolean("enchantments."+config_key+".set-fire", false);
    
    this.recharge_exp = plugin.getConfig().getString("enchantments."+config_key+".recharge-exp","2.5 * L + 10");
    this.recharge_exp = this.recharge_exp.toLowerCase().replace("pow", "Math.pow");
    this.show_on_durability = plugin.getConfig().getBoolean("enchantments."+config_key+".recharge-durability", false);
    
    this.action_type = ActionType.valueOf(plugin.getConfig().getString("enchantments."+config_key+".action-type","RIGHT_CLICK_AIR"));
    
    this.cooldown_complete_sound = plugin.getConfig().getString("enchantments."+config_key+".sound.recharge-complete","LEVEL_UP");
    this.cooldown_pending_sound = plugin.getConfig().getString("enchantments."+config_key+".sound.recahrge-pending","ANVIL_LAND");
    
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
  public void onPlayerInteract(PlayerInteractEvent event, LoreEnchantment ench,
      String[] data)
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
    SamEnchantments.debugLog("Enchantment Explode found explosive power to be " + explosion_power);
    
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
    SamEnchantments.debugLog("Enchantment Explode found recharge time to be " + recharge_time);
    
    // Do specific enchantment stuff here"
    Player player = event.getPlayer();
    Location loc = player.getLocation();
    player.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), (float) explosion_power, this.set_fire, this.destroy_blocks);
    
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
