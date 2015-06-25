package net.samongi.SamEnchantments.ItemEnchantments;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnItemInventoryClick;

public class EnchantmentCommand extends LoreEnchantment implements OnItemInventoryClick
{

  public EnchantmentCommand(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin);
  }

  @Override
  public void onItemInventoryClick(InventoryClickEvent event, LoreEnchantment ench, String[] data)
  {
    if(event.isShiftClick()) return;
    event.setCancelled(true);
    String command  = "";
    for(String d : data) command += d + " ";
    command = command.trim().replace("/", "").replace("\"", "").replace("'", "");
    Player player = (Player)event.getWhoClicked();
    player.performCommand(command);
    player.updateInventory();
      
  }

}
