package net.samongi.SamEnchantments.ItemEnchantments;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnItemInventoryClick;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class EnchantmentImmovable extends LoreEnchantment implements OnItemInventoryClick
{
  public EnchantmentImmovable(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin);
  }

  @Override
  public void onItemInventoryClick(InventoryClickEvent event, LoreEnchantment ench, String[] data)
  {
    event.setCancelled(true);
  }
}
