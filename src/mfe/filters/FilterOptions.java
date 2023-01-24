package mfe.filters;

import arc.*;
import arc.func.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.maps.filters.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;
import static mfe.MapFilterExt.*;

public class FilterOptions{
    public static final Boolf<Block> oresOptional = b -> b == Blocks.air || (b instanceof OverlayFloor && !headless && Core.atlas.isFound(b.fullIcon));

    public static class SliderOption extends FilterOption{
        final String name;
        final Floatp getter;
        final Floatc setter;
        final float min, max, step;

        boolean display = true;

        SliderOption(String name, Floatp getter, Floatc setter, float min, float max){
            this(name, getter, setter, min, max, (max - min) / 200);
        }

        SliderOption(String name, Floatp getter, Floatc setter, float min, float max, float step){
            this.name = name;
            this.getter = getter;
            this.setter = setter;
            this.min = min;
            this.max = max;
            this.step = step;
        }

        public SliderOption display(){
            display = true;
            return this;
        }

        @Override
        public void build(Table table){
            Element base;
            if(!display){
                Label l = new Label("@filter.option." + name);
                l.setWrap(true);
                l.setStyle(Styles.outlineLabel);
                base = l;
            }else{
                Table t = new Table().marginLeft(11f).marginRight(11f);
                base = t;
                t.add("@filter.option." + name).growX().wrap().style(Styles.outlineLabel);
                t.label(() -> Strings.autoFixed(getter.get(), 2)).style(Styles.outlineLabel).right().labelAlign(Align.right).padLeft(6);
            }
            base.touchable = Touchable.disabled;

            Slider slider = new Slider(min, max, step, false);
            slider.moved(setter);
            slider.setValue(getter.get());
            if(updateEditorOnChange){
                slider.changed(changed);
            }else{
                slider.released(changed);
            }

            table.stack(slider, base).colspan(2).pad(3).growX();
        }
    }

    public static class BlockOption extends FilterOption{
        final String name;
        final Prov<Block> supplier;
        final Cons<Block> consumer;
        final Boolf<Block> filter;

        BlockOption(String name, Prov<Block> supplier, Cons<Block> consumer, Boolf<Block> filter){
            this.name = name;
            this.supplier = supplier;
            this.consumer = consumer;
            this.filter = filter;
        }

        @Override
        public void build(Table table){
            table.button(b -> b.image(supplier.get().uiIcon).update(i -> ((TextureRegionDrawable)i.getDrawable())
                    .setRegion(supplier.get() == Blocks.air ? Icon.none.getRegion() : supplier.get().uiIcon)).size(iconSmall), () -> {
                BaseDialog dialog = new BaseDialog("");
                dialog.setFillParent(false);
                int i = 0;
                for(Block block : content.blocks()){
                    if(!filter.get(block)) continue;

                    dialog.cont.image(block == Blocks.air ? Icon.none.getRegion() : block.uiIcon).size(iconMed).pad(3).get().clicked(() -> {
                        consumer.get(block);
                        dialog.hide();
                        changed.run();
                    });
                    if(++i % 10 == 0) dialog.cont.row();
                }

                dialog.closeOnBack();
                dialog.show();
            }).pad(4).margin(12f);

            table.add("@filter.option." + name);
        }
    }

    public static class ToggleOption extends FilterOption{
        final String name;
        final Boolp getter;
        final Boolc setter;

        ToggleOption(String name, Boolp getter, Boolc setter){
            this.name = name;
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public void build(Table table){
            table.row();
            CheckBox check = table.check("@filter.option." + name, setter).growX().padBottom(5).padTop(5).center().get();
            check.changed(changed);
        }
    }

    public static class FieldOption extends FilterOption{
        final String name;
        final Prov<String> getter;
        final Cons<String> setter;
        TextField.TextFieldFilter textF;
        TextField.TextFieldValidator textV;

        FieldOption(String name, Prov<String> getter, Cons<String> setter, TextField.TextFieldFilter textf, TextField.TextFieldValidator textv){
            this.name = name;
            this.getter = getter;
            this.setter = (str) -> {
                setter.get(str);
                changed.run();
            };
            textF = textf;
            textV = textv;
        }

        @Override
        public void build(Table table){
            table.row();
            table.add("@filter.option." + name);
            TextField text = table.field(String.valueOf(getter.get()), textF, setter).growX().padBottom(5).padTop(5).center().get();
            text.setValidator(textV);
        }
    }

     public static class SliderFieldOption extends SliderOption{
        boolean textMode = false;
         SliderFieldOption(String name, Floatp getter, Floatc setter, float min, float max, float step){
             super(name, getter, setter, min, max, step);
         }

         @Override
         public void build(Table table){
             table.clear();
             if(!textMode){
                 super.build(table);
             }else{
                 table.add("@filter.option." + name);
                 TextField text = table.field(String.valueOf(getter.get()), TextField.TextFieldFilter.floatsOnly, s -> setter.get(Strings.parseFloat(s))).growX().padBottom(5).padTop(5).center().get();
                 text.setValidator(s -> Strings.parseFloat(s) >= min && Strings.parseFloat(s) <= max);
                 text.changed(changed);
             }
             table.button("" + Iconc.pencil, Styles.flatt, () -> {
                 textMode = !textMode;
                 build(table);
             }).checked(textMode).size(buttonSize);
         }
     }
}
