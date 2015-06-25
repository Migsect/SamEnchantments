package net.samongi.SamEnchantments.Abilities;

import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnPlayerInteract;
import net.samongi.LoreEnchantments.Util.EntityUtil;
import net.samongi.LoreEnchantments.Util.StringUtil;
import net.samongi.LoreEnchantments.Util.ActionUtil.ActionType;
import net.samongi.SamEnchantments.SamEnchantments;
import net.samongi.SamongiLib.Effects.EffectUtil;

public class EnchantmentSplashing extends LoreEnchantment implements OnPlayerInteract
{
  
  private ActionType action_type;
  private int max_level;
  
  private String inject_sound;
  
  private String radius_exp;
  
  public EnchantmentSplashing(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin);
    this.max_level = plugin.getConfig().getInt("enchantments." + config_key + ".max-level", 10);
    
    String action_type_str = plugin.getConfig().getString("enchantments."+config_key+".action-type");
    if(action_type_str == null) this.action_type = null;
    else this.action_type = ActionType.getByString(action_type_str);
    
    this.inject_sound = plugin.getConfig().getString("enchantments."+config_key+".sound.inject","ZOMBIE_INFECT");
    this.radius_exp = plugin.getConfig().getString("enchantments."+config_key+".radius-exp","5 * L");
    this.radius_exp = this.radius_exp.toLowerCase().replace("pow", "Math.pow");
    
    // Testing the expressions:
    ScriptEngine eng = SamEnchantments.getJavaScriptEngine();
    
    SamEnchantments.debugLog("Testing expression: '" + this.radius_exp + "'");
    String var_exp1 = radius_exp.replace("b", ""+1).replace("l", ""+1);
    try{eng.eval(var_exp1);}
    catch (ScriptException e1)
    {
      SamEnchantments.log("Expression: '" + this.radius_exp + "' under key '" + config_key + "' did not parse correctly");
      this.radius_exp = "5 * L";
      return;
    }
    
  }

  @Override
  public void onPlayerInteract(PlayerInteractEvent event, LoreEnchantment ench, String[] data)
  {
    ActionType action = null;
    if(data.length > 4) action = ActionType.getByString(data[data.length - 1]);
    if(action == null) action = this.action_type;
    if(action == null) return;
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found action to be: " + action);
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found event-action to be: " + ActionType.getActionType(event));
    if(!action.isSimilar(ActionType.getActionType(event))) return;
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found actions to be similar");
    
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
    
    // We expect the last to be duration, the second to last being the amplitude
    //   and the left overs to be the potioneffect.
    if(data.length < 4) return;
    int time = StringUtil.getSeconds(data[data.length - 1]);
    // If the action is defined with the enchantment
    if(time == 0) time = StringUtil.getSeconds(data[data.length - 2]);
    if(time == 0) return;
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found duration to be: " + time);
    int strength = 0;
    int strength_loc = 2;
    try{strength = Integer.parseInt(data[data.length - 2]);}catch(NumberFormatException e){};
    if(strength == 0) strength = StringUtil.numeralToInt(data[data.length - 2]);
    // If the action is defined with the enchantment
    if(strength == 0)
    {
      try{strength = Integer.parseInt(data[data.length - 3]);}catch(NumberFormatException e){};
      strength_loc = 3;
    }
    if(strength == 0) 
    {
      strength = StringUtil.numeralToInt(data[data.length - 3]);
      strength_loc = 3;
    }
    if(strength == 0) return;
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found strength to be: " + strength);
    // Bring back together the heading elements
    String effect = "";
    for(int i = 1; i < data.length - strength_loc; i++)
    {
      effect += data[i] + " ";
    }
    effect = effect.trim();
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found effect to be: " + effect);
    
    PotionEffectType type = translatePotionEffect(effect);
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found effect type to be: " + type);
    if(type == null) return;
    
    ScriptEngine eng = SamEnchantments.getJavaScriptEngine();
    // Getting the max distance
    double radius = 0;
    try
    {
      String var_exp = this.radius_exp.replace("l", ""+ench_level);
      
      Object ret = eng.eval(var_exp);
      
      double value = 0; 
      if(ret instanceof Integer) value = ((Integer)ret).intValue();
      else if(ret instanceof Double) value = ((Double)ret).doubleValue();
      radius = value;
    }
    catch (ScriptException e){radius = 0;}
    SamEnchantments.debugLog("Enchantment Explode found explosive power to be " + radius);
    
    
    Player player = event.getPlayer();
    PotionEffect p_effect = new PotionEffect(type, time * 20, strength-1, true, true);
    List<LivingEntity> entities = EntityUtil.getNearbyLivingEntities(player, radius);
    for(LivingEntity e : entities) e.addPotionEffect(p_effect);
    
    EffectUtil.displayDustSphereCloud(player.getEyeLocation(), 255, 255, 255, 400 * ench_level * ench_level, radius);
    
    Sound inject_sound = Sound.valueOf(this.inject_sound);
    if(inject_sound != null) player.getWorld().playSound(player.getLocation(), inject_sound, (float) (radius * 1.0F / 15), 1.0F);
  }
  
  private PotionEffectType translatePotionEffect(String str)
  {
    PotionEffectType ret = PotionEffectType.getByName(str.toUpperCase().replace(" ", "_"));
    if(ret != null) return ret; // If we got something, then we can return it.  Otherwise we need to do a full translation.
    switch(str.toLowerCase())
    {
      case "speed": return PotionEffectType.SPEED;
      case "slowness": return PotionEffectType.SLOW;
      case "haste": return PotionEffectType.FAST_DIGGING;
      case "mining fatigue": return PotionEffectType.SLOW_DIGGING;
      case "strength": return PotionEffectType.INCREASE_DAMAGE;
      case "instant health": return PotionEffectType.HEAL;
      case "instant damage": return PotionEffectType.HARM;
      case "jump boost": return PotionEffectType.JUMP;
      case "nausea": return PotionEffectType.CONFUSION;
      case "regeneration": return PotionEffectType.REGENERATION;
      case "resistance": return PotionEffectType.DAMAGE_RESISTANCE;
      case "fire resistance": return PotionEffectType.FIRE_RESISTANCE;
      case "water breathing": return PotionEffectType.WATER_BREATHING;
      case "invisibility": return PotionEffectType.INVISIBILITY;
      case "blindness": return PotionEffectType.BLINDNESS;
      case "night vision": return PotionEffectType.NIGHT_VISION;
      case "hunger": return PotionEffectType.HUNGER;
      case "weakness": return PotionEffectType.WEAKNESS;
      case "poison": return PotionEffectType.POISON;
      case "wither": return PotionEffectType.WITHER;
      case "health boost": return PotionEffectType.HEALTH_BOOST;
      case "absorption": return PotionEffectType.ABSORPTION;
      case "saturation": return PotionEffectType.SATURATION;
    }
    return null;
  }
}
