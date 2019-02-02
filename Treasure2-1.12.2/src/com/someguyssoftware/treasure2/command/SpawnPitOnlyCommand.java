/**
 * 
 */
package com.someguyssoftware.treasure2.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.someguyssoftware.gottschcore.positional.Coords;
import com.someguyssoftware.gottschcore.positional.ICoords;
import com.someguyssoftware.gottschcore.world.WorldInfo;
import com.someguyssoftware.treasure2.Treasure;
import com.someguyssoftware.treasure2.enums.Pits;
import com.someguyssoftware.treasure2.generator.pit.IPitGenerator;
import com.someguyssoftware.treasure2.generator.pit.StructurePitGenerator;
import com.someguyssoftware.treasure2.world.gen.structure.IStructureInfoProvider;
import com.someguyssoftware.treasure2.worldgen.ChestWorldGenerator;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * 
 * @author Mark Gottschling on Mar 8, 2018
 *
 */
public class SpawnPitOnlyCommand extends CommandBase {

	@Override
	public String getName() {
		return "trpitonly";
	}

	@Override
	public String getUsage(ICommandSender var1) {
		return "/trpitonly <x> <y> <z> [pit]: spawns a named Treasure! pit at location (x,y,z)";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender commandSender, String[] args) {
		EntityPlayer player = (EntityPlayer) commandSender.getCommandSenderEntity();
		try {

			int x, y, z = 0;
			x = Integer.parseInt(args[0]);
			y = Integer.parseInt(args[1]);
			z = Integer.parseInt(args[2]);
			
			String pitName = "";
			if (args.length > 3) {
				pitName = args[3];
			}
			
			if (pitName.equals("")) pitName = Pits.SIMPLE_PIT.name();
			Pits pit = Pits.valueOf(pitName.toUpperCase());
			Treasure.logger.debug("Pit:" + pit + "; " + pit.ordinal());
			
			if (player != null) {
    			World world = commandSender.getEntityWorld();
    			Treasure.logger.debug("Starting to build Treasure! pit only ...");

    			Random random = new Random();
    			ICoords spawnCoords = new Coords(x, y, z);
    			ICoords surfaceCoords = WorldInfo.getDryLandSurfaceCoords(world, new Coords(x, WorldInfo.getHeightValue(world, spawnCoords), z));
    			Treasure.logger.debug("spawn coords @ {}", spawnCoords.toShortString());
    			Treasure.logger.debug("surfaceCoords @ {}", surfaceCoords.toShortString());
    			ChestWorldGenerator chestGen = new ChestWorldGenerator();
//    			IPitGenerator pitGen = chestGen.pitGenerators.get(pit);
    			IPitGenerator pitGen = null;
    			if (pit == Pits.STRUCTURE_PIT) {
    				// select a pit from the subset
    				List<IPitGenerator> pits = new ArrayList<IPitGenerator>(ChestWorldGenerator.structurePitGenerators.values());
    				IPitGenerator parentPit = pits.get(random.nextInt(pits.size()));
    				// create a new pit instance (new instance as it contains state)
    				pitGen = new StructurePitGenerator(parentPit);
    			}
    			else {
    				pitGen = ChestWorldGenerator.pitGenerators.get(pit);
    			}    			
    			boolean isGen = pitGen.generate(world, random, surfaceCoords , spawnCoords);
    			if (isGen && pit == Pits.STRUCTURE_PIT) {
    				Treasure.logger.debug(((IStructureInfoProvider)pitGen).getInfo());
    			}
    		}
		}
		catch(Exception e) {
			player.sendMessage(new TextComponentString("Error:  " + e.getMessage()));
			Treasure.logger.error("Error generating Treasure! pit:", e);
			e.printStackTrace();
		}
	}
}
