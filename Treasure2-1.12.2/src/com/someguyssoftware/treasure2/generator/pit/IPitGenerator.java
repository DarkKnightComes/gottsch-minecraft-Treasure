package com.someguyssoftware.treasure2.generator.pit;

import java.util.Random;

import com.someguyssoftware.gottschcore.generator.IGeneratorResult;
import com.someguyssoftware.gottschcore.positional.ICoords;
import com.someguyssoftware.treasure2.generator.ITreasureGeneratorResult;

import net.minecraft.world.World;

public interface IPitGenerator {

	/**
	 * 
	 * @param world
	 * @param random
	 * @param surfaceCoords
	 * @param spawnCoords
	 * @return
	 */
	public ITreasureGeneratorResult generate(World world, Random random, ICoords surfaceCoords, ICoords spawnCoords);
	
	public boolean generateBase(World world, Random random, ICoords surfaceCoords, ICoords spawnCoords);

	public boolean generatePit(World world, Random random, ICoords surfaceCoords, ICoords spawnCoords);
	
	public boolean generateEntrance(World world, Random random, ICoords surfaceCoords, ICoords spawnCoords);

	public int getOffsetY();
	public void setOffsetY(int i);
}