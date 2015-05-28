package net.samongi.SamEnchantments.BowEnchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import net.samongi.LoreEnchantments.EventHandling.LoreEnchantment;
import net.samongi.LoreEnchantments.Interfaces.OnEntityShootBow;
import net.samongi.LoreEnchantments.Utilities.StringUtilities;
import net.samongi.SamEnchantments.SamEnchantments;

public class EnchantmentVolley extends LoreEnchantment implements OnEntityShootBow
{
  private JavaPlugin plugin;
  
  public EnchantmentVolley(JavaPlugin plugin)
  {
    super("Volley");
    this.plugin = plugin;
  }

  @Override
  public void onEntityShootBow(EntityShootBowEvent event, String[] data)
  {
    Arrow arrow = (Arrow)event.getProjectile();
    
    ItemStack bow = event.getBow();
    
    // Extracting the needed information from the data
    if(data.length < 1) return;
    String power = data[0];
    int ench_level = 0;
    try{ench_level = Integer.parseInt(power);} catch(NumberFormatException e){}
    if(ench_level == 0) ench_level = StringUtilities.numeralToInt(power);
    if(ench_level == 0) return;
    SamEnchantments.debugLog("Enchantment Volley found level to be: " + ench_level);
    int max_level = plugin.getConfig().getInt("enchantments.volley.max-level",10);
    if(ench_level > max_level) ench_level = max_level;
    Vector main_vector = arrow.getVelocity();
    // double distance = main_vector.length();
    // double yaw = getYaw(main_vector);
    // SamEnchantments.debugLog("Yaw for vector <" + main_vector.getX() + ", " + main_vector.getY() + ", " + main_vector.getZ() + "> : " + yaw);
    // double pitch = getPitch(main_vector);
    // SamEnchantments.debugLog("Pitch for vector <" + main_vector.getX() + ", " + main_vector.getY() + ", " + main_vector.getZ() + "> : " + pitch);
    
    double deviation = ench_level * plugin.getConfig().getDouble("enchantments.volley.deviation-per-level",3);
    // double max_deviation = plugin.getConfig().getDouble("enchantments.volley.max-deviation",3);
    // double max_yaw_diff = ench_level*3;//plugin.getConfig().getDouble("enchantment-config.volley.yaw", 5.0);
    // double max_pitch_diff = ench_level*3;//plugin.getConfig().getDouble("enchantment-config.volley.pitch", 5.0);
    
    if(ench_level < 1) return;
    int arrow_num = (int) Math.pow(2,ench_level); // The number of extra arrows is exponential.
    
    Random rand = new Random();
    // Making a list of the new_traj to file
    List<Vector> new_trajs = new ArrayList<>();
    // for(int i = 0 ; i <= arrow_num ; i++ ) new_trajs.add(fromEuler(distance, (yaw-.5*max_yaw_diff) + rand.nextDouble() * max_yaw_diff, (pitch-.5*max_pitch_diff) + rand.nextDouble() * max_pitch_diff));
    for(int i = 0 ; i <= arrow_num ; i++ ) 
    {
      double x_rotate = (-deviation/2) + (deviation * rand.nextDouble());
      double y_rotate = (-deviation/2) + (deviation * rand.nextDouble());
      double z_rotate = (-deviation/2) + (deviation * rand.nextDouble());
      new_trajs.add(rotateX(rotateY(rotateZ(main_vector, z_rotate), y_rotate), x_rotate));
    }
    for(Vector v : new_trajs)
    {  
      Arrow new_arrow = (Arrow)arrow.getWorld().spawnEntity(arrow.getLocation(), EntityType.ARROW);
      // setting the arrow base info.
      new_arrow.setVelocity(v);
      new_arrow.setShooter(arrow.getShooter());
      new_arrow.setCritical(arrow.isCritical());
      new_arrow.setKnockbackStrength(arrow.getKnockbackStrength());
      new_arrow.setFireTicks(arrow.getFireTicks());
      new_arrow.setBounce(false);
      new_arrow.spigot().setDamage(arrow.spigot().getDamage());
      // Setting that the arrow should be treated as a ghost arrow
      FixedMetadataValue ghost_md = new FixedMetadataValue(this.plugin, true);
      new_arrow.setMetadata("ghost", ghost_md);
      // Setting the bow that shot it.
      FixedMetadataValue bow_md = new FixedMetadataValue(this.plugin, bow.clone());
      new_arrow.setMetadata("bow", bow_md);
      SamEnchantments.debugLog("Double Checking for Metadata: " + new_arrow.hasMetadata("bow"));
      SamEnchantments.debugLog("  Elements: " + new_arrow.getMetadata("bow").size());
    }
  }
  
  private Vector rotateX(Vector vec, double angle)
  {
    double x = vec.getX();
    double y = (vec.getY() * Math.cos(Math.toRadians(angle))) - (vec.getZ() * Math.sin(Math.toRadians(angle)));
    double z = (vec.getY() * Math.sin(Math.toRadians(angle))) + (vec.getZ() * Math.cos(Math.toRadians(angle)));
    return new Vector(x,y,z);
  }
  private Vector rotateY(Vector vec, double angle)
  {
    double x = (vec.getX() * Math.cos(Math.toRadians(angle))) + (vec.getZ() * Math.sin(Math.toRadians(angle)));
    double y = vec.getY();
    double z = -(vec.getX() * Math.sin(Math.toRadians(angle))) + (vec.getZ() * Math.cos(Math.toRadians(angle)));
    return new Vector(x,y,z);
  }
  private Vector rotateZ(Vector vec, double angle)
  {
    double x = (vec.getX() * Math.cos(Math.toRadians(angle))) - (vec.getY() * Math.sin(Math.toRadians(angle)));
    double y = (vec.getX() * Math.sin(Math.toRadians(angle))) + (vec.getY() * Math.cos(Math.toRadians(angle)));
    double z = vec.getZ();
    return new Vector(x,y,z);
  }
  
  // takes angles
  @SuppressWarnings("unused")
  private final static Vector fromEuler(double distance, double yaw, double pitch)
  {
    double x = Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
    double z = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
    double y = Math.sin(Math.toRadians(pitch));
    Vector vec = new Vector(x,y,z);
    // Scales the vector to our liking.
    return vec.multiply(Math.sqrt(distance * distance  / vec.lengthSquared()));
  }
  // in degrees
  @SuppressWarnings("unused")
  private final static double getYaw(Vector vec)
  {
    double x = vec.getX();
    double z = vec.getZ();
    if(z == 0 && x > 0) return 0;
    if(z == 0 && x < 0) return 180;
    double ret = Math.toDegrees(Math.atan(x / z));
    if(z < 0) ret += 180;
    return ret;
  }
  // in degrees
  @SuppressWarnings("unused")
  private final static double getPitch(Vector vec)
  {
    return Math.toDegrees(Math.asin(vec.getY() / vec.length()));
  }
}
