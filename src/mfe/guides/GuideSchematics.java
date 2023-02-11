package mfe.guides;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mi2.utils.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mfe.MapFilterExt.*;
import static mindustry.Vars.*;

public class GuideSchematics{
    public static StringMap schematics = new StringMap();
    public static String prefix = "Sche-";
    public static BaseDialog schematicsDialog;

    static{
        schematicsDialog = new BaseDialog("@schmatic");
        schematicsDialog.addCloseButton();
        schematicsDialog.shown(GuideSchematics::rebuild);
    }

    public static void save(){
        schematics.each((name, sche) -> config.putstr(prefix + name, sche));
    }

    public static void load(){
        JsonValue cfg = RefUtils.getValue(config, "cfg");
        cfg.forEach(jv -> {
            if(jv.name.startsWith(prefix)){
                schematics.put(jv.name.substring(prefix.length()), jv.asString());
            }
        });
    }

    public static void rebuild(){
        schematicsDialog.cont.clear();
        schematicsDialog.cont.button(Iconc.save + Core.bundle.get("guide.schematic.new"), Styles.flatBordert, () -> {
            if(GuideSeqImage.guides.isEmpty()){
                ui.showInfoFade("@guide.schematic.emptyguide", 10f);
                return;
            }
            ui.showTextInput("@schematic.add", "@name", "", text -> {
                boolean replacement = schematics.containsKey(text);
                if(replacement){
                    ui.showConfirm("@confirm", "@schematic.replace", () -> {
                        schematics.put(text, JsonIO.write(GuideSeqImage.guides));
                        ui.showInfoFade("@schematic.saved");
                    });
                }else{
                    schematics.put(text, JsonIO.write(GuideSeqImage.guides));
                    ui.showInfoFade("@schematic.saved");
                }
                rebuild();
                save();
            });
        }).size(500f, 100f);

        schematicsDialog.cont.row();

        schematicsDialog.cont.pane(t -> {
            final int[] co = new int[1];
            schematics.each((name, sche) -> {
                t.table(b -> {
                    b.setBackground(Tex.pane);

                    b.add(name).height(buttonSize).left();

                    b.row();

                    b.table(tt -> {
                        tt.button("" + Iconc.paste, Styles.flatBordert, () -> {
                            try{
                                GuideSeqImage.clearGuides();
                                GuideSeqImage.guides.set(JsonIO.read(Seq.class, sche));
                                GuideSeqImage.rebuild();
                                schematicsDialog.hide();
                            }catch(Throwable e){
                                ui.showException(e);
                                Log.err(e);
                            }
                        }).size(buttonSize);
                        tt.button("" + Iconc.paste, Styles.flatBordert, () -> {
                            try{
                                GuideSeqImage.guides.add(JsonIO.read(Seq.class, sche));
                                GuideSeqImage.rebuild();
                                schematicsDialog.hide();
                            }catch(Throwable e){
                                ui.showException(e);
                                Log.err(e);
                            }
                        }).size(buttonSize).with(tb -> {
                            tb.add("+").color(Color.green).style(Styles.outlineLabel).name("memory").width(0.1f).with(l -> {
                                l.setAlignment(Align.bottomRight);
                                l.setFontScale(0.75f);
                            }).fillY();
                        });

                        tt.button("" + Iconc.cancel, Styles.flatBordert, () -> ui.showConfirm(Core.bundle.get("@schematic.delete.confirm") + ": " + name, () -> {
                            schematics.remove(name);
                            save();
                        })).size(buttonSize).with(tb -> tb.getLabel().setColor(Color.scarlet));
                    }).left();
                    b.row();
                    b.labelWrap(sche).grow().maxHeight(150f).with(l -> l.setAlignment(Align.topLeft)).color(Color.gray);
                }).size(400f, 250f).pad(4f);

                if(Mathf.mod(++co[0], Math.max(1, Core.graphics.getWidth() / 410)) == 0) t.row();
            });
        }).grow();
    }
}
