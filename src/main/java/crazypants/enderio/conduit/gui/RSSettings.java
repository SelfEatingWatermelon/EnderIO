package crazypants.enderio.conduit.gui;

import crazypants.enderio.EnderIO;
import crazypants.enderio.conduit.IConduit;
import crazypants.enderio.gui.IconEIO;

public class RSSettings extends BaseSettingsPanel {

  public RSSettings(GuiExternalConnection gui, IConduit con) {
	super(IconEIO.WRENCH_OVERLAY_RS, EnderIO.lang.localize("itemRSConduit.name"), gui, con);
  }

}
