package cofh.thermalexpansion.block.device;

import cofh.api.energy.IEnergyReceiver;
import cofh.core.CoFHProps;
import cofh.lib.util.helpers.BlockHelper;
import cofh.lib.util.helpers.ServerHelper;
import cofh.thermalexpansion.block.device.BlockDevice.Types;
import cpw.mods.fml.common.registry.GameRegistry;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

public class TileExtender extends TileDeviceBase implements IFluidHandler {

	public static void initialize() {

		int type = BlockDevice.Types.EXTENDER.ordinal();

		defaultSideConfig[type] = new SideConfig();
		defaultSideConfig[type].numConfig = 2;
		defaultSideConfig[type].slotGroups = new int[][] { {}, {} };
		defaultSideConfig[type].allowInsertionSide = new boolean[] { false, false };
		defaultSideConfig[type].allowExtractionSide = new boolean[] { false, false };
		defaultSideConfig[type].allowInsertionSlot = new boolean[] {};
		defaultSideConfig[type].allowExtractionSlot = new boolean[] {};
		defaultSideConfig[type].sideTex = new int[] { 0, 7 };
		defaultSideConfig[type].defaultSides = new byte[] { 0, 0, 0, 0, 0, 0 };

		GameRegistry.registerTileEntity(TileExtender.class, "thermalexpansion.Extender");
	}

	static final int MAX_CACHE_LEVEL = 128;
	static int[][] slotsCache;

	static {
		slotsCache = new int[MAX_CACHE_LEVEL][];
		slotsCache[0] = CoFHProps.EMPTY_INVENTORY;
	}

	public static int[] getNonSidedSlots(int n) {

		if (n < MAX_CACHE_LEVEL && slotsCache[n] != null) {
			return slotsCache[n];
		}
		int[] slots = new int[n];
		for (int j = 0; j < n; j++) {
			slots[j] = j;
		}
		if (n < MAX_CACHE_LEVEL) {
			slotsCache[n] = slots;
		}
		return slots;
	}

	ISidedInventory targetInventorySided;
	IInventory targetInventory;
	IEnergyReceiver targetReceiver;
	IFluidHandler targetHandler;

	TileEntity targetTile;
	boolean cached = false;

	boolean polling = false;

	public TileExtender() {

		super(Types.EXTENDER);
	}

	@Override
	public void onNeighborBlockChange() {

		super.onNeighborBlockChange();
		updateHandlers();
	}

	@Override
	public void onNeighborTileChange(int tileX, int tileY, int tileZ) {

		super.onNeighborTileChange(tileX, tileY, tileZ);
		updateHandlers();
	}

	@Override
	public void markDirty() {

		super.markDirty();

		if (checkPollingAndNeighbor()) {
			return;
		}
		polling = true;
		if (targetTile != null) {
			targetTile.markDirty();
		}
		polling = false;
	}

	protected void updateHandlers() {

		cached = true;

		if (ServerHelper.isClientWorld(worldObj)) {
			return;
		}
		targetTile = BlockHelper.getAdjacentTileEntity(this, facing);

		if (targetTile != null) {
			if (targetTile.isInvalid()) {
				targetTile = null;
			} else if (targetTile instanceof TileExtender) {
				targetTile = null;
			}
		}
		if (targetTile instanceof ISidedInventory) {
			targetInventorySided = (ISidedInventory) targetTile;
		} else {
			targetInventorySided = null;
		}
		if (targetTile instanceof IInventory) {
			targetInventory = (IInventory) targetTile;
		} else {
			targetInventory = null;
		}
		if (targetTile instanceof IEnergyReceiver) {
			targetReceiver = (IEnergyReceiver) targetTile;
		} else {
			targetReceiver = null;
		}
		if (targetTile instanceof IFluidHandler) {
			targetHandler = (IFluidHandler) targetTile;
		} else {
			targetHandler = null;
		}
	}

	public boolean checkPollingAndNeighbor() {

		if (polling || ServerHelper.isClientWorld(worldObj)) {
			return true;
		}
		if (!cached || (targetTile != null && targetTile.isInvalid())) {
			updateHandlers();
		}
		return targetTile != null && !targetTile.isInvalid();
	}

	/* IReconfigurableFacing */
	@Override
	public boolean rotateBlock() {

		super.rotateBlock();
		updateHandlers();
		return true;
	}

	/* IEnergyReceiver */
	@Override
	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {

		if (checkPollingAndNeighbor()) {
			return 0;
		}
		polling = true;
		int energy = targetReceiver != null ? targetReceiver.receiveEnergy(ForgeDirection.VALID_DIRECTIONS[facing ^ 1], maxReceive, simulate) : 0;
		polling = false;
		return energy;
	}

	@Override
	public int getEnergyStored(ForgeDirection from) {

		if (checkPollingAndNeighbor()) {
			return 0;
		}
		polling = true;
		int energy = targetReceiver != null ? targetReceiver.getEnergyStored(ForgeDirection.VALID_DIRECTIONS[facing ^ 1]) : 0;
		polling = false;
		return energy;
	}

	@Override
	public int getMaxEnergyStored(ForgeDirection from) {

		if (checkPollingAndNeighbor()) {
			return 0;
		}
		polling = true;
		int maxEnergy = targetReceiver != null ? targetReceiver.getMaxEnergyStored(ForgeDirection.VALID_DIRECTIONS[facing ^ 1]) : 0;
		polling = false;
		return maxEnergy;
	}

	@Override
	public boolean canConnectEnergy(ForgeDirection from) {

		if (checkPollingAndNeighbor()) {
			return false;
		}
		polling = true;
		boolean canConnect = targetReceiver != null ? targetReceiver.canConnectEnergy(ForgeDirection.VALID_DIRECTIONS[facing ^ 1]) : false;
		polling = false;
		return canConnect;
	}

