package net.samongi.SamEnchantments.Abilities;

import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnPlayerInteract;

public class EnchantmentExplosive extends LoreEnchantment implements OnPlayerInteract
{

  protected EnchantmentExplosive(String name, JavaPlugin owning_plugin)
  {
    super(name, owning_plugin);
    // TODO Auto-generated constructor stub
  }

  @Override
  public void onPlayerInteract(PlayerInteractEvent event, LoreEnchantment ench,
      String[] data)
  {
    // TODO Auto-generated method stub
    
  }

}
