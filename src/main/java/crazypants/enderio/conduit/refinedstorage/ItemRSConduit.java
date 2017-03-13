package crazypants.enderio.conduit.refinedstorage;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import crazypants.enderio.IModObject;
import crazypants.enderio.ModObject;
import crazypants.enderio.conduit.AbstractItemConduit;
import crazypants.enderio.conduit.ConduitDisplayMode;
import crazypants.enderio.conduit.IConduit;
import crazypants.enderio.conduit.ItemConduitSubtype;
import crazypants.enderio.conduit.geom.Offset;
import crazypants.enderio.conduit.registry.ConduitRegistry;
import crazypants.enderio.gui.IconEIO;

public class ItemRSConduit extends AbstractItemConduit {
	
	private static ItemConduitSubtype[] subtypes = new ItemConduitSubtype[] { new ItemConduitSubtype(ModObject.itemRSConduit.name(), "enderio:itemRSConduit") };
	
	private final ConduitRegistry.ConduitInfo conduitInfo;

    public static ItemRSConduit create() {
		if (RSUtil.isRSEnabled()) {
		  ItemRSConduit result = new ItemRSConduit();
		  result.init();
		  return result;
		}
		return null;
    }

    protected ItemRSConduit() {
        super(ModObject.itemRSConduit, subtypes);
        conduitInfo = new ConduitRegistry.ConduitInfo(getBaseConduitType(), Offset.SOUTH_UP, Offset.SOUTH_UP, Offset.NORTH_EAST, Offset.EAST_UP);
        conduitInfo.addMember(RSConduit.class);
        ConduitRegistry.register(conduitInfo);
        ConduitDisplayMode.registerDisplayMode(new ConduitDisplayMode(getBaseConduitType(), IconEIO.WRENCH_OVERLAY_ME, IconEIO.WRENCH_OVERLAY_ME_OFF));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerRenderers() {
      super.registerRenderers();
      conduitInfo.addRenderer(new RSConduitRenderer());
    }

	@Override
	public Class<? extends IConduit> getBaseConduitType() {
		return IRSConduit.class;
	}

	@Override
	public IConduit createConduit(ItemStack item, EntityPlayer player) {
	    RSConduit con = new RSConduit(item.getItemDamage());
	    return con;
	}

	@Override
	public boolean shouldHideFacades(ItemStack stack, EntityPlayer player) {
		return true;
	}

}
