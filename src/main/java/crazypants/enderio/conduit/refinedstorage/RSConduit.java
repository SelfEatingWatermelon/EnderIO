package crazypants.enderio.conduit.refinedstorage;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

import com.enderio.core.api.client.gui.ITabPanel;
import com.enderio.core.common.vecmath.Vector4f;
import com.raoulvdberge.refinedstorage.api.IRSAPI;
import com.raoulvdberge.refinedstorage.api.RSAPIInject;
import com.raoulvdberge.refinedstorage.api.network.INetworkMaster;
import com.raoulvdberge.refinedstorage.api.network.INetworkNode;

import crazypants.enderio.conduit.AbstractConduit;
import crazypants.enderio.conduit.AbstractConduitNetwork;
import crazypants.enderio.conduit.ConnectionMode;
import crazypants.enderio.conduit.IConduit;
import crazypants.enderio.conduit.RaytraceResult;
import crazypants.enderio.conduit.geom.CollidableComponent;
import crazypants.enderio.conduit.gui.GuiExternalConnection;
import crazypants.enderio.conduit.gui.RSSettings;
import crazypants.enderio.render.registry.TextureRegistry;
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

  @RSAPIInject
  public static IRSAPI RSAPI;

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

  public boolean hasConnectableConditions(TileEntity te) {
    for (Predicate<TileEntity> p : RSAPI.getConnectableConditions()) {
      if (p.test(te)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean canConnectToExternal(EnumFacing direction, boolean ignoreConnectionMode) {
    // Can we conduct in the provided direction?
    if (canConduct(direction)) {
      TileEntity facing = getLocation().getLocation(direction).getTileEntity(getBundle().getBundleWorldObj());

      // Check if the facing tile has connectable conditions
      if (hasConnectableConditions(facing)) {

        // They can connect, but if they are also an INetworkNode check if they can connect in our direction
        if (facing instanceof INetworkNode) {
          return ((INetworkNode) facing).canAcceptConnection(direction.getOpposite());
        }

        return true;
      }

    }

    return false;
  }

  public EnumSet<EnumFacing> getConnections() {
    EnumSet<EnumFacing> cons = EnumSet.noneOf(EnumFacing.class);
    cons.addAll(getConduitConnections());
    for (EnumFacing dir : getExternalConnections()) {
      cons.add(dir);
    }
    return cons;
  }

  @Override
  public ConnectionMode getNextConnectionMode(EnumFacing dir) {
    return ConnectionMode.IN_OUT;
  }

  @Override
  public ConnectionMode getPreviousConnectionMode(EnumFacing dir) {
    return ConnectionMode.IN_OUT;
  }

  @Override
  public void connectionsChanged() {
    super.connectionsChanged();
    if (this.rsnet != null) {
      this.rsnet.getNodeGraph().rebuild();
    }
  }

  @Override
  public boolean onBlockActivated(EntityPlayer player, EnumHand hand, RaytraceResult res, List<RaytraceResult> all) {
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
    return true;
  }

  @Override
  public INetworkMaster getNetworkMaster() {
    return this.rsnet;
  }

}
