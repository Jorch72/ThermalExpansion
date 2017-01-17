package cofh.thermalexpansion.block.dynamo;

import cofh.api.tileentity.IRedstoneControl.ControlMode;
import cofh.core.item.ItemBlockCore;
import cofh.lib.util.helpers.*;
import cofh.thermalexpansion.util.ReconfigurableHelper;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.List;

public class ItemBlockDynamo extends ItemBlockCore {

	public static ItemStack setDefaultTag(ItemStack container) {

		ReconfigurableHelper.setFacing(container, 1);
		RedstoneControlHelper.setControl(container, ControlMode.DISABLED);
		EnergyHelper.setDefaultEnergyTag(container, 0);
		AugmentHelper.writeAugments(container, BlockDynamo.defaultAugments);

		return container;
	}

	public ItemBlockDynamo(Block block) {

		super(block);
		setHasSubtypes(true);
		setMaxDamage(0);
		setNoRepair();
	}

	@Override
	public String getUnlocalizedName(ItemStack item) {

		return "tile.thermalexpansion.dynamo." + BlockDynamo.NAMES[ItemHelper.getItemDamage(item)] + ".name";
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean check) {

		SecurityHelper.addOwnerInformation(stack, list);
		if (StringHelper.displayShiftForDetail && !StringHelper.isShiftKeyDown()) {
			list.add(StringHelper.shiftForDetails());
		}
		if (!StringHelper.isShiftKeyDown()) {
			return;
		}
		SecurityHelper.addAccessInformation(stack, list);

		list.add(StringHelper.localize("info.thermalexpansion.dynamo.generate"));
		list.add(StringHelper.getInfoText("info.thermalexpansion.dynamo." + BlockDynamo.NAMES[ItemHelper.getItemDamage(stack)]));

		if (ItemHelper.getItemDamage(stack) == BlockDynamo.Types.STEAM.ordinal()) {
			list.add(StringHelper.getNoticeText("info.thermalexpansion.dynamo.steam.0"));
		}
		RedstoneControlHelper.addRSControlInformation(stack, list);
	}

}
