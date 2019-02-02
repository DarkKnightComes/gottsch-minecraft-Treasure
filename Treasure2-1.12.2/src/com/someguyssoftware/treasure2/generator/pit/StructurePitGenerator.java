package com.someguyssoftware.treasure2.generator.pit;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.someguyssoftware.gottschcore.Quantity;
import com.someguyssoftware.gottschcore.positional.Coords;
import com.someguyssoftware.gottschcore.positional.ICoords;
import com.someguyssoftware.treasure2.Treasure;
import com.someguyssoftware.treasure2.block.TreasureBlocks;
import com.someguyssoftware.treasure2.enums.Rarity;
import com.someguyssoftware.treasure2.enums.StructureMarkers;
import com.someguyssoftware.treasure2.generator.GenUtil;
import com.someguyssoftware.treasure2.generator.structure.StructureGenerator;
import com.someguyssoftware.treasure2.tileentity.ProximitySpawnerTileEntity;
import com.someguyssoftware.treasure2.world.gen.structure.IStructureInfo;
import com.someguyssoftware.treasure2.world.gen.structure.IStructureInfoProvider;
import com.someguyssoftware.treasure2.world.gen.structure.StructureInfo;
import com.someguyssoftware.treasure2.world.gen.structure.TreasureTemplate;
import com.someguyssoftware.treasure2.world.gen.structure.TreasureTemplateManager;
import com.someguyssoftware.treasure2.world.gen.structure.TreasureTemplateManager.StructureType;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.common.DungeonHooks;


/**
 * Generates lava blocks outside the main pit to prevent players from digging down on the edges
 * @author Mark Gottschling on Dec 9, 2018
 *
 */
public class StructurePitGenerator extends AbstractPitGenerator implements IStructureInfoProvider {
	
	IPitGenerator generator;
	IStructureInfo info;
	
	/**
	 * 
	 */
	public StructurePitGenerator() {
		getBlockLayers().add(50, Blocks.AIR);
		getBlockLayers().add(25,  Blocks.SAND);
		getBlockLayers().add(15, Blocks.GRAVEL);
		getBlockLayers().add(10, Blocks.LOG);
	}
	
	/**
	 * 
	 * @param generator
	 */
	public StructurePitGenerator(IPitGenerator generator) {
		this();
		setGenerator(generator);
		Treasure.logger.debug("using parent generator -> {}", generator.getClass().getSimpleName());
	}
	
	@Override
	public boolean generateEntrance(World world, Random random, ICoords surfaceCoords, ICoords spawnCoords) {
		return getGenerator().generateEntrance(world, random, surfaceCoords, spawnCoords);
	}
	
	@Override
	public boolean generatePit(World world, Random random, ICoords surfaceCoords, ICoords spawnCoords) {
		getGenerator().setOffsetY(0);
		return getGenerator().generatePit(world, random, surfaceCoords, spawnCoords);
	}
	
