package cr0s.WarpDrive.machines;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Optional;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.lua.ILuaContext;

import java.util.ArrayList;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.ForgeDirection;
import cr0s.WarpDrive.*;

public class TileEntityRadar extends WarpEnergyTE {
	private ArrayList<TileEntityReactor> results;

	private int scanRadius = 0;
	private int cooldownTime = 0;
	
	public TileEntityRadar() {
		peripheralName = "radar";
		methodsArray = new String[] {
				"scanRay",			// 0
				"scanRadius",		// 1
				"getResultsCount",	// 2
				"getResult",		// 3
				"getEnergyLevel",	// 4
				"pos"				// 5
			};
	}
	
	@Override
	public void updateEntity() {
		if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
			return;
		}
		super.updateEntity();

		try {
			if (getBlockMetadata() == 2) {
				cooldownTime++;
				if (cooldownTime > (20 * ((scanRadius / 1000) + 1))) {
					WarpDrive.debugPrint("" + this + " Scanning over " + scanRadius + " radius...");
					results = WarpDrive.warpCores.searchWarpCoresInRadius(xCoord, yCoord, zCoord, scanRadius);
					WarpDrive.debugPrint("" + this + " Scan found " + results.size() + " results");
					worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 1, 1 + 2);
					cooldownTime = 0;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
	}

	// ComputerCraft IPeripheral methods implementation
	@Override
	public void attach(IComputerAccess computer) {
		super.attach(computer);
		if (WarpDriveConfig.G_LUA_SCRIPTS != WarpDriveConfig.LUA_SCRIPTS_NONE) {
			computer.mount("/radar", ComputerCraftAPI.createResourceMount(WarpDrive.class, "warpdrive", "lua/radar"));
	        computer.mount("/warpupdater", ComputerCraftAPI.createResourceMount(WarpDrive.class, "warpdrive", "lua/common/updater"));
			if (WarpDriveConfig.G_LUA_SCRIPTS == WarpDriveConfig.LUA_SCRIPTS_ALL) {
				computer.mount("/scan", ComputerCraftAPI.createResourceMount(WarpDrive.class, "warpdrive", "lua/radar/scan"));
				computer.mount("/ping", ComputerCraftAPI.createResourceMount(WarpDrive.class, "warpdrive", "lua/radar/ping"));
			}
		}
		if (getBlockMetadata() == 0) {
			worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 1, 1 + 2);
		}
	}

	@Override
	public void detach(IComputerAccess computer) {
		super.detach(computer);
		// worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 0, 1 + 2);
	}
	
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws Exception {
	   	String methodName = methodsArray[method];
		if (methodName.equals("scanRay")) {// scanRay (toX, toY, toZ)
			return new Object[] { -1 };
			
		} else if (methodName.equals("scanRadius")) {// scanRadius (radius)
			// always clear results
			results = null;
			
			// validate parameters
			if (arguments.length != 1) {
				return new Boolean[] { false };
			}
			int radius;
			try {
				radius = toInt(arguments[0]);
			} catch(Exception e) {
               	return new Boolean[] { false };
            }
			if (radius <= 0 || radius > 10000) {
				scanRadius = 0;
				return new Boolean[] { false };
			}
			if (!consumeEnergy(Math.max(radius, 100) * Math.max(radius, 100), false)) {
				return new Boolean[] { false };
			}
			
			// Begin searching
			scanRadius = radius;
			cooldownTime = 0;
			if (getBlockMetadata() != 2) {
				worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 2, 1 + 2);
			}
			return new Boolean[] { true };

		} else if (methodName.equals("getResultsCount")) {
			if (results != null) {
				return new Integer[] { results.size() };
			}
			return new Integer[] { -1 };
			
		} else if (methodName.equals("getResult")) {
			if (arguments.length == 1 && (results != null)) {
				int index;
				try {
					index = toInt(arguments[0]);
				} catch(Exception e) {
					return new Object[] { "FAIL", 0, 0, 0 };
				}
				if (index >= 0 && index < results.size()) {
					TileEntityReactor res = results.get(index);
					if (res != null)
					{
						int yAddition = (res.worldObj.provider.dimensionId == WarpDriveConfig.G_SPACE_DIMENSION_ID) ? 256 : (res.worldObj.provider.dimensionId == WarpDriveConfig.G_HYPERSPACE_DIMENSION_ID) ? 512 : 0;
						return new Object[] { res.coreFrequency, res.xCoord, res.yCoord + yAddition, res.zCoord };
					}
				}
			}
			return new Object[] { "FAIL", 0, 0, 0 };
			
		} else if (methodName.equals("getEnergyLevel")) {
			return new Integer[] { getEnergyStored() };
				
		} else if (methodName.equals("pos")) {
			return new Integer[] { xCoord, yCoord, zCoord };
		}

		return null;
	}

	@Override
	public int getMaxEnergyStored() {
		return WarpDriveConfig.WR_MAX_ENERGY_VALUE;
	}

	// IEnergySink methods implementation
	@Override
	public int getMaxSafeInput() {
		return Integer.MAX_VALUE;
	}

    @Override
    public boolean canInputEnergy(ForgeDirection from) {
    	return true;
    }
}
