package cofh.thermalexpansion.block.machine;

import cofh.lib.util.helpers.ItemHelper;
import cofh.thermalexpansion.ThermalExpansion;
import cofh.thermalexpansion.gui.client.machine.GuiFurnace;
import cofh.thermalexpansion.gui.container.machine.ContainerFurnace;
import cofh.thermalexpansion.util.crafting.FurnaceManager;
import cofh.thermalexpansion.util.crafting.FurnaceManager.RecipeFurnace;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class TileFurnace extends TileMachineBase {

	private static final int TYPE = BlockMachine.Type.FURNACE.getMetadata();

	public static void initialize() {

		defaultSideConfig[TYPE] = new SideConfig();
		defaultSideConfig[TYPE].numConfig = 4;
		defaultSideConfig[TYPE].slotGroups = new int[][] { {}, { 0 }, { 1 }, { 0, 1 } };
		defaultSideConfig[TYPE].allowInsertionSide = new boolean[] { false, true, false, true };
		defaultSideConfig[TYPE].allowExtractionSide = new boolean[] { false, true, true, true };
		defaultSideConfig[TYPE].allowInsertionSlot = new boolean[] { true, false, false };
		defaultSideConfig[TYPE].allowExtractionSlot = new boolean[] { true, true, false };
		defaultSideConfig[TYPE].sideTex = new int[] { 0, 1, 4, 7 };
		defaultSideConfig[TYPE].defaultSides = new byte[] { 1, 1, 2, 2, 2, 2 };

		GameRegistry.registerTileEntity(TileFurnace.class, "thermalexpansion:machine_furnace");

		config();
	}

	public static void config() {

		String category = "Machine.Furnace";
		BlockMachine.enable[TYPE] = ThermalExpansion.CONFIG.get(category, "Enable", true);

		defaultEnergyConfig[TYPE] = new EnergyConfig();
		defaultEnergyConfig[TYPE].setDefaultParams(20);
	}

	private int inputTracker;
	private int outputTracker;

	/* AUGMENTS */
	public boolean augmentFood;
	public boolean augmentOre;

	public TileFurnace() {

		super();
		inventory = new ItemStack[1 + 1 + 1];
	}

	@Override
	public int getType() {

		return TYPE;
	}

	@Override
	protected boolean canStart() {

		if (inventory[0] == null || energyStorage.getEnergyStored() <= 0) {
			return false;
		}
		if (augmentFood && !FurnaceManager.isFood(inventory[0]) || augmentOre && !FurnaceManager.isOre(inventory[0])) {
			return false;
		}
		RecipeFurnace recipe = FurnaceManager.getRecipe(inventory[0]);

		if (recipe == null) {
			return false;
		}
		ItemStack output = recipe.getOutput();

		return inventory[1] == null || inventory[1].isItemEqual(output) && inventory[1].stackSize + output.stackSize <= output.getMaxStackSize();
	}

	@Override
	protected boolean hasValidInput() {

		RecipeFurnace recipe = FurnaceManager.getRecipe(inventory[0]);

		if (augmentFood && !FurnaceManager.isFood(inventory[0]) || augmentOre && !FurnaceManager.isOre(inventory[0])) {
			return false;
		}
		return recipe != null && recipe.getInput().stackSize <= inventory[0].stackSize;
	}

	@Override
	protected void processStart() {

		processMax = FurnaceManager.getRecipe(inventory[0]).getEnergy() * energyMod / ENERGY_BASE;

		if (augmentOre) {
			processMax *= 2;
		}
		processRem = processMax;
	}

	@Override
	protected void processFinish() {

		RecipeFurnace recipe = FurnaceManager.getRecipe(inventory[0]);

		if (recipe == null) {
			isActive = false;
			wasActive = true;
			tracker.markTime(worldObj);
			processRem = 0;
			return;
		}
		ItemStack output = recipe.getOutput();
		if (inventory[1] == null) {
			inventory[1] = ItemHelper.cloneStack(output);
		} else {
			inventory[1].stackSize += output.stackSize;
		}
		if ((augmentFood && FurnaceManager.isFood(inventory[0]) || augmentOre && FurnaceManager.isOre(inventory[0])) && inventory[1].stackSize < inventory[1].getMaxStackSize()) {
			inventory[1].stackSize += output.stackSize;
		}
		inventory[0].stackSize -= recipe.getInput().stackSize;

		if (inventory[0].stackSize <= 0) {
			inventory[0] = null;
		}
	}

	@Override
	protected void transferInput() {

		if (!enableAutoInput) {
			return;
		}
		int side;
		for (int i = inputTracker + 1; i <= inputTracker + 6; i++) {
			side = i % 6;
			if (sideCache[side] == 1) {
				if (extractItem(0, ITEM_TRANSFER[level], EnumFacing.VALUES[side])) {
					inputTracker = side;
					break;
				}
			}
		}
	}

	@Override
	protected void transferOutput() {

		if (!enableAutoOutput) {
			return;
		}
		if (inventory[1] == null) {
			return;
		}
		int side;
		for (int i = outputTracker + 1; i <= outputTracker + 6; i++) {
			side = i % 6;
			if (sideCache[side] == 2) {
				if (transferItem(1, ITEM_TRANSFER[level], EnumFacing.VALUES[side])) {
					outputTracker = side;
					break;
				}
			}
		}
	}

	/* GUI METHODS */
	@Override
	public Object getGuiClient(InventoryPlayer inventory) {

		return new GuiFurnace(inventory, this);
	}

	@Override
	public Object getGuiServer(InventoryPlayer inventory) {

		return new ContainerFurnace(inventory, this);
	}

	/* NBT METHODS */
	@Override
	public void readFromNBT(NBTTagCompound nbt) {

		super.readFromNBT(nbt);

		inputTracker = nbt.getInteger("TrackIn");
		outputTracker = nbt.getInteger("TrackOut");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {

		super.writeToNBT(nbt);

		nbt.setInteger("TrackIn", inputTracker);
		nbt.setInteger("TrackOut", outputTracker);
		return nbt;
	}

	/* IInventory */
	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {

		return slot != 0 || (augmentFood ? FurnaceManager.isFood(stack) : augmentOre ? FurnaceManager.isOre(stack) : FurnaceManager.recipeExists(stack));
	}

}