	/**
	 * 
	 * @param world
	 * @param random
	 * @param surfaceCoords
	 * @param spawnCoords
	 * @return
	 */
	@Override
	public boolean generate(World world, Random random, ICoords surfaceCoords, ICoords spawnCoords) {
		// is the chest placed in a cavern
		boolean inCavern = false;
		
		// check above if there is a free space - chest may have spawned in underground cavern, ravine, dungeon etc
		IBlockState blockState = world.getBlockState(spawnCoords.add(0, 1, 0).toPos());
		
		// if there is air above the origin, then in cavern. (pos in isAir() doesn't matter)
		if (blockState == null || blockState.getMaterial() == Material.AIR) {
			Treasure.logger.debug("Spawn coords is in cavern.");
			inCavern = true;
		}
		
		if (inCavern) {
			Treasure.logger.debug("Shaft is in cavern... finding ceiling.");
			spawnCoords = GenUtil.findUndergroundCeiling(world, spawnCoords.add(0, 1, 0));
			if (spawnCoords == null) {
				Treasure.logger.warn("Exiting: Unable to locate cavern ceiling.");
				return false;
			}
		}
	
		// get distance to surface
		int yDist = (surfaceCoords.getY() - spawnCoords.getY()) - 2;
//		Treasure.logger.debug("Distance to ySurface =" + yDist);
	
		IStructureInfo info = null;
		
		if (yDist > 6) {
			Treasure.logger.debug("generating structure room at -> {}", spawnCoords.toShortString());
			
			// TODO will want the structures organized better to say grab RARE UNDERGROUND ROOMs
			// select a random underground structure
//			List<Template> templates = Treasure.TEMPLATE_MANAGER.getTemplates().values().stream().collect(Collectors.toList());
			List<Template> templates = Treasure.TEMPLATE_MANAGER.getTemplatesByType(StructureType.UNDERGROUND);
			
			TreasureTemplate template = (TreasureTemplate) templates.get(random.nextInt(templates.size()));
			if (template == null) {
				Treasure.logger.debug("could not find random template");
				return false;
			}
			
//			List<Template> templates2 = Treasure.TEMPLATE_MANAGER.getTemplateTable()
//					.get(TreasureTemplateManager.StructureType.UNDERGROUND, Rarity.COMMON);
			
			// find the offset block
			int offset = 0;
			ICoords offsetCoords = template.findCoords(random, GenUtil.getMarkerBlock(StructureMarkers.OFFSET));
			if (offsetCoords != null) {
				offset = -offsetCoords.getY();
			}
			
			// check if the yDist is big enough to accodate a room
			BlockPos size = template.getSize();
//			Treasure.logger.debug("template size -> {}, offset -> {}", size, offset);
			
			// if size of room is greater the distance to the surface minus 3, then fail 
			if (size.getY() + offset + 3 >= yDist) {
				Treasure.logger.debug("Structure is too large for available space.");
				return getGenerator().generate(world, random, surfaceCoords, spawnCoords);
			}

			// update the spawn coords with the offset
			spawnCoords = spawnCoords.add(0, offset, 0);
	
			// find the entrance block
			ICoords entranceCoords = template.findCoords(random, GenUtil.getMarkerBlock(StructureMarkers.ENTRANCE));
			if (entranceCoords == null) {
				Treasure.logger.debug("Unable to locate entrance position.");
				return false;
			}
			
			// select a random rotation
			Rotation rotation = Rotation.values()[random.nextInt(Rotation.values().length)];
			Treasure.logger.debug("rotation used -> {}", rotation);
			
			// setup placement
			PlacementSettings placement = new PlacementSettings();
			placement.setRotation(rotation).setRandom(random);
			
			// NOTE these values are still relative to origin (spawnCoords);
			ICoords newEntrance = new Coords(TreasureTemplate.transformedBlockPos(placement, entranceCoords.toPos()));
		
			/*
			 *  adjust spawn coords to line up room entrance with pit
			 */
			BlockPos transformedSize = template.transformedSize(rotation);
			ICoords roomCoords = alignToPit(spawnCoords, newEntrance, transformedSize, placement);
//			Treasure.logger.debug("aligned room coords -> {}", roomCoords.toShortString());
			
			// generate the structure
			info = new StructureGenerator().generate(world, random, template, placement, roomCoords);
			if (info == null) return false;			
			Treasure.logger.debug("returned info -> {}", info);
			setInfo(info);
			
			// interrogate info for spawners and any other special block processing (except chests that are handler by caller
			List<ICoords> spawnerCoords = (List<ICoords>) info.getMap().get(GenUtil.getMarkerBlock(StructureMarkers.SPAWNER));
			List<ICoords> proximityCoords = (List<ICoords>) info.getMap().get(GenUtil.getMarkerBlock(StructureMarkers.PROXIMITY_SPAWNER));

			/*
			 *  TODO could lookup to some sort of map of structure -> spawner info
			 *  ex.	uses a Guava Table:
			 *  		map.put(ResourceLocation("treasure2:underground/basic1", SPAWNER, new SpawnerInfo("minecraft:Spider"));
			 *  		map.put(ResourceLocation("treasure2:underground/basic1", PROXIMITY, new SpawnerInfo("minecraft:Spider", new Quantity(1,2), 5D));
			 */
			
			// populate vanilla spawners
			for (ICoords c : spawnerCoords) {
				ICoords c2 = roomCoords.add(c);
				world.setBlockState(c2.toPos(), Blocks.MOB_SPAWNER.getDefaultState());
				TileEntityMobSpawner te = (TileEntityMobSpawner) world.getTileEntity(c2.toPos());
				ResourceLocation r = DungeonHooks.getRandomDungeonMob(random);
				te.getSpawnerBaseLogic().setEntityId(r);
			}
			
			// populate proximity spawners
			for (ICoords c : proximityCoords) {
				ICoords c2 = roomCoords.add(c);
		    	world.setBlockState(c2.toPos(), TreasureBlocks.PROXIMITY_SPAWNER.getDefaultState());
		    	ProximitySpawnerTileEntity te = (ProximitySpawnerTileEntity) world.getTileEntity(c2.toPos());
		    	ResourceLocation r = DungeonHooks.getRandomDungeonMob(random);
		    	te.setMobName(r);
		    	te.setMobNum(new Quantity(1, 2));
		    	te.setProximity(5D);
			}
			
//			Treasure.logger.debug("generating shaft (top of room) @ " + spawnCoords.add(0, size.getY(),0).toShortString());
			
			// shaft enterance
			generateEntrance(world, random, surfaceCoords, spawnCoords.add(0, size.getY()+1, 0));
			
			// build the pit
			generatePit(world, random, surfaceCoords, spawnCoords.add(0, size.getY(), 0));
		}			
		// shaft is only 2-6 blocks long - can only support small covering
		else if (yDist >= 2) {
			// simple short pit
			new SimpleShortPitGenerator().generate(world, random, surfaceCoords, spawnCoords);
		}		
		Treasure.logger.debug("Generated Structure Pit at " + spawnCoords.toShortString());
		return true;
	}

