package mfe.filters;

import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.maps.filters.*;
import mindustry.world.*;

import static mfe.filters.FilterOptions.*;

public class AdvancedNoiseFilter extends MI2UGenerateFilter{
    public float threshold = 0.5f, octaves = 3f, falloff = 0.5f;
    public Block floor = Blocks.air, block = Blocks.air, target = Blocks.air, target2 = Blocks.air;

    @Override
    public FilterOption[] options(){
        return new FilterOption[]{
                new SliderFieldOption("threshold", () -> threshold, f -> threshold = f, 0f, 1f, 0.01f),
                new SliderFieldOption("octaves", () -> octaves, f -> octaves = f, 1f, 10f, 0.1f),
                new SliderFieldOption("falloff", () -> falloff, f -> falloff = f, 0f, 1f, 0.01f),
                new BlockOption("targetFloor", () -> target, b -> target = b, FilterOption.floorsOptional),
                new BlockOption("floor", () -> floor, b -> floor = b, FilterOption.floorsOptional),
                new BlockOption("targetWall", () -> target2, b -> target2 = b, FilterOption.wallsOptional),
                new BlockOption("wall", () -> block, b -> block = b, FilterOption.wallsOptional)
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

        if(noise > threshold){
            if(floor != Blocks.air && in.floor == target) in.floor = floor;
            if(block != Blocks.air && in.block == target2) in.block = block;
        }
    }
}
