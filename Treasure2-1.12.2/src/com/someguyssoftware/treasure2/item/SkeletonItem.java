/**
 * 
 */
package com.someguyssoftware.treasure2.item;

import com.someguyssoftware.gottschcore.item.ModItem;
import com.someguyssoftware.gottschcore.world.WorldInfo;
import com.someguyssoftware.treasure2.Treasure;
import com.someguyssoftware.treasure2.block.SkeletonBlock;
import com.someguyssoftware.treasure2.block.TreasureBlocks;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * @author Mark Gottschling on Feb 2, 2019
 *
 */
public class SkeletonItem extends ModItem {
	public static final int MAX_STACK_SIZE = 1;

	/**
	 * 
	 */
	public SkeletonItem(String modID, String name) {
		super();
		this.setItemName(modID, name);
		this.setMaxStackSize(MAX_STACK_SIZE);
		this.setCreativeTab(Treasure.TREASURE_TAB);
	}

	/**
	 * Called when a Block is right-clicked with this Item
	 */
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos posIn, EnumHand hand, EnumFacing facingIn, float hitX, float hitY, float hitZ) {
		if (WorldInfo.isClientSide(worldIn)) {
			return EnumActionResult.SUCCESS;
		} 
		else if (facingIn != EnumFacing.UP) {
			Treasure.logger.debug("Skeleton item: facing up... fail.");
			return EnumActionResult.FAIL;
		} 
		else {
			IBlockState blockState = worldIn.getBlockState(posIn);
			Block block = blockState.getBlock();
			boolean flag = block.isReplaceable(worldIn, posIn);

			if (!flag) {
				posIn = posIn.up();
			}

			int i = MathHelper.floor((double) (player.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
			EnumFacing facing = EnumFacing.getHorizontal(i);
			BlockPos pos = posIn.offset(facing);
			ItemStack itemStack = player.getHeldItem(hand);
			Treasure.logger.debug("Can edit 1 -> {} @ {}", player.canPlayerEdit(posIn, facingIn, itemStack), posIn);
			Treasure.logger.debug("Can edit 2 -> {} @ {}", player.canPlayerEdit(pos, facingIn, itemStack), pos);
			if (player.canPlayerEdit(posIn, facingIn, itemStack) && player.canPlayerEdit(pos, facingIn, itemStack)) {
				Treasure.logger.debug("Can edit block -> {}", pos);
				IBlockState state2 = worldIn.getBlockState(pos);
				boolean flag1 = state2.getBlock().isReplaceable(worldIn, pos);
				boolean flag2 = flag || worldIn.isAirBlock(posIn);
				boolean flag3 = flag1 || worldIn.isAirBlock(pos);
				Treasure.logger.debug("Flag1 -> {}", flag1);
				Treasure.logger.debug("Flag2 -> {}", flag2);
				Treasure.logger.debug("Flag3 -> {}", flag3);
				if (flag2 && flag3 && worldIn.getBlockState(posIn.down()).isTopSolid() && worldIn.getBlockState(pos.down()).isTopSolid()) {
					IBlockState skeletonState = TreasureBlocks.SKELETON.getDefaultState().withProperty(BlockBed.FACING, facing)
							.withProperty(SkeletonBlock.PART, SkeletonBlock.EnumPartType.BOTTOM);
					
					worldIn.setBlockState(posIn, skeletonState, 10);
					worldIn.setBlockState(pos, skeletonState.withProperty(BlockBed.PART, BlockBed.EnumPartType.HEAD), 10);
					SoundType soundtype = skeletonState.getBlock().getSoundType(skeletonState, worldIn, posIn, player);
					worldIn.playSound((EntityPlayer) null, posIn, soundtype.getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);

					worldIn.notifyNeighborsRespectDebug(posIn, block, false);
					worldIn.notifyNeighborsRespectDebug(pos, state2.getBlock(), false);

					if (player instanceof EntityPlayerMP) {
						CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, posIn, itemStack);
					}
					itemStack.shrink(1);
					return EnumActionResult.SUCCESS;
				} else {
					return EnumActionResult.FAIL;
				}
			} else {
				return EnumActionResult.FAIL;
			}
		}
	}

}
