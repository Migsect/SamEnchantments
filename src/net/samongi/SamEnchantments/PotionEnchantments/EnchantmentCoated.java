package net.samongi.SamEnchantments.PotionEnchantments;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnEntityDamageEntity;
import net.samongi.LoreEnchantments.Util.StringUtil;
import net.samongi.SamEnchantments.SamEnchantments;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EnchantmentCoated extends LoreEnchantment implements OnEntityDamageEntity
{
  @SuppressWarnings("unused")
  private JavaPlugin plugin;
  
  public EnchantmentCoated(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin);
    this.plugin = plugin;
  }

  @Override
  public void onEntityDamageEntity(EntityDamageByEntityEvent event, LoreEnchantment ench, String[] data)
  {
    // We expect the last to be duration, the second to last being the amplitude
    //   and the left overs to be the potioneffect.
    if(data.length < 3) return;
    int time = StringUtil.getSeconds(data[data.length - 1]);
    if(time == 0) return;
    SamEnchantments.debugLog("Enchantment Coated found duration to be: " + time);
    int strength = 0;
    try{strength = Integer.parseInt(data[data.length - 2]);}catch(NumberFormatException e){};
    if(strength == 0) strength = StringUtil.numeralToInt(data[data.length - 2]);
    if(strength == 0) return;
    SamEnchantments.debugLog("Enchantment Coated found strength to be: " + strength);
    // Bring back together the heading elements
    String effect = "";
    for(int i = 0; i < data.length - 2; i++)
    {
      effect += data[i] + " ";
    }
    effect = effect.trim();
    SamEnchantments.debugLog("Enchantment Coated found effect to be: " + effect);
    
    PotionEffectType type = translatePotionEffect(effect);
    SamEnchantments.debugLog("Enchantment Coated found effect type to be: " + type);
    if(type == null) return;
    
    if(!(event.getEntity() instanceof LivingEntity)) return;
    LivingEntity entity = (LivingEntity)event.getEntity();
    PotionEffect p_effect = new PotionEffect(type, time * 20, strength-1, true, true);
    entity.addPotionEffect(p_effect);
    
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
