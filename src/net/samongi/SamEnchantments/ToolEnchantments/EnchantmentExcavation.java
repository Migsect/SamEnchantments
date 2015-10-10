package net.samongi.SamEnchantments.ToolEnchantments;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnBlockBreak;

public class EnchantmentExcavation extends LoreEnchantment implements OnBlockBreak
{
  private int max_level;
  private Map<Material, Map<Material, String[]>> break_materials = new HashMap<>();
  
  protected EnchantmentExcavation(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin);
    this.max_level = plugin.getConfig().getInt("enchantments." + config_key + ".max-level", 10);
    
    ConfigurationSection breakables = plugin.getConfig().getConfigurationSection("enchantments." + config_key + ".break-materials");
    if(breakables != null)
    {
      Set<String> keys = breakables.getKeys(false);
      for(String k : keys)
      {
        Material mat = Material.getMaterial(k);
        if(mat == null) continue;
        
      }
    }
  }

  @Override
  public void onBlockBreak(BlockBreakEvent event, LoreEnchantment ench, String[] data)
  {
    // TODO Auto-generated method stub
    
  }

}
