package com.fredtargaryen.fragileglass.config.behaviour.configloader;

import com.fredtargaryen.fragileglass.DataReference;
import com.fredtargaryen.fragileglass.config.behaviour.data.BreakerData;
import com.fredtargaryen.fragileglass.config.behaviour.datamanager.EntityDataManager;
import net.minecraft.entity.EntityType;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

public class EntityConfigLoader extends ConfigLoader{
    private EntityDataManager manager;
    private HashMap<EntityType, BreakerData> entities;

    public EntityConfigLoader(EntityDataManager manager, HashMap<EntityType, BreakerData> entities) {
        this.manager = manager;
        this.entities = entities;
    }

    /**
     * Parse one line of the file.
     */
    @Override
    protected void parseLine() throws ConfigLoadException {
        String[] values = line.split(" ");
        //Validate number of values on row
        if(values.length < 3) {
            throw new ConfigLoadException("There must be at least 3 values here.");
        }
        else {
            //Get collection of entities described on this line
            Collection<EntityType<?>> entityTypes;
            if(values[0].charAt(0) == '#') {
                //values[0] is a tag representing multiple entities
                entityTypes = EntityTypeTags.getCollection()
                        .getOrCreate(new ResourceLocation(values[0].substring(1)))
                        .getAllElements();
            }
            else {
                //values[0] is a single entity
                entityTypes = new ArrayList<>();
                //Check the first value is a ResourceLocation in the Forge EntityType registry, i.e. refers to a valid entity
                EntityType entry = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(values[0]));
                if (entry == null) {
                    throw new ConfigLoadException("There is no entity type with the resource location " + values[0] + ".");
                } else {
                    entityTypes.add(entry);
                }
            }
            try {
                for(EntityType et : entityTypes) {
                    //Validate minSpeed and silently clamp to >= 0
                    double minSpeedSquared = Double.parseDouble(values[1]);
                    minSpeedSquared = Math.max(minSpeedSquared * minSpeedSquared, 0.0);
                    //Validate maxSpeed and silently clamp to <= max speed
                    double maxSpeedSquared = Double.parseDouble(values[2]);
                    maxSpeedSquared = Math.min(maxSpeedSquared * maxSpeedSquared, DataReference.MAXIMUM_ENTITY_SPEED_SQUARED);
                    //Ensure minSpeed <= maxSpeed. If not, silently swap the values
                    if (minSpeedSquared > maxSpeedSquared) {
                        double temp = minSpeedSquared;
                        minSpeedSquared = maxSpeedSquared;
                        maxSpeedSquared = temp;
                    }
                    this.entities.put(et, new BreakerData(minSpeedSquared, maxSpeedSquared, Arrays.copyOfRange(values, 3, values.length)));
                }
            }
            catch(NumberFormatException nfe) {
                //Thrown when speed values can't be parsed as Doubles
                throw new ConfigLoadException("One of your speed values can't be read as a decimal number.");
            }
        }
    }
}