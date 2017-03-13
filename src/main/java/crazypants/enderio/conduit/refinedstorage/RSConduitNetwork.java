package crazypants.enderio.conduit.refinedstorage;

import crazypants.enderio.conduit.AbstractConduitNetwork;

public class RSConduitNetwork extends AbstractConduitNetwork<IRSConduit, IRSConduit> {

	protected RSConduitNetwork() {
		super(IRSConduit.class, IRSConduit.class);
	}

}
