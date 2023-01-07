package mfe.filters;

import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.maps.filters.*;
import mindustry.world.*;

import static mfe.filters.FilterOptions.*;
import static mindustry.maps.filters.FilterOption.*;

public class AdvancedOreFilter extends MI2UGenerateFilter{
    public float threshold = 0.5f, octaves = 3f, falloff = 0.5f;
    public Block ore = Blocks.oreCopper, targetFloor = Blocks.air, targetOre = Blocks.air;

    @Override
    public FilterOption[] options(){
        return new FilterOption[]{
                new SliderFieldOption("threshold", () -> threshold, f -> threshold = f, 0f, 1f, 0.01f),
                new SliderFieldOption("octaves", () -> octaves, f -> octaves = f, 1f, 10f, 0.1f),
                new SliderFieldOption("falloff", () -> falloff, f -> falloff = f, 0f, 1f, 0.01f),
                new BlockOption("ore", () -> ore, b -> ore = b, oresOnly),
                new BlockOption("targetFloor", () -> targetFloor, b -> targetFloor = b, floorsOptional),
                new BlockOption("targetOre", () -> targetOre, b -> targetOre = b, oresOptional)
        };
    }

    @Override
    public char icon(){
        return Iconc.blockLogicDisplay;
    }

    @Override
    public void apply(GenerateInput in){
        preConsume(in);
        if(regionConsumer == this && regionseq.count(r -> r.contains(in.x, in.y)) <= 0) return;
        var v = Tmp.v3.set(in.x, in.y);
        if(transConsumer == this) transeq.each(c -> c.get(v));
        float noise = noise(v.x, v.y, 1f, octaves, falloff);

        if(noise > threshold && in.overlay != Blocks.spawn && in.floor.asFloor().hasSurface()){
            if((targetFloor == Blocks.air || in.floor == targetFloor) && (targetOre == Blocks.air || in.overlay == targetOre))
            in.overlay = ore;
        }
    }
}
