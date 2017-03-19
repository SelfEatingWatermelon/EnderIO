package crazypants.enderio.conduit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.enderio.core.common.util.BlockCoord;
import com.enderio.core.common.util.DyeColor;

import crazypants.enderio.EnderIO;
import crazypants.enderio.GuiID;
import crazypants.enderio.conduit.IConduitBundle.FacadeRenderState;
import crazypants.enderio.conduit.me.MEUtil;
import crazypants.enderio.conduit.oc.OCUtil;
import crazypants.enderio.conduit.redstone.IRedstoneConduit;
import crazypants.enderio.conduit.redstone.Signal;
import crazypants.enderio.conduit.registry.ConduitRegistry;
import crazypants.enderio.machine.RedstoneControlMode;
import crazypants.enderio.paint.YetaUtil;
import crazypants.enderio.sound.IModSound;
import crazypants.enderio.sound.SoundHelper;
import net.minecraft.block.SoundType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static crazypants.enderio.ModObject.itemConduitProbe;

public class ConduitUtil {

  public static final Random RANDOM = new Random();

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static void ensureValidNetwork(IConduit conduit) {
    TileEntity te = conduit.getBundle().getEntity();
    World world = te.getWorld();
    Collection<? extends IConduit> connections = ConduitUtil.getConnectedConduits(world, te.getPos(), conduit.getBaseConduitType());

    if (reuseNetwork(conduit, connections, world)) {
      return;
    }

    AbstractConduitNetwork res = conduit.createNetworkForType();
    res.init(conduit.getBundle(), connections, world);
    return;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static boolean reuseNetwork(IConduit con, Collection<? extends IConduit> connections, World world) {
    AbstractConduitNetwork network = null;
    for (IConduit conduit : connections) {
      if (network == null) {
        network = conduit.getNetwork();
      } else if (network != conduit.getNetwork()) {
        return false;
      }
    }
    if (network == null) {
      return false;
    }
    if (con.setNetwork(network)) {
      network.addConduit(con);
      return true;
    }
    return false;
  }

  public static <T extends IConduit> void disconectConduits(T con, EnumFacing connDir) {
    con.conduitConnectionRemoved(connDir);
    BlockCoord loc = con.getLocation().getLocation(connDir);
    IConduit neighbour = ConduitUtil.getConduit(con.getBundle().getEntity().getWorld(), loc.x, loc.y, loc.z, con.getBaseConduitType());
    if (neighbour != null) {
      neighbour.conduitConnectionRemoved(connDir.getOpposite());
      if (neighbour.getNetwork() != null) {
        neighbour.getNetwork().destroyNetwork();
      }
    }
    if (con.getNetwork() != null) { // this should have been destroyed when
                                    // destroying the neighbour's network but
                                    // lets just make sure
      con.getNetwork().destroyNetwork();
    }
    con.connectionsChanged();
    if (neighbour != null) {
      neighbour.connectionsChanged();
    }
  }

  public static <T extends IConduit> boolean joinConduits(T con, EnumFacing faceHit) {
    BlockCoord loc = con.getLocation().getLocation(faceHit);
    IConduit neighbour = ConduitUtil.getConduit(con.getBundle().getEntity().getWorld(), loc.x, loc.y, loc.z, con.getBaseConduitType());
    if (neighbour != null && con.canConnectToConduit(faceHit, neighbour) && neighbour.canConnectToConduit(faceHit.getOpposite(), con)) {
      con.conduitConnectionAdded(faceHit);
      neighbour.conduitConnectionAdded(faceHit.getOpposite());
      if (con.getNetwork() != null) {
        con.getNetwork().destroyNetwork();
      }
      if (neighbour.getNetwork() != null) {
        neighbour.getNetwork().destroyNetwork();
      }
      con.connectionsChanged();
      neighbour.connectionsChanged();
      return true;
    }
    return false;
  }

  public static boolean forceSkylightRecalculation(World worldObj, int xCoord, int yCoord, int zCoord) {
    return forceSkylightRecalculation(worldObj, new BlockPos(xCoord, yCoord, zCoord));
  }

  public static boolean forceSkylightRecalculation(World worldObj, BlockPos pos) {
    int height = worldObj.getHeight(pos).getY();
    if (height <= pos.getY()) {
      for (int i = 1; i < 12; i++) {
        final BlockPos offset = pos.offset(EnumFacing.UP, i);
        if (worldObj.isAirBlock(offset)) {
          // We need to force the re-lighting of the column due to a change
          // in the light reaching below the block from the sky. To avoid
          // modifying core classes to expose this functionality I am just
          // placing then breaking
          // a block above this one to force the check

          worldObj.setBlockState(offset, Blocks.STONE.getDefaultState(), 3);
          worldObj.setBlockToAir(offset);

          return true;
        }
      }
    }
    return false;
  }

  @SideOnly(Side.CLIENT)
  public static FacadeRenderState getRequiredFacadeRenderState(IConduitBundle bundle, EntityPlayer player) {
    if (!bundle.hasFacade()) {
      return FacadeRenderState.NONE;
    }
    if (YetaUtil.isFacadeHidden(bundle, player)) {
      return FacadeRenderState.WIRE_FRAME;
    }
    return FacadeRenderState.FULL;
  }

  public static boolean isConduitEquipped(EntityPlayer player) {
    return isConduitEquipped(player, EnumHand.MAIN_HAND);
  }

  public static boolean isConduitEquipped(EntityPlayer player, EnumHand hand) {
    player = player == null ? EnderIO.proxy.getClientPlayer() : player;
    if (player == null) {
      return false;
    }
    ItemStack equipped = player.getHeldItem(hand);
    if (equipped == null) {
      return false;
    }
    return equipped.getItem() instanceof IConduitItem;
  }

  public static boolean isProbeEquipped(EntityPlayer player, EnumHand hand) {
    player = player == null ? EnderIO.proxy.getClientPlayer() : player;
    if (player == null) {
      return false;
    }
    ItemStack equipped = player.getHeldItem(hand);
    if (equipped == null) {
      return false;
    }
    return equipped.getItem() == itemConduitProbe.getItem();
  }

  public static <T extends IConduit> T getConduit(World world, int x, int y, int z, Class<T> type) {
    return getConduit(world, new BlockPos(x, y, z), type);
  }

  public static <T extends IConduit> T getConduit(World world, BlockPos pos, Class<T> type) {
    if (world == null) {
      return null;
    }
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof IConduitBundle) {
      IConduitBundle con = (IConduitBundle) te;
      return con.getConduit(type);
    }
    return null;
  }

