package net.samongi.SamEnchantments.ItemEnchantments;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnPlayerItemConsume;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class EnchantmentBottomless extends LoreEnchantment implements OnPlayerItemConsume
{
  public EnchantmentBottomless(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin);
  }

  @Override
  public void onPlayerItemConsume(PlayerItemConsumeEvent event, LoreEnchantment ench, String[] data)
  {
    ItemStack item = event.getItem().clone();
    Player player = event.getPlayer();
    int slot = player.getInventory().getHeldItemSlot();
    BukkitRunnable task = new BukkitRunnable()
    {  
      @Override
      public void run()
      {
        player.getInventory().setHeldItemSlot(slot);
        player.setItemInHand(item);
        player.updateInventory();
      }
    };
    task.runTaskLater(this.getOwningPlugin(), 1);
  }

}
