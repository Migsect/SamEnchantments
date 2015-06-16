package net.samongi.SamEnchantments.ItemEnchantments;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnPlayerDropItem;

import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class EnchantmentUndroppable extends LoreEnchantment implements OnPlayerDropItem
{
  public EnchantmentUndroppable(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin);
  }

  @Override
  public void onPlayerDropItem(PlayerDropItemEvent event, LoreEnchantment ench, String[] data)
  {
    event.setCancelled(true);
  }
}
