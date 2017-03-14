package crazypants.enderio.conduit.refinedstorage;

import java.util.EnumSet;
import java.util.List;

import com.enderio.core.api.client.gui.ITabPanel;
import com.enderio.core.common.vecmath.Vector4f;
import com.raoulvdberge.refinedstorage.api.network.INetworkMaster;
import com.raoulvdberge.refinedstorage.api.network.INetworkNode;

import crazypants.enderio.conduit.AbstractConduit;
import crazypants.enderio.conduit.AbstractConduitNetwork;
import crazypants.enderio.conduit.ConduitUtil;
import crazypants.enderio.conduit.ConnectionMode;
import crazypants.enderio.conduit.IConduit;
import crazypants.enderio.conduit.RaytraceResult;
import crazypants.enderio.conduit.geom.CollidableComponent;
import crazypants.enderio.conduit.gui.GuiExternalConnection;
import crazypants.enderio.conduit.gui.RSSettings;
import crazypants.enderio.render.registry.TextureRegistry;
import crazypants.enderio.tool.ToolUtil;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static crazypants.enderio.ModObject.itemRSConduit;

public class RSConduit extends AbstractConduit implements IRSConduit {

  protected RSConduitNetwork network = null;

  protected boolean connected = false;
  protected INetworkMaster rsnet = null;

  public static TextureRegistry.TextureSupplier coreTexture = TextureRegistry.registerTexture("blocks/rsConduitCore");
  public static TextureRegistry.TextureSupplier longTexture = TextureRegistry.registerTexture("blocks/rsConduit");

  public RSConduit() {
    super();
  }

  public RSConduit(int meta) {
    super();
  }

  @SideOnly(Side.CLIENT)
  public static void initIcons() {
  }

  @Override
  public Class<? extends IConduit> getBaseConduitType() {
    return IRSConduit.class;
  }

  @Override
  public ItemStack createItem() {
    return new ItemStack(itemRSConduit.getItem(), 1, 0);
  }

  @Override
  public AbstractConduitNetwork<?, ?> getNetwork() {
    return network;
  }

  @Override
  public boolean setNetwork(AbstractConduitNetwork<?, ?> network) {
    this.network = (RSConduitNetwork) network;
    return true;
  }

  @Override
  public AbstractConduitNetwork<?, ?> createNetworkForType() {
    return new RSConduitNetwork();
  }

  @Override
  public TextureAtlasSprite getTextureForState(CollidableComponent component) {
    if (component.dir == null) {
      return coreTexture.get(TextureAtlasSprite.class);
    } else {
      return longTexture.get(TextureAtlasSprite.class);
    }
  }

  @Override
  public TextureAtlasSprite getTransmitionTextureForState(CollidableComponent component) {
    return null;
  }

  @Override
  public Vector4f getTransmitionTextureColorForState(CollidableComponent component) {
    return null;
  }

  @Override
  public ITabPanel createPanelForConduit(GuiExternalConnection gui, IConduit con) {
    return new RSSettings(gui, con);
  }

  @Override
  public int getTabOrderForConduit(IConduit con) {
    return 6;
  }

  @Override
  public boolean canConnectToExternal(EnumFacing direction, boolean ignoreConnectionMode) {
    if (getConnectionMode(direction) == ConnectionMode.DISABLED && !ignoreConnectionMode) {
      return false;
    }

    TileEntity te = getLocation().getLocation(direction).getTileEntity(getBundle().getBundleWorldObj());
    if (te instanceof INetworkNode) {
      return ((INetworkNode) te).canConduct(direction.getOpposite());
    } else if (te instanceof INetworkMaster) {
      return true;
    }

    return false;
  }

  @Override
  public void connectionsChanged() {
    if (rsnet != null) {
      rsnet.getNodeGraph().rebuild();
    }
    super.connectionsChanged();
  }

  public EnumSet<EnumFacing> getConnections() {
    EnumSet<EnumFacing> cons = EnumSet.noneOf(EnumFacing.class);
    cons.addAll(getConduitConnections());
    for (EnumFacing dir : getExternalConnections()) {
      if (getConnectionMode(dir) != ConnectionMode.DISABLED) {
        cons.add(dir);
      }
    }
    return cons;
  }

  @Override
  public ConnectionMode getNextConnectionMode(EnumFacing dir) {
    ConnectionMode mode = getConnectionMode(dir);
    mode = mode == ConnectionMode.IN_OUT ? ConnectionMode.DISABLED : ConnectionMode.IN_OUT;
    return mode;
  }

  @Override
  public ConnectionMode getPreviousConnectionMode(EnumFacing dir) {
    return getNextConnectionMode(dir);
  }

  @Override
  public void setConnectionMode(EnumFacing dir, ConnectionMode mode) {
    if (this.rsnet != null && isConnectedTo(dir)) {
      this.rsnet.getNodeGraph().rebuild();
    }
    super.setConnectionMode(dir, mode);
  }

  @Override
  public boolean onBlockActivated(EntityPlayer player, EnumHand hand, RaytraceResult res, List<RaytraceResult> all) {
    if (ToolUtil.isToolEquipped(player, hand)) {
      if (!getBundle().getEntity().getWorld().isRemote) {
        if (res != null && res.component != null) {
          EnumFacing connDir = res.component.dir;
          EnumFacing faceHit = res.movingObjectPosition.sideHit;
          if (connDir == null || connDir == faceHit) {
            if (getConnectionMode(faceHit) == ConnectionMode.DISABLED) {
              setConnectionMode(faceHit, ConnectionMode.IN_OUT);
              return true;
            }
            return ConduitUtil.joinConduits(this, faceHit);
          } else if (externalConnections.contains(connDir)) {
            setConnectionMode(connDir, getNextConnectionMode(connDir));
            return true;
          } else if (containsConduitConnection(connDir)) {
            ConduitUtil.disconectConduits(this, connDir);
            return true;
          }
        }
      }
    }
    return false;
  }

  @Override
  public void onAddedToBundle() {
    super.onAddedToBundle();

    World world = bundle.getBundleWorldObj();
    EnumSet<EnumFacing> conns = getConnections();
    for (EnumFacing facing : conns) {
      TileEntity tile = getLocation().getLocation(facing).getTileEntity(world);
      if (tile instanceof INetworkNode && ((INetworkNode) tile).isConnected()) {
        ((INetworkNode) tile).getNetwork().getNodeGraph().rebuild();
        break;
      } else if (tile instanceof INetworkMaster) {
        ((INetworkMaster) tile).getNodeGraph().rebuild();
        break;
      }
    }
  }

  @Override
  public void onRemovedFromBundle() {
    super.onRemovedFromBundle();

    if (this.rsnet != null) {
      this.rsnet.getNodeGraph().rebuild();
    }
  }

  @Override
  public void onConnected(INetworkMaster network) {
    this.connected = true;
    this.rsnet = network;
  }

  @Override
  public void onDisconnected(INetworkMaster network) {
    this.connected = false;
    this.rsnet = null;
  }

  @Override
  public boolean isConnected() {
    return this.connected;
  }

  @Override
  public boolean canUpdate() {
    return true;
  }

  @Override
  public boolean canConduct(EnumFacing direction) {
    return getConnectionMode(direction) != ConnectionMode.DISABLED;
  }

  @Override
  public INetworkMaster getNetworkMaster() {
    return this.rsnet;
  }

}
