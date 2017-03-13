package crazypants.enderio.conduit.refinedstorage;

import crazypants.enderio.config.Config;
import net.minecraftforge.fml.common.Loader;

public class RSUtil {

	  private static boolean useCheckPerformed = false;
	  private static boolean isRSConduitEnabled = false;

	  public static boolean isRSEnabled() {
	    if(!useCheckPerformed) {
	    	isRSConduitEnabled = Loader.isModLoaded("refinedstorage") && Config.enableRSConduits;
	      useCheckPerformed = true;
	    }
	    return isRSConduitEnabled;
	  }
}