  public static <T extends IConduit> T getConduit(World world, TileEntity te, EnumFacing dir, Class<T> type) {
    return ConduitUtil.getConduit(world, te.getPos().getX() + dir.getFrontOffsetX(), te.getPos().getY() + dir.getFrontOffsetY(),
        te.getPos().getZ() + dir.getFrontOffsetZ(), type);
  }

  public static <T extends IConduit> Collection<T> getConnectedConduits(World world, int x, int y, int z, Class<T> type) {
    return getConnectedConduits(world, new BlockPos(x, y, z), type);
  }

  public static <T extends IConduit> Collection<T> getConnectedConduits(World world, BlockPos pos, Class<T> type) {
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof IConduitBundle)) {
      return Collections.emptyList();
    }
    List<T> result = new ArrayList<T>();
    IConduitBundle root = (IConduitBundle) te;
    T con = root.getConduit(type);
    if (con != null) {
      for (EnumFacing dir : con.getConduitConnections()) {
        T connected = getConduit(world, root.getEntity(), dir, type);
        if (connected != null) {
          result.add(connected);
        }

      }
    }
    return result;
  }

  public static void writeToNBT(IConduit conduit, NBTTagCompound conduitRoot) {
    if (conduit == null) {
      conduitRoot.setString("UUID", UUID.nameUUIDFromBytes("null".getBytes()).toString());
    } else {
      conduitRoot.setString("UUID", ConduitRegistry.getInstanceUUID(conduit).toString());
      conduit.writeToNBT(conduitRoot);
    }
  }

  public static IConduit readConduitFromNBT(NBTTagCompound conduitRoot, short nbtVersion) {
    if (conduitRoot.hasKey("UUID")) {
      String UUIDString = conduitRoot.getString("UUID");
      IConduit result = ConduitRegistry.getInstance(UUID.fromString(UUIDString));
      if (result != null) {
        result.readFromNBT(conduitRoot, nbtVersion);
      }
      return result;
    }

    // legacy NBT
    String typeName = conduitRoot.getString("conduitType");
    NBTTagCompound conduitBody = conduitRoot.getCompoundTag("conduit");
    if (typeName == null || conduitBody == null) {
      return null;
    }
    if ((typeName.contains("conduit.oc") && !OCUtil.isOCEnabled()) || (typeName.contains("conduit.me") && !MEUtil.isMEEnabled())) {
      return null;
    }
    IConduit result;
    try {
      result = (IConduit) Class.forName(typeName).newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Could not create an instance of the conduit with name: " + typeName, e);
    }
    result.readFromNBT(conduitBody, nbtVersion);
    return result;

  }

  public static boolean isRedstoneControlModeMet(IConduit conduit, RedstoneControlMode mode, DyeColor col) {

    if (mode == RedstoneControlMode.IGNORE) {
      return true;
    } else if (mode == RedstoneControlMode.NEVER) {
      return false;
    } else if (mode == null) {
      return false;
    }

    int signalStrength = getInternalSignalForColor(conduit.getBundle(), col);
    if (signalStrength < RedstoneControlMode.MIN_ON_LEVEL && DyeColor.RED == col) {
      signalStrength = Math.max(signalStrength, conduit.getExternalRedstoneLevel());
    }
    return RedstoneControlMode.isConditionMet(mode, signalStrength);
  }

  public static int isBlockIndirectlyGettingPoweredIfLoaded(World world, BlockPos pos) {
    int i = 0;

    for (EnumFacing enumfacing : EnumFacing.values()) {
      final BlockPos offset = pos.offset(enumfacing);
      if (world.isBlockLoaded(offset)) {
        int j = world.getRedstonePower(offset, enumfacing);

        if (j >= 15) {
          return 15;
        }

        if (j > i) {
          i = j;
        }
      }
    }

    return i;
  }

  public static int getInternalSignalForColor(IConduitBundle bundle, DyeColor col) {
    int signalStrength = 0;
    if (bundle == null) {
      return 0;
    }
    IRedstoneConduit rsCon = bundle.getConduit(IRedstoneConduit.class);
    if (rsCon != null) {
      Collection<Signal> signals = rsCon.getNetworkOutputs(null);
      for (Signal sig : signals) {
        if (sig.color == col) {
          if (sig.strength > signalStrength) {
            signalStrength = sig.strength;
          }
        }
      }
    }
    return signalStrength;
  }

  public static boolean isFluidValid(FluidStack fluidStack) {
    if (fluidStack != null) {
      String name = FluidRegistry.getFluidName(fluidStack);
      if (name != null && !name.trim().isEmpty()) {
        return true;
      }
    }
    return false;
  }

  public static void openConduitGui(World world, BlockPos pos, EntityPlayer player) {
    openConduitGui(world, pos.getX(), pos.getY(), pos.getZ(), player);
  }

  public static void openConduitGui(World world, int x, int y, int z, EntityPlayer player) {
    TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
    if (!(te instanceof TileConduitBundle)) {
      return;
    }
    IConduitBundle cb = (IConduitBundle) te;
    Set<EnumFacing> cons = new HashSet<EnumFacing>();
    boolean hasInsulated = false;
    for (IConduit con : cb.getConduits()) {
      cons.addAll(con.getExternalConnections());
      if (ConduitRegistry.get(con).canConnectToAnything()) {
        hasInsulated = true;
      }
    }
    if (cons.isEmpty() && !hasInsulated) {
      return;
    }
    if (cons.size() == 1) {
      EnumFacing facing = cons.iterator().next();
      GuiID.facing2guiid(facing).openGui(world, new BlockPos(x, y, z), player, facing);
      return;
    }
    GuiID.GUI_ID_EXTERNAL_CONNECTION_SELECTOR.openClientGui(world, new BlockPos(x, y, z), player, null);
  }

  public static void playBreakSound(SoundType snd, World world, int x, int y, int z) {
    SoundHelper.playSound(world, new BlockPos(x, y, z), new Sound(snd.getBreakSound()), (snd.getVolume() + 1.0F) / 2.0F, snd.getPitch() * 0.8F);
  }

  public static void playHitSound(SoundType snd, World world, int x, int y, int z) {
    SoundHelper.playSound(world, new BlockPos(x, y, z), new Sound(snd.getHitSound()), (snd.getVolume() + 1.0F) / 2.0F, snd.getPitch() * 0.8F);
  }

  public static void playStepSound(SoundType snd, World world, int x, int y, int z) {
    SoundHelper.playSound(world, new BlockPos(x, y, z), new Sound(snd.getStepSound()), (snd.getVolume() + 1.0F) / 2.0F, snd.getPitch() * 0.8F);
  }

  public static void playPlaceSound(SoundType snd, World world, int x, int y, int z) {
    SoundHelper.playSound(world, new BlockPos(x, y, z), new Sound(snd.getPlaceSound()), (snd.getVolume() + 1.0F) / 2.0F, snd.getPitch() * 0.8F);
  }

  private static class Sound implements IModSound {

    private final SoundEvent event;

    public Sound(SoundEvent event) {
      this.event = event;
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public SoundEvent getSoundEvent() {
      return event;
    }

    @Override
    public SoundCategory getSoundCategory() {
      return SoundCategory.BLOCKS;
    }

  }
}