	/**
	 * 
	 * @param spawnCoords
	 * @param newEntrance
	 * @param transformedSize
	 * @param placement
	 * @return
	 */
	private ICoords alignToPit(ICoords spawnCoords, ICoords newEntrance, BlockPos transformedSize, PlacementSettings placement) {
		ICoords startCoords = null;
		// NOTE work with rotations only for now
		
		// first offset spawnCoords by newEntrance
		startCoords = spawnCoords.add(-newEntrance.getX(), 0, -newEntrance.getZ());
		
		// make adjustments for the rotation. REMEMBER that pits are 2x2
		switch (placement.getRotation()) {
		case CLOCKWISE_90:
			startCoords = startCoords.add(1, 0, 0);
			break;
		case CLOCKWISE_180:
			startCoords = startCoords.add(1, 0, 1);
			break;
		case COUNTERCLOCKWISE_90:
			startCoords = startCoords.add(0, 0, 1);
			break;
		default:
			break;
		}
		return startCoords;
	}
	
	/**
	 * @return the generator
	 */
	public IPitGenerator getGenerator() {
		return generator;
	}

	/**
	 * @param generator the generator to set
	 */
	public void setGenerator(IPitGenerator generator) {
		this.generator = generator;
	}

	/**
	 * @return the info
	 */
	@Override
	public IStructureInfo getInfo() {
		return info;
	}

	/**
	 * @param info2 the info to set
	 */
	protected void setInfo(IStructureInfo info) {
		this.info = info;
	}

	@Override
	public String toString() {
		return "StructurePitGenerator [generator=" + generator + ", info=" + info + "]";
	}
	
}
