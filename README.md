# Fragile-Glass
Fragile Glass and Thin Ice is a Minecraft Forge mod which does the following things:
* Adds Fragile Glass (including stained and pane versions), made with sugar and water
* Adds Thin Ice which breaks underfoot unless stepped on carefully
* Adds Weak Stone (disabled by default) which crumbles into gravel on contact
* Adds configuration for practically any block (vanilla or any mod) so that it can break, update, change or fall down when something collides with it hard enough
* Adds configuration for how fast different entities have to be travelling to break a fragile block

This is for versions of the mod for Minecraft 1.8 upwards.
For Minecraft 1.7.10, see [the Fragile-Glass-1.7.10 repo](https://github.com/fredtargaryen/Fragile-Glass-1.7.10).

## Information for players
### Customising Fragile Blocks
Full information on how to customise blocks' behaviour on collision can be found in these places:
* At the bottom of [this file](https://github.com/fredtargaryen/Fragile-Glass/blob/master/src/main/java/com/fredtargaryen/fragileglass/world/FragilityDataManager.java).
* In your game folder, under `config/fragileglassft_blocks.cfg`.
### Customising Fragile Entities
Full information on how to customise entities' minimum and maximum collision speed can be found in these places:
* At the bottom of [this file](https://github.com/fredtargaryen/Fragile-Glass/blob/blocklist/src/main/java/com/fredtargaryen/fragileglass/world/BreakerDataManager.java).
* In your game folder, under `config/fragileglassft_entities.cfg`.
### Issues
Please report any issues on [the Issues page](https://github.com/fredtargaryen/Fragile-Glass/issues).

## Information for mod developers
### Compatibility with your mod
For your mod to work with Fragile Glass, add it as a dependency following the instructions [here](https://github.com/MinecraftForge/ForgeGradle/wiki/Dependencies). 

If you want the dependency to be optional, you can check if Fragile Glass was loaded with `Loader.isModLoaded("fragileglassft")`.

You can find the latest Fragile Glass files [here](https://minecraft.curseforge.com/projects/fragile-glass-and-thin-ice/files).

To make a tile entity with custom collision behaviour, add the capability like this:
```
@Mod.EventBusSubscriber // Needed for static event handler; can be non-static if you wish
public class YourBaseModClass {
    ...
    @SubscribeEvent
    public static void onTileEntityConstruct(AttachCapabilitiesEvent<TileEntity> evt) {
        TileEntity te = evt.getObject();
        if(te instanceof TileEntityYourFragileTileEntity) {
            ICapabilityProvider icp = new ICapabilityProvider() { // Can use a serializable version as long as you implement the interface
                IFragileCapability inst = new IFragileCapability() {
                    @Override
                    public void onCrash(IBlockState state, TileEntity te, Entity crasher, double speed) {
                        // Do whatever you like in here
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
            evt.addCapability(DataReference.FRAGILE_CAP_LOCATION, icp);
        }
    }
}
```
If you want to support custom fragility values from `fragileglassft_blocks.cfg`, if they were entered correctly you can access them like this:
```
FragilityData fragData = FragilityDataManager.getInstance().getTileEntityFragilityData(te);
FragileBehaviour fb = fragData.getBehaviour(); //If this is not set to MOD you won't be able to use your custom behaviour
double breakSpeed = fragData.getBreakSpeed();
int updateDelay = fragData.getUpdateDelay();
IBlockState state = fragData.getNewBlockState();
String[] extraData = fragData.getExtraData(); //Extra values as unparsed strings
```
* Because this is a custom behaviour you are free to use breakSpeed, updateDelay, state and extraData for any purpose,
but the validation process can change or reject certain values so it is better to put values for custom behaviour in extraData.
* It is your responsibility to explain to your users how each fragility value is used for your tile entity.
* It is your users' responsibility to enter the fragility values correctly. If they enter a behaviour value other than MOD they will not see your custom behaviour on collision.
### Pull Requests
Any pull requests are very welcome. There are currently no standards for pull requests but clean code which
follows the existing patterns is appreciated. If the pull request is a new feature, message me first to see
if I will accept it!