	/* IFluidHandler */
	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {

		if (from.ordinal() == facing) {
			return 0;
		}
		if (checkPollingAndNeighbor()) {
			return 0;
		}
		polling = true;
		int amount = targetHandler != null ? targetHandler.fill(ForgeDirection.VALID_DIRECTIONS[facing ^ 1], resource, doFill) : 0;
		polling = false;
		return amount;
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {

		if (from.ordinal() == facing) {
			return null;
		}
		if (checkPollingAndNeighbor()) {
			return null;
		}
		polling = true;
		FluidStack drain = targetHandler != null ? targetHandler.drain(ForgeDirection.VALID_DIRECTIONS[facing ^ 1], resource, doDrain) : null;
		polling = false;
		return drain;
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {

		if (from.ordinal() == facing) {
			return null;
		}
		if (checkPollingAndNeighbor()) {
			return null;
		}
		polling = true;
		FluidStack drain = targetHandler != null ? targetHandler.drain(ForgeDirection.VALID_DIRECTIONS[facing ^ 1], maxDrain, doDrain) : null;
		polling = false;
		return drain;
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid) {

		if (from.ordinal() == facing) {
			return false;
		}
		if (checkPollingAndNeighbor()) {
			return false;
		}
		polling = true;
		boolean canFill = targetHandler != null ? targetHandler.canFill(ForgeDirection.VALID_DIRECTIONS[facing ^ 1], fluid) : false;
		polling = false;
		return canFill;
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid) {

		if (from.ordinal() == facing) {
			return false;
		}
		if (checkPollingAndNeighbor()) {
			return false;
		}
		polling = true;
		boolean canDrain = targetHandler != null ? targetHandler.canDrain(ForgeDirection.VALID_DIRECTIONS[facing ^ 1], fluid) : false;
		polling = false;
		return canDrain;
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from) {

		if (from.ordinal() == facing) {
			return CoFHProps.EMPTY_TANK_INFO;
		}
		if (checkPollingAndNeighbor()) {
			return CoFHProps.EMPTY_TANK_INFO;
		}
		polling = true;
		FluidTankInfo[] tankInfo = targetHandler != null ? targetHandler.getTankInfo(ForgeDirection.VALID_DIRECTIONS[facing ^ 1]) : CoFHProps.EMPTY_TANK_INFO;
		polling = false;
		return tankInfo;
	}

	/* IInventory */
	@Override
	public int getSizeInventory() {

		if (ServerHelper.isClientWorld(worldObj)) {
			return 1;
		}
		if (checkPollingAndNeighbor()) {
			return 0;
		}
		polling = true;
		int size = targetInventory != null ? targetInventory.getSizeInventory() : 0;
		polling = false;
		return size;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {

		if (checkPollingAndNeighbor()) {
			return null;
		}
		polling = true;
		ItemStack stack = targetInventory != null ? targetInventory.getStackInSlot(slot) : null;
		polling = false;
		return stack;
	}

	@Override
	public ItemStack decrStackSize(int slot, int amount) {

		if (checkPollingAndNeighbor()) {
			return null;
		}
		polling = true;
		ItemStack stack = targetInventory != null ? targetInventory.decrStackSize(slot, amount) : null;
		polling = false;
		return stack;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slot) {

		if (checkPollingAndNeighbor()) {
			return null;
		}
		polling = true;
		ItemStack stack = targetInventory != null ? targetInventory.getStackInSlotOnClosing(slot) : null;
		polling = false;
		return stack;
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) {

		if (checkPollingAndNeighbor()) {
			return;
		}
		polling = true;
		if (targetInventory != null) {
			targetInventory.setInventorySlotContents(slot, stack);
		}
		polling = false;
	}

	@Override
	public int getInventoryStackLimit() {

		if (checkPollingAndNeighbor()) {
			return 64;
		}
		polling = true;
		int num = targetInventory != null ? targetInventory.getInventoryStackLimit() : 64;
		polling = false;
		return num;
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {

		if (checkPollingAndNeighbor()) {
			return false;
		}
		polling = true;
		boolean valid = targetInventory != null ? targetInventory.isItemValidForSlot(slot, stack) : false;
		polling = false;
		return valid;
	}

	/* ISidedInventory */
	@Override
	public int[] getAccessibleSlotsFromSide(int side) {

		if (side == facing) {
			return CoFHProps.EMPTY_INVENTORY;
		}
		if (checkPollingAndNeighbor()) {
			return CoFHProps.EMPTY_INVENTORY;
		}
		polling = true;
		int[] slots;
		if (targetInventorySided != null) {
			slots = targetInventorySided.getAccessibleSlotsFromSide(side);
		} else if (targetInventory != null) {
			slots = getNonSidedSlots(targetInventory.getSizeInventory());
		} else {
			slots = CoFHProps.EMPTY_INVENTORY;
		}
		polling = false;
		return slots;
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, int side) {

		if (side == facing) {
			return false;
		}
		if (checkPollingAndNeighbor()) {
			return false;
		}
		polling = true;
		boolean canInsert = targetInventorySided != null ? targetInventorySided.canInsertItem(slot, stack, side) : targetInventory != null;
		polling = false;
		return canInsert;
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int side) {

		if (side == facing) {
			return false;
		}
		if (checkPollingAndNeighbor()) {
			return false;
		}
		polling = true;
		boolean canExtract = targetInventorySided != null ? targetInventorySided.canExtractItem(slot, stack, side) : targetInventory != null;
		polling = false;
		return canExtract;
	}

}
