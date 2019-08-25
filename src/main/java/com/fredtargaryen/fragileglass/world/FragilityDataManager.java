package com.fredtargaryen.fragileglass.world;

import com.fredtargaryen.fragileglass.DataReference;
import com.fredtargaryen.fragileglass.FragileGlassBase;
import com.fredtargaryen.fragileglass.tileentity.capability.IFragileCapability;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.HashMap;

import static com.fredtargaryen.fragileglass.world.FragilityDataManager.FragileBehaviour.*;

/**
 * Responsible for everything to do with block fragility data from fragileglassft_blocks.cfg.
 */
public class FragilityDataManager {
    private static FragilityDataManager INSTANCE;

    private File configDir;
    private File configFile;

    private HashMap<String, FragilityData> tileEntityData;
    private HashMap<IBlockState, FragilityData> blockStateData;

    public enum FragileBehaviour {
        //Break if above the break speed
        BREAK,
        //Update after the update delay if above the break speed
        UPDATE,
        //Change to a different BlockState
        CHANGE,
        //Change to an EntityFallingBlock of the given BlockState
        FALL,
        //Load the data but don't even construct the capability; let another mod deal with it all
        MOD
    }

    public static FragilityDataManager getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new FragilityDataManager();
        }
        return INSTANCE;
    }

    public FragilityDataManager() {
        this.tileEntityData = new HashMap<>();
        this.blockStateData = new HashMap<>();
    }

    public void addCapabilityIfPossible(TileEntity te, AttachCapabilitiesEvent<TileEntity> evt) {
        FragilityData fragData = this.getTileEntityFragilityData(te);
        if (fragData != null) {
            FragileBehaviour fb = fragData.getBehaviour();
            //If MOD, a mod will define the capability at some point, so ignore
            if (fb != FragileBehaviour.MOD) {
                //If one of the other behaviours, and the capability has been defined, must ignore.
                if(!evt.getCapabilities().containsKey(DataReference.FRAGILE_CAP_LOCATION)) {
                    if (fb == FragileBehaviour.BREAK) {
                        ICapabilityProvider iCapProv = new ICapabilityProvider() {
                            IFragileCapability inst = new IFragileCapability() {
                                @Override
                                public void onCrash(IBlockState state, TileEntity te, Entity crasher, double speed) {
                                    if (speed > fragData.getBreakSpeed()) {
                                        te.getWorld().destroyBlock(te.getPos(), true);
                                    }
                                }
                            };

                            @Override
                            public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
                                return capability == FragileGlassBase.FRAGILECAP;
                            }

                            @Nullable
                            @Override
                            public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
                                return capability == FragileGlassBase.FRAGILECAP ? FragileGlassBase.FRAGILECAP.<T>cast(inst) : null;
                            }
                        };
                        evt.addCapability(DataReference.FRAGILE_CAP_LOCATION, iCapProv);
                    } else if (fb == FragileBehaviour.UPDATE) {
                        ICapabilityProvider iCapProv = new ICapabilityProvider() {
                            IFragileCapability inst = new IFragileCapability() {
                                @Override
                                public void onCrash(IBlockState state, TileEntity te, Entity crasher, double speed) {
                                    if (speed > fragData.getBreakSpeed()) {
                                        World w = te.getWorld();
                                        BlockPos tilePos = te.getPos();
                                        w.scheduleUpdate(tilePos, w.getBlockState(tilePos).getBlock(), fragData.getUpdateDelay());
                                    }
                                }
                            };

                            @Override
                            public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
                                return capability == FragileGlassBase.FRAGILECAP;
                            }

                            @Nullable
                            @Override
                            public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
                                return capability == FragileGlassBase.FRAGILECAP ? FragileGlassBase.FRAGILECAP.<T>cast(inst) : null;
                            }
                        };
                        evt.addCapability(DataReference.FRAGILE_CAP_LOCATION, iCapProv);
                    } else if (fb == CHANGE) {
                        ICapabilityProvider iCapProv = new ICapabilityProvider() {
                            IFragileCapability inst = new IFragileCapability() {
                                @Override
                                public void onCrash(IBlockState state, TileEntity te, Entity crasher, double speed) {
                                    if (speed > fragData.getBreakSpeed()) {
                                        te.getWorld().setBlockState(te.getPos(), fragData.getNewBlockState());
                                    }
                                }
                            };

                            @Override
                            public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
                                return capability == FragileGlassBase.FRAGILECAP;
                            }

                            @Nullable
                            @Override
                            public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
                                return capability == FragileGlassBase.FRAGILECAP ? FragileGlassBase.FRAGILECAP.<T>cast(inst) : null;
                            }
                        };
                        evt.addCapability(DataReference.FRAGILE_CAP_LOCATION, iCapProv);
                    } else if (fb == FALL) {
                        ICapabilityProvider iCapProv = new ICapabilityProvider() {
                            IFragileCapability inst = new IFragileCapability() {
                                @Override
                                public void onCrash(IBlockState state, TileEntity te, Entity crasher, double speed) {
                                    if (speed > fragData.getBreakSpeed()) {
                                        World w = te.getWorld();
                                        BlockPos pos = te.getPos();
                                        if (BlockFalling.canFallThrough(w.getBlockState(pos.down()))) {
                                            EntityFallingBlock fallingBlock = new EntityFallingBlock(w, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, state);
                                            fallingBlock.tileEntityData = te.writeToNBT(new NBTTagCompound());
                                            w.spawnEntity(fallingBlock);
                                        }
                                    }
                                }
                            };

                            @Override
                            public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
                                return capability == FragileGlassBase.FRAGILECAP;
                            }

                            @Nullable
                            @Override
                            public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
                                return capability == FragileGlassBase.FRAGILECAP ? FragileGlassBase.FRAGILECAP.<T>cast(inst) : null;
                            }
                        };
                        evt.addCapability(DataReference.FRAGILE_CAP_LOCATION, iCapProv);
                    }
                }
            }
        }
    }

    public FragilityData getBlockStateFragilityData(IBlockState state) {
        if(this.blockStateData.containsKey(state)) {
            return this.blockStateData.get(state);
        }
        return null;
    }

    public FragilityData getTileEntityFragilityData(TileEntity te) {
        //Consider me the ambassador for using "clarse" instead of "clazz"
        Class<? extends TileEntity> clarse = te.getClass();
        //Use the tile entity's class in the TileEntityRegistry to get its ResourceLocation
        ResourceLocation resourceLocation = TileEntity.getKey(clarse);
        if(resourceLocation != null) {
            //The TileEntity is in the registry
            String resourceLocationString = resourceLocation.toString();
            //Check the ResourceLocation string is in the manager, i.e. if the cfg was valid, it was in the cfg
            if (this.tileEntityData.containsKey(resourceLocationString)) {
                //If the cfg was valid and the string was in the cfg, there must be fragility data
                return this.tileEntityData.get(resourceLocationString);
            }
        }
        return null;
    }

    private void handleConfigFileException(Exception e) {
        FMLLog.bigWarning("Could not load "+DataReference.MODID+"_blocks.cfg! " +
                "Default block behaviour will be loaded. No custom data will take effect.");
        e.printStackTrace();
        this.loadDefaultData();
    }

    public boolean hasBlockStateFragilityData() {
        return !this.blockStateData.isEmpty();
    }

    public boolean hasTileEntityFragilityData() {
        return !this.tileEntityData.isEmpty();
    }

    public boolean isResourceLocationValidBlock(String resourceLocation) {
        return ForgeRegistries.BLOCKS.containsKey(new ResourceLocation(resourceLocation));
    }

    private void loadDefaultData() {
        this.blockStateData.clear();
        this.tileEntityData.clear();
        this.tileEntityData.put(DataReference.MODID + ":tefg", new FragilityData(BREAK, 0.165, 0, Blocks.AIR.getDefaultState(), new String[]{}));
        this.blockStateData.put(FragileGlassBase.thinIce.getDefaultState(), new FragilityData(BREAK, 0.0, 0, Blocks.AIR.getDefaultState(), new String[]{}));
        this.tileEntityData.put(DataReference.MODID + ":tews", new FragilityData(UPDATE, 0.0, 10, Blocks.AIR.getDefaultState(), new String[]{}));
    }

    /**
     * Set up to read fragileglassft_blocks.cfg. MUST be called in postInit, when all Blocks and Tile Entities have
     * been created!
     */
    public void loadBlockData() {
        try {
            FragilityConfigLoader fcl = new FragilityConfigLoader(this, this.blockStateData, this.tileEntityData);
            File[] fileList = this.configDir.listFiles();
            if(fileList != null) {
                String fileName = this.configFile.getName();
                System.out.println("Found file " + fileName + "; now loading");
                BufferedReader br = new BufferedReader(new FileReader(this.configFile));
                fcl.loadFile(br, this.configDir, fileName);
                br.close();
                for(File file : fileList) {
                    fileName = file.getName();
                    String[] fileNameParts = fileName.split("_");
                    if(fileNameParts.length == 3) {
                        System.out.println("Found file "+fileName+"; now loading");
                        if(fileNameParts[0].equals(DataReference.MODID) && fileNameParts[1].equals("blocks")) {
                            br = new BufferedReader(new FileReader(file));
                            fcl.loadFile(br, this.configDir, fileName);
                            br.close();
                        }
                    }
                }
            }
        }
        catch(IOException ioe) {
            this.handleConfigFileException(new Exception());
        }
    }

    public void setupDirsAndFiles(File configDir) {
        this.configDir = configDir;
        this.configFile = new File(this.configDir, DataReference.MODID + "_blocks.cfg");
        if(!this.configFile.exists()) {
            try {
                //Config file is not in config folder! Write from defaultFileData (see bottom of file)
                FMLLog.log.warn("[FRAGILITY CONFIG] No config file found! Writing a new one.");
                FileWriter fw = new FileWriter(this.configFile);
                for(String s : defaultFileData) {
                    fw.write(s);
                }
                fw.close();
            }
            catch(IOException ioe) {
                this.handleConfigFileException(ioe);
            }
        }
    }

    //Doesn't look like I can read from assets so sadly all this is needed for now
    private static final String[] defaultFileData = new String[] {
            "#################################################\n",
            "#FRAGILE GLASS AND THIN ICE CONFIG FILE - BLOCKS#\n",
            "#################################################\n",
            "#THINK VERY CAREFULLY AND BACK UP YOUR WORLDS BEFORE ADDING ENTRIES HERE!\n",
            "#(You probably don't really want to make ALL DIRT BLOCKS fragile, for example.)\n",
            "#Here is where you can configure which blocks are fragile and which are not, and modify basic behaviour.\n",
            "\n#--Limitations--\n",
            "#* This will not work for blocks which are basically air blocks, e.g. Air blocks and 'logic' blocks.\n",
            "#* If you specify block states you should be as specific as possible; if you leave out a property it\n",
            "#  will only work for blocks with the properties you specified, and the default for everything else.\n",
            "#* If your entry is not for a valid block, the mod will assume you entered a tile entity. However it\n",
            "#  cannot check if tile entities are valid, so you won't be warned. Check your spellings carefully.\n",
            "\n#--How to customise--\n",
            "#To add a comment to the file, start the line with a # symbol.\n",
            "#To make a block fragile, add a new row in this file following this format:\n",
            "#<modid>:<ID>[properties] <BREAK/UPDATE/CHANGE/FALL/MOD> <min speed> <update delay/new state> <extra values>\n",
            "#* 'modid:ID' is the ResourceLocation string used to register with Forge.\n",
            "#  - 'modid' can be found by looking in the 'modid' entry of the mod's mcmod.info file.\n",
            "#    For vanilla Minecraft this is just 'minecraft'.\n",
            "#  - For blocks WITH tile entities, 'ID' is the name used to register the Tile Entity with Forge.\n",
            "#    You can find these by searching for 'GameRegistry.registerTileEntity' in the mod's source code...\n",
            "#    or by asking the developer. These are easy to guess in vanilla Minecraft.\n",
            "#  - For blocks WITHOUT tile entities you need the block's registry name. You can usually find this by\n",
            "#    looking at the block in-game with the F3 menu on - below it are the blockstate properties.\n",
            "#    > Only add the properties if you are specifying behaviour for specific blockstates.\n",
            "#      Not all properties need to be specified; see the door example below.\n",
            "#* You must choose one of 'BREAK', 'UPDATE', 'CHANGE', 'FALL' or 'MOD'; the block will have one of the\n",
            "#  following 'crash behaviours':\n",
            "#  - For all crash behaviours, the 'breaker' entity must be travelling above its minimum speed. If so,\n",
            "#    it must then be above the speed defined for the block. Meeting both these conditions causes the\n",
            "#    crash behaviour to trigger.\n",
            "#  - 'BREAK': the block breaks immediately.\n",
            "#  - 'UPDATE': a block update is triggered.\n",
            "#  - 'CHANGE': the block changes into a specified blockstate.\n",
            "#  - 'FALL': the block falls immediately.\n",
            "#  - 'MOD': for mod tile entities with custom behaviours ONLY. Modders should make custom tile\n",
            "#           entities and implement IFragileCapability with the behaviour they want. This mod loads all\n",
            "#           the extra values and it is up to the modder how they are used. NOTE: If a tile entity has a\n",
            "#           custom behaviour it will be used regardless of the behaviour value.\n",
            "#* The first number is a minimum speed (must be decimal). The breaker must be moving above their\n",
            "#  breaking speed, AND above this speed, to trigger the crash behaviour. Speed is measured in blocks\n",
            "#  per tick, which is metres per second divided by 20.\n",
            "#* The second number is only used by the UPDATE behaviour. It must be an integer. It specifies the\n",
            "#  delay between the collision and the block update. Delays are measured in ticks and there are 20\n",
            "#  ticks per second.\n",
            "#* The value after the second number is only used by the CHANGE behaviour. It must be a blockstate\n",
            "#  (same format as the first value in each line). This is the state the block will change into. If you\n",
            "#  aren't using this value leave a - here.\n",
            "#* You can add extra values of any format, separated by spaces, for any mod blocks that might require\n",
            "#  them.\n",
            "\n#--Fun example lines you may wish to uncomment--\n",
            "#Make vanilla glass and ice fragile too\n",
            "#minecraft:ice BREAK 0.165 0 -\n",
            "#minecraft:glass BREAK 0.165 0 -\n",
            "#minecraft:glass_pane BREAK 0.165 0 -\n",
            "#minecraft:stained_glass BREAK 0.165 0 -\n",
            "#minecraft:stained_glass_pane BREAK 0.165 0 -\n",
            "#Make obsidian as fragile as it is IRL\n",
            "#minecraft:obsidian BREAK 0.165 0 -\n",
            "#Weak sandstone\n",
            "#minecraft:sandstone FALL 0.0 0 minecraft:sandstone\n",
            "#minecraft:red_sandstone FALL 0.0 0 minecraft:red_sandstone\n",
            "#Burst through doors when sprinting into them\n",
            "#minecraft:wooden_door[open=false] CHANGE 0.165 0 minecraft:wooden_door[open=true]\n",
            "#minecraft:birch_door[open=false] CHANGE 0.165 0 minecraft:birch_door[open=true]\n",
            "#minecraft:acacia_door[open=false] CHANGE 0.165 0 minecraft:acacia_door[open=true]\n",
            "#minecraft:spruce_door[open=false] CHANGE 0.165 0 minecraft:spruce_door[open=true]\n",
            "#minecraft:jungle_door[open=false] CHANGE 0.165 0 minecraft:jungle_door[open=true]\n",
            "#minecraft:dark_oak_door[open=false] CHANGE 0.165 0 minecraft:dark_oak_door[open=true]\n",
            "#Cause suspended sand to fall when you are near it\n",
            "#minecraft:sand UPDATE 0.0 10 -\n",
            "#Safe lava that turns into slime at the last minute\n",
            "#minecraft:lava CHANGE 0.0 0 minecraft:slime\n",
            "#\n#--Default values, in case you break something--\n",
            "#All fragile glass blocks:\n",
            "#fragileglassft:tefg BREAK 0.165 0 -\n",
            "#Thin ice:\n",
            "#fragileglassft:thinice BREAK 0.0 0 -\n",
            "#Weak stone:\n",
            "#fragileglassft:tews UPDATE 0.0 10 -\n\n",
            "fragileglassft:tefg BREAK 0.165 0 -\n",
            "fragileglassft:thinice BREAK 0.0 0 -\n",
            "fragileglassft:tews UPDATE 0.0 10 -\n"
    };
}
