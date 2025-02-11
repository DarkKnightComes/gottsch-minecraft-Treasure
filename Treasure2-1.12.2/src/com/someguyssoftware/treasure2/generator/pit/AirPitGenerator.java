package com.someguyssoftware.treasure2.generator.pit;

import java.util.Random;

import com.someguyssoftware.gottschcore.generator.IGeneratorResult;
import com.someguyssoftware.gottschcore.positional.ICoords;
import com.someguyssoftware.gottschcore.random.RandomWeightedCollection;
import com.someguyssoftware.treasure2.Treasure;
import com.someguyssoftware.treasure2.generator.GenUtil;
import com.someguyssoftware.treasure2.generator.ITreasureGeneratorResult;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;


/**
 * 
 * @author Mark Gottschling
 *
 */
public class AirPitGenerator extends AbstractPitGenerator {

	/**
	 * 
	 */
	public AirPitGenerator() {
		getBlockLayers().add(50, Blocks.AIR);
	}
	
	/**
	 * 
	 * @param world
	 * @param random
	 * @param surfaceCoords
	 * @param spawnCoords
	 * @return
	 */
	public ITreasureGeneratorResult generate(World world, Random random, ICoords surfaceCoords, ICoords spawnCoords) {
		ITreasureGeneratorResult result =super.generate(world, random, surfaceCoords, spawnCoords); 
		if (result.isSuccess()) {
			Treasure.logger.debug("Generated Air Pit at " + spawnCoords.toShortString());
		}
		return result;
	}
}
