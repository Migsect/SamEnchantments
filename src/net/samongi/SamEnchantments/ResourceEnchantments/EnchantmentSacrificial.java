package net.samongi.SamEnchantments.ResourceEnchantments;

import java.util.Random;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnItemDamage;
import net.samongi.LoreEnchantments.Util.StringUtil;
import net.samongi.SamEnchantments.SamEnchantments;
public class EnchantmentSacrificial extends LoreEnchantment implements OnItemDamage
{
  
  private int max_level;
  private int min_health;
  
  protected EnchantmentSacrificial(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin);
    this.max_level = plugin.getConfig().getInt("enchantments." + config_key + ".max-level", 10);
    this.min_health = plugin.getConfig().getInt("enchantments." + config_key + ".min-health", 0);
    
  }

  @Override
  public void onItemDamage(PlayerItemDamageEvent event, LoreEnchantment ench, String[] data)
  {
    //ItemStack item = event.getItem();
    int dur_dam = event.getDamage();
    
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
    
    Random rand = new Random();
    // This will be the possibility to deal 1 heart of damage for the durability.
    double heart_chance = ench_level / this.max_level;
    for(int i = 0 ; i < dur_dam ; i++)
    {
      if(rand.nextDouble() <= heart_chance)
      {
        Player player = event.getPlayer();
        // This is so people can put safety caps onto the health being dealt
        if(player.getHealth() <= this.min_health) return;
        // This is setting the health of the player
        player.setHealth(player.getHealth() - 1);
        // This is setting the damage of the event because if there is more than 1 damage
        // being done, then more damage can go through.
        event.setDamage(event.getDamage() - 1);
        // We are formally canceling the event at this stage.
        if(event.getDamage() <= 0)
        {
          event.setCancelled(true);
          return;
        }
      }
    }
  }

}
