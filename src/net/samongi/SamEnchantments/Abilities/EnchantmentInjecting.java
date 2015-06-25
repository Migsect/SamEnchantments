package net.samongi.SamEnchantments.Abilities;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnPlayerInteract;
import net.samongi.LoreEnchantments.Util.StringUtil;
import net.samongi.LoreEnchantments.Util.ActionUtil.ActionType;
import net.samongi.SamEnchantments.SamEnchantments;

public class EnchantmentInjecting extends LoreEnchantment implements OnPlayerInteract
{
  
  private ActionType action_type;
  
  private String inject_sound;
  
  public EnchantmentInjecting(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin);
    
    String action_type_str = plugin.getConfig().getString("enchantments."+config_key+".action-type");
    if(action_type_str == null) this.action_type = null;
    else this.action_type = ActionType.getByString(action_type_str);
    
    this.inject_sound = plugin.getConfig().getString("enchantments."+config_key+".sound.inject","PISTON_EXTEND");
    
    
  }

  @Override
  public void onPlayerInteract(PlayerInteractEvent event, LoreEnchantment ench, String[] data)
  {
    ActionType action = null;
    if(data.length > 3) action = ActionType.getByString(data[data.length - 1]);
    if(action == null) action = this.action_type;
    if(action == null) return;
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found action to be: " + action);
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found event-action to be: " + ActionType.getActionType(event));
    if(!action.isSimilar(ActionType.getActionType(event))) return;
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found actions to be similar");
    
    // We expect the last to be duration, the second to last being the amplitude
    //   and the left overs to be the potioneffect.
    if(data.length < 3) return;
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
    for(int i = 0; i < data.length - strength_loc; i++)
    {
      effect += data[i] + " ";
    }
    effect = effect.trim();
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found effect to be: " + effect);
    
    PotionEffectType type = translatePotionEffect(effect);
    SamEnchantments.debugLog("Enchantment " + this.getName() + " found effect type to be: " + type);
    if(type == null) return;
    
    Player player = event.getPlayer();
    PotionEffect p_effect = new PotionEffect(type, time * 20, strength-1, true, true);
    player.addPotionEffect(p_effect);
    
    Sound inject_sound = Sound.valueOf(this.inject_sound);
    if(inject_sound != null) player.getWorld().playSound(player.getLocation(), inject_sound, 1.0F, 1.0F);
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
