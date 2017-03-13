package crazypants.enderio.conduit.refinedstorage;

import net.minecraft.util.EnumFacing;

import com.raoulvdberge.refinedstorage.api.network.INetworkMaster;

import crazypants.enderio.conduit.IConduit;

public interface IRSConduit extends IConduit {

	void onConnected(INetworkMaster network);

	void onDisconnected(INetworkMaster network);

	boolean isConnected();

	boolean canUpdate();

	boolean canConduct(EnumFacing direction);

	INetworkMaster getNetworkMaster();

}
