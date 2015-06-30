package net.samongi.SamEnchantments.ResourceEnchantments;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnBlockBreak;
import net.samongi.LoreEnchantments.Interfaces.OnEntityDamageEntity;
import net.samongi.LoreEnchantments.Interfaces.OnEntityShootBow;
import net.samongi.LoreEnchantments.Interfaces.OnPlayerInteract;
import net.samongi.LoreEnchantments.Interfaces.OnPlayerItemConsume;
import net.samongi.LoreEnchantments.Util.ActionUtil.ActionType;
import net.samongi.LoreEnchantments.Util.EntityUtil;
import net.samongi.LoreEnchantments.Util.StringUtil;
import net.samongi.SamEnchantments.SamEnchantments;
import net.samongi.SamongiLib.Items.Wrappers.ComparableItemStack;
import net.samongi.SamongiLib.Items.Wrappers.ReferenceItemStack;
import net.samongi.SamongiLib.Items.Wrappers.ComparableItemStack.ItemComparison;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class EnchantmentRecharging extends LoreEnchantment implements OnPlayerInteract, OnEntityShootBow, OnEntityDamageEntity, OnPlayerItemConsume, OnBlockBreak
{
  private String cooldown_complete_sound;
  private String cooldown_pending_sound;
  
  private boolean do_amount_show = false;
  private boolean do_durability_show = false;
  
  private int visual_tick = 20;
  
  // Player -> ActionType -> ItemStack -> Recharge
  private Map<String, Map<String, Map<ComparableItemStack, Recharge>>> recharging_tasks = new HashMap<>();
  
  public EnchantmentRecharging(JavaPlugin plugin, String name, String config_key)
  {
    super(name, plugin);

    this.cooldown_complete_sound = plugin.getConfig().getString("enchantments."+config_key+".sound.recharge-complete","LEVEL_UP");
    this.cooldown_pending_sound = plugin.getConfig().getString("enchantments."+config_key+".sound.recahrge-pending","ANVIL_LAND");
    
    this.do_amount_show = plugin.getConfig().getBoolean("enchantments."+config_key+".show.amount", false);
    this.do_durability_show = plugin.getConfig().getBoolean("enchantments."+config_key+".show.durability", false);
    this.visual_tick = plugin.getConfig().getInt("enchantments."+config_key+".show.tick", 20);
    
  }
  
  private void setItemRecharging(Player player, String action, ItemStack item, Recharge recharge)
  {
    Set<ItemComparison> comparisons = new HashSet<>();
    comparisons.add(ItemComparison.AMOUNT);
    comparisons.add(ItemComparison.DURABILITY);
    ComparableItemStack ref_item = new ComparableItemStack(item, comparisons); // will be excluding the comparitors
    // First create all the little stuff.
    if(!recharging_tasks.containsKey(player.getName())) recharging_tasks.put(player.getName(), new HashMap<String, Map<ComparableItemStack, Recharge>>());
    if(!recharging_tasks.get(player.getName()).containsKey(action)) recharging_tasks.get(player.getName()).put(action, new HashMap<ComparableItemStack, Recharge>());
    // add the item.  Should exist because we checked it.
    recharging_tasks.get(player.getName()).get(action).put(ref_item, recharge);
  }
  private boolean isRecharging(Player player, String action, ItemStack item)
  {
    Set<ItemComparison> comparisons = new HashSet<>();
    comparisons.add(ItemComparison.AMOUNT);
    comparisons.add(ItemComparison.DURABILITY);
    ComparableItemStack ref_item = new ComparableItemStack(item, comparisons); // will be excluding the comparitors
    if(!recharging_tasks.containsKey(player.getName())) return false;
    if(!recharging_tasks.get(player.getName()).containsKey(action)) return false;
    if(!recharging_tasks.get(player.getName()).get(action).keySet().contains(ref_item)) return false;
    return true;
  }
  private void removeItemRecharging(Player player, String action, ItemStack item)
  {
    Set<ItemComparison> comparisons = new HashSet<>();
    comparisons.add(ItemComparison.AMOUNT);
    comparisons.add(ItemComparison.DURABILITY);
    ComparableItemStack ref_item = new ComparableItemStack(item, comparisons); // will be excluding the comparitors
    // Checking to see if it really is recharging
    if(!this.isRecharging(player, action, item)) return;
    recharging_tasks.get(player.getName()).get(action).remove(ref_item);
    // Clean up the action -> ItemStacks portion if it doesn't have anything.
    if(recharging_tasks.get(player.getName()).get(action).size() == 0) recharging_tasks.get(player.getName()).remove(action);
    // Clean up the Player -> action portion if it doesn't have anything.
    if(recharging_tasks.get(player.getName()).size() == 0) recharging_tasks.remove(player);
    // This should clean up any hanging objects (no memory leaks here!)
  }
  private void printRecharging()
  {
    SamEnchantments.debugLog("Printing out the currently pending Enchantments:");
    for(String player : recharging_tasks.keySet())
    {
      SamEnchantments.debugLog("  '" + player + "':");
      for(String action : recharging_tasks.get(player).keySet())
      {
        SamEnchantments.debugLog("    '" + action + "':");
        for(ComparableItemStack item : recharging_tasks.get(player).get(action).keySet())
        {
          SamEnchantments.debugLog("      '" + item.getItem().getType() + ":" + item.getItem().getDurability() + ":" + item.getItem().getAmount() + "': " + item.hashCode());
        }
      }
    }
  }
  
  public class Recharge extends BukkitRunnable
  {
    private final String player_name;
    private final String action;
    private final ReferenceItemStack recharging_item;
    
    private final EnchantmentRecharging source;
    private RechargeUpdater updater;
    
    private final int start_ticks; // The ticks that the bukkit started at.
    private final int total_ticks; // The ticks that the recharge will last for.
    
    private boolean visual_durability_recharge = false;
    private boolean visual_amount_recharge = false;
    private int visual_tick = 1;
    
    public Recharge(Player player, String action, ItemStack item, EnchantmentRecharging source, int tick_time)
    {
      this.player_name = player.getName();
      this.action = action;
      this.recharging_item = new ReferenceItemStack(item);
      this.source = source;
      this.total_ticks = tick_time;
      this.start_ticks = (int) (System.currentTimeMillis() / 50);
    }
    
    public final void setVisualDurability(boolean bool){this.visual_durability_recharge = bool;}
    public final void setVisualAmount(boolean bool){this.visual_amount_recharge = bool;}
    public final void setVisualTick(int tick){this.visual_tick = tick;}
    
    public final boolean isVisualDurability(){return this.visual_durability_recharge;}
    public final boolean isVisualAmount(){return this.visual_amount_recharge;}
    
    public final String getPlayerName(){return this.player_name;}
    public final Player getPlayer(){return Bukkit.getPlayer(this.player_name);}
    
    public final ItemStack getItemStack(){return this.recharging_item.getItem();}
    public final ReferenceItemStack getReferenceItemStack(){return this.recharging_item;}
    public final boolean isItemStack(ItemStack item){return (new ReferenceItemStack(item)).equals(this.recharging_item);}
    
    public final int getTotalTicks(){return this.total_ticks;}
    public final int getRemainingTicks(){return (int) (this.total_ticks + this.start_ticks - (System.currentTimeMillis() / 50));}
    public final boolean isRecharging(){return this.getRemainingTicks() > 0;}
    
    public void start()
    {
      this.runTaskLater(source.getOwningPlugin(), this.total_ticks);
      this.startUpdater();
    }
    private void startUpdater()
    {
      if(!this.isVisualAmount() && !this.isVisualDurability()) return;
      SamEnchantments.debugLog("Starting Recharge Updater: visual_tick: " + this.visual_tick + ", do_durability: " + this.visual_durability_recharge + ", do_amount: " + this.visual_amount_recharge);
      this.updater = new RechargeUpdater(this);
      this.updater.runTaskTimer(this.source.getOwningPlugin(), 0, this.visual_tick);
    }
    public void finish()
    {
      this.run();
      this.cancel();
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public void run()
    { 
      // Cleaning the thing from records.
      this.source.removeItemRecharging(this.getPlayer(), action, recharging_item.getItem());
      
      // Stopping the Recharge Updater
      if(this.updater != null) this.updater.complete();
      
      // Play the sound.
      Sound cooldown_complete_sound = Sound.valueOf(source.cooldown_complete_sound);
      if(cooldown_complete_sound != null) this.getPlayer().playSound(this.getPlayer().getLocation(), cooldown_complete_sound, .1F, .5F);
      else this.getPlayer().playSound(this.getPlayer().getLocation(), source.cooldown_complete_sound, .1F, .5F);
    }
     
  }
  /**Will update the items when it runs.
   */
  public class RechargeUpdater extends BukkitRunnable
  {
    private final Recharge recharge;
    public RechargeUpdater(Recharge recharge)
    {
      this.recharge = recharge;
    }
    @Override
    public void run()
    {
      if(recharge.isVisualDurability()) updateDurability();
      if(recharge.isVisualAmount()) updateAmount();
    }
    private final void updateDurability()
    {
      ItemStack item = recharge.getItemStack();
      double filled_ratio = this.recharge.getRemainingTicks() / (double)this.recharge.getTotalTicks();
      
      short max_durability = item.getType().getMaxDurability();
      short new_durability = (short) (max_durability * filled_ratio);
      
      if(new_durability >= max_durability - 1) new_durability = (short) (max_durability - 2);
      item.setDurability(new_durability);
    }
    private final void updateAmount()
    {
      ItemStack item = recharge.getItemStack();
      double filled_ratio = this.recharge.getRemainingTicks() / (double)this.recharge.getTotalTicks();
      
      // int max_amount = item.getMaxStackSize();
      int max_amount = 100;
      int new_amount = (int) (max_amount * filled_ratio);
      
      if(new_amount <= 1) new_amount = 1;
      item.setAmount(new_amount);
    }
    public final void complete()
    {
      if(recharge.isVisualDurability()) completeDurability();
      if(recharge.isVisualAmount()) completeAmount();
      this.cancel();
    }
    private final void completeDurability(){this.recharge.getItemStack().setDurability((short) 0);;}
    private final void completeAmount(){this.recharge.getItemStack().setAmount(1);}
    
  }
  
  private void route(Player player, String action, ActionType action_type, ItemStack item, Cancellable event, String[] data, int ticks)
  {
    /* Entity Clicks
     * 
     */
    if(action.startsWith("right click entity"))
    {
      if(!action_type.equals(ActionType.RIGHT_CLICK) &&
          !action_type.equals(ActionType.RIGHT_CLICK_AIR) &&
          !action_type.equals(ActionType.RIGHT_CLICK_BLOCK)) return;
      if(data.length != 5) return;
      if(!this.checkIfClickedEntity(player, action, data)) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    if(action.startsWith("shift right click entity"))
    {
      if(!action_type.equals(ActionType.SHIFT_RIGHT_CLICK) &&
          !action_type.equals(ActionType.SHIFT_RIGHT_CLICK_AIR) &&
          !action_type.equals(ActionType.SHIFT_RIGHT_CLICK_BLOCK)) return;
      if(data.length != 6) return;
      if(!this.checkIfClickedEntity(player, action, data)) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    if(action.startsWith("left click entity"))
    {
      if(!action_type.equals(ActionType.LEFT_CLICK) &&
          !action_type.equals(ActionType.LEFT_CLICK_AIR) &&
          !action_type.equals(ActionType.LEFT_CLICK_BLOCK)) return;
      if(data.length != 5) return;
      if(!this.checkIfClickedEntity(player, action, data)) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    if(action.startsWith("shift left click entity"))
    {
      if(!action_type.equals(ActionType.SHIFT_LEFT_CLICK) &&
          !action_type.equals(ActionType.SHIFT_LEFT_CLICK_AIR) &&
          !action_type.equals(ActionType.SHIFT_LEFT_CLICK_BLOCK)) return;
      if(data.length != 6) return;
      if(!this.checkIfClickedEntity(player, action, data)) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    /* Air Clicks
     * 
     */
    if(action.startsWith("right click air"))
    {
      if(!action_type.equals(ActionType.RIGHT_CLICK_AIR)) return;
      if(data.length != 4) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    if(action.startsWith("shift right click air"))
    {
      if(!action_type.equals(ActionType.SHIFT_RIGHT_CLICK_AIR)) return;
      if(data.length != 5) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    if(action.startsWith("left click air"))
    {
      if(!action_type.equals(ActionType.LEFT_CLICK_AIR)) return;
      if(data.length != 4) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    if(action.startsWith("shift left click air"))
    {
      if(!action_type.equals(ActionType.SHIFT_LEFT_CLICK_AIR)) return;
      if(data.length != 5) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    /* Block Clicks
     * 
     */
    if(action.startsWith("right click block"))
    {
      if(!action_type.equals(ActionType.RIGHT_CLICK_BLOCK)) return;
      if(data.length != 4) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    if(action.startsWith("shift right click block"))
    {
      if(!action_type.equals(ActionType.SHIFT_RIGHT_CLICK_BLOCK)) return;
      if(data.length != 5) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    if(action.startsWith("left click block"))
    {
      if(!action_type.equals(ActionType.LEFT_CLICK_BLOCK)) return;
      if(data.length != 4) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    if(action.startsWith("shift left click block"))
    {
      if(!action_type.equals(ActionType.SHIFT_LEFT_CLICK_BLOCK)) return;
      if(data.length != 5) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    /* Basic Clicks
     * 
     */
    if(action.startsWith("right click"))
    {
      if(!action_type.equals(ActionType.RIGHT_CLICK) &&
          !action_type.equals(ActionType.RIGHT_CLICK_AIR) &&
          !action_type.equals(ActionType.RIGHT_CLICK_BLOCK)) return;
      if(data.length != 3) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    if(action.startsWith("shift right click"))
    {
      if(!action_type.equals(ActionType.SHIFT_RIGHT_CLICK) &&
          !action_type.equals(ActionType.SHIFT_RIGHT_CLICK_AIR) &&
          !action_type.equals(ActionType.SHIFT_RIGHT_CLICK_BLOCK)) return;
      if(data.length != 4) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    if(action.startsWith("left click"))
    {
      if(!action_type.equals(ActionType.LEFT_CLICK) &&
          !action_type.equals(ActionType.LEFT_CLICK_AIR) &&
          !action_type.equals(ActionType.LEFT_CLICK_BLOCK)) return;
      if(data.length != 3) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    if(action.startsWith("shift left click"))
    {
      if(!action_type.equals(ActionType.SHIFT_LEFT_CLICK) &&
          !action_type.equals(ActionType.SHIFT_LEFT_CLICK_AIR) &&
          !action_type.equals(ActionType.SHIFT_LEFT_CLICK_BLOCK)) return;
      if(data.length != 4) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    /* Consume Actions
     * 
     */
    if(action.startsWith("consume all"))
    {
      if(!action_type.equals(ActionType.CONSUME_ALL) &&
          !action_type.equals(ActionType.CONSUME) &&
          !action_type.equals(ActionType.SHIFT_CONSUME)) return;
      if(data.length != 3) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    if(action.startsWith("consume"))
    {
      if(!action_type.equals(ActionType.CONSUME)) return;
      if(data.length != 2) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    if(action.startsWith("shift consume"))
    {
      if(!action_type.equals(ActionType.SHIFT_CONSUME)) return;
      if(data.length != 3) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    /* Attack Actions
     * 
     */

    if(action.startsWith("attack all"))
    {
      if(!action_type.equals(ActionType.ATTACK_ALL) &&
          !action_type.equals(ActionType.ATTACK) &&
          !action_type.equals(ActionType.SHIFT_ATTACK)) return;
      if(data.length != 3) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    if(action.startsWith("attack"))
    {
      if(!action_type.equals(ActionType.ATTACK)) return;
      if(data.length != 2) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    if(action.startsWith("shift attack"))
    {
      if(!action_type.equals(ActionType.SHIFT_ATTACK)) return;
      if(data.length != 3) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    /* Shoot Action
     * 
     */
    if(action.startsWith("shoot bow all"))
    {
      if(!action_type.equals(ActionType.SHOOT_BOW_ALL) &&
          !action_type.equals(ActionType.SHOOT_BOW) &&
          !action_type.equals(ActionType.SHIFT_SHOOT_BOW)) return;
      if(data.length != 4) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    if(action.startsWith("shoot bow"))
    {
      if(!action_type.equals(ActionType.SHOOT_BOW)) return;
      if(data.length != 3) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    if(action.startsWith("shift shoot bow"))
    {
      if(!action_type.equals(ActionType.SHIFT_SHOOT_BOW)) return;
      if(data.length != 4) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    /* Block Break Action
     * 
     */
    if(action.startsWith("block break all"))
    {
      if(!action_type.equals(ActionType.BLOCK_BREAK_ALL) &&
          !action_type.equals(ActionType.BLOCK_BREAK) &&
          !action_type.equals(ActionType.SHIFT_BLOCK_BREAK)) return;
      if(data.length != 4) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    if(action.startsWith("block break"))
    {
      if(!action_type.equals(ActionType.BLOCK_BREAK)) return;
      if(data.length != 3) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
    if(action.startsWith("shift block break"))
    {
      if(!action_type.equals(ActionType.SHIFT_BLOCK_BREAK)) return;
      if(data.length != 4) return;
      
      boolean success = doCancelRecharging(player, action, item, event);
      if(success) return;
      doSetRecharging(player, action, item, ticks);
    }
  }
  
  private boolean checkIfClickedEntity(Player player, String action, String[] data)
  {
    String distances = data[data.length - 2]; // Second to last is the thing.
    String[] split_distances = distances.split("-");
    double max_distance = 0;
    double min_distance = 0;
    if(split_distances.length == 1) // Only one distance
    {
      try{max_distance = Double.parseDouble(split_distances[0]);}catch(NumberFormatException e){return false;}
    }
    if(split_distances.length == 2) // Has two distances (min-max)
    {
      try{min_distance = Double.parseDouble(split_distances[0]);}catch(NumberFormatException e){return false;}
      try{max_distance = Double.parseDouble(split_distances[1]);}catch(NumberFormatException e){return false;}
    }
    // Getting the looked at entity
    LivingEntity entity = EntityUtil.getLookedAtEntity(player, min_distance, max_distance, 1);
    if(entity == null){return false;} // if the entity isn't anything, return false;
    return true;
  }
  
  @SuppressWarnings("deprecation")
  private boolean doCancelRecharging(Player player, String action, ItemStack item, Cancellable event)
  {
 // Cancel the event if recharging
    if(this.isRecharging(player, action, item))
    {
      SamEnchantments.debugLog("Enchantment Recharge cancelled event as item was still recharging");
      SamEnchantments.debugLog("  Info: Player-" + player.getName() + " action-" + action + " itemtype-" +item.getType());
      Sound cooldown_pending_sound = Sound.valueOf(this.cooldown_pending_sound);
      if(cooldown_pending_sound != null) player.playSound(player.getLocation(), cooldown_pending_sound, .1F, .5F);
      else player.playSound(player.getLocation(), this.cooldown_pending_sound, .1F, .5F);
      event.setCancelled(true);
      return true;
    }
    return false;
  }
  private void doSetRecharging(Player player, String action, ItemStack item, int ticks)
  {
    SamEnchantments.debugLog("Enchantment Recharge starting recharging");
    SamEnchantments.debugLog("  Info: Player-" + player.getName() + " action-" + action + " itemtype-" +item.getType());
    this.printRecharging();
    
    // Setting up the recharge
    Recharge recharge = new Recharge(player, action, item, this, ticks);
    recharge.setVisualAmount(this.do_amount_show);
    recharge.setVisualDurability(this.do_durability_show);
    recharge.setVisualTick(this.visual_tick);
    this.setItemRecharging(player, action, item, recharge);
    recharge.start();
    this.printRecharging();
  }
  
  @Override
  public void onPlayerInteract(PlayerInteractEvent event, LoreEnchantment ench, String[] data)
  {
    if(data.length < 1) return;
    
    // Getting the time (should be the last value)
    int time = StringUtil.getSeconds(data[data.length - 1]);
    int ticks = time * 20;
    
    String action = "";
    if(data.length > 1) for(String d : data) action += d + " ";
    if(action.length() == 0) action = "All";
    action = action.trim();
    
    Player player = event.getPlayer();
    ItemStack item = event.getItem();
    
    // Grabbing the action_type
    ActionType action_type = ActionType.getActionType(event);
    SamEnchantments.debugLog("Routing using action: '" + action + "', ActionType: '" + action_type.toString() + "'");
    this.route(player, action, action_type, item, event, data, ticks);
    
  }
  
  @Override
  public void onBlockBreak(BlockBreakEvent event, LoreEnchantment ench, String[] data)
  {
    if(data.length < 1) return;
    
    // Getting the time (should be the last value)
    int time = StringUtil.getSeconds(data[data.length - 1]);
    int ticks = time * 20;
    
    String action = "";
    if(data.length > 1) for(String d : data) action += d + " ";
    if(action.length() == 0) action = "All";
    action = action.trim();
    
    // Getting the Player
    Player player = event.getPlayer();
    ItemStack item = player.getItemInHand();
    
    // Grabbing the action_type
    ActionType action_type;
    if(!player.isSneaking()) action_type = ActionType.BLOCK_BREAK;
    else action_type = ActionType.SHIFT_BLOCK_BREAK;
        
    SamEnchantments.debugLog("Routing using action: '" + action + "', ActionType: '" + action_type.toString() + "'");
    this.route(player, action, action_type, item, event, data, ticks);
  }

  @Override
  public void onPlayerItemConsume(PlayerItemConsumeEvent event,LoreEnchantment ench, String[] data)
  {
    if(data.length < 1) return;
    
    // Getting the time (should be the last value)
    int time = StringUtil.getSeconds(data[data.length - 1]);
    int ticks = time * 20;
    
    String action = "";
    if(data.length > 1) for(String d : data) action += d + " ";
    if(action.length() == 0) action = "All";
    action = action.trim();
    
    // Getting the Player
    Player player = event.getPlayer();
    ItemStack item = event.getItem();
    
    // Grabbing the action_type
    ActionType action_type;
    if(!player.isSneaking()) action_type = ActionType.CONSUME;
    else action_type = ActionType.SHIFT_CONSUME;
        
    SamEnchantments.debugLog("Routing using action: '" + action + "', ActionType: '" + action_type.toString() + "'");
    this.route(player, action, action_type, item, event, data, ticks); 
  }

  @Override
  public void onEntityShootBow(EntityShootBowEvent event, LoreEnchantment ench,
      String[] data)
  {
    if(data.length < 1) return;
    
    if(!(event.getEntity() instanceof Player)) return;

    // Getting the time (should be the last value)
    int time = StringUtil.getSeconds(data[data.length - 1]);
    int ticks = time * 20;
    
    String action = "";
    if(data.length > 1) for(String d : data) action += d + " ";
    if(action.length() == 0) action = "All";
    action = action.trim();
    
    // Getting the Player
    Player player = (Player)event.getEntity();
    ItemStack item = event.getBow();
    
    // Grabbing the action_type
    ActionType action_type;
    if(!player.isSneaking()) action_type = ActionType.SHOOT_BOW;
    else action_type = ActionType.SHIFT_SHOOT_BOW;
        
    SamEnchantments.debugLog("Routing using action: '" + action + "', ActionType: '" + action_type.toString() + "'");
    this.route(player, action, action_type, item, event, data, ticks);
    
  }

  @Override
  public void onEntityDamageEntity(EntityDamageByEntityEvent event,
      LoreEnchantment ench, String[] data)
  {
    if(data.length < 1) return;
    
    if(!(event.getDamager() instanceof Player)) return;
    
    // Getting the time (should be the last value)
    int time = StringUtil.getSeconds(data[data.length - 1]);
    int ticks = time * 20;
    
    String action = "";
    if(data.length > 1) for(String d : data) action += d + " ";
    if(action.length() == 0) action = "All";
    action = action.trim();
    
    // Getting the Player
    Player player = (Player)event.getEntity();
    ItemStack item = player.getItemInHand();
    
    // Grabbing the action_type
    ActionType action_type;
    if(!player.isSneaking()) action_type = ActionType.SHOOT_BOW;
    else action_type = ActionType.SHIFT_SHOOT_BOW;
        
    SamEnchantments.debugLog("Routing using action: '" + action + "', ActionType: '" + action_type.toString() + "'");
    this.route(player, action, action_type, item, event, data, ticks);
  }
}
