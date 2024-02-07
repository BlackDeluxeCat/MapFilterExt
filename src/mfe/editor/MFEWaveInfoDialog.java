package mfe.editor;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.editor.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;
import static mindustry.game.SpawnGroup.never;

public class MFEWaveInfoDialog extends BaseDialog{
    public static MFEWaveInfoDialog mfewave = new MFEWaveInfoDialog();
    Seq<SpawnGroup> groups = new Seq<>();
    Seq<SpawnGroup> selectedGroups = new Seq<>();
    int search = -1;
    WaveGraph graph = new WaveGraph();
    WaveCanvas canvas = new WaveCanvas();
    Table config;
    float updateTimer, updatePeriod = 1f;
    boolean batchEditing;
    boolean checkedSpawns;
    public MFEWaveInfoDialog(){
        super("@waves.title");

        shown(() -> {
            setup();
            checkedSpawns = false;
            batchEditing = false;
        });
        hidden(() -> state.rules.spawns = groups);

        onResize(this::setup);
        addCloseButton();

        buttons.button("@waves.edit", Icon.edit, () -> {
            BaseDialog dialog = new BaseDialog("@waves.edit");
            dialog.addCloseButton();
            dialog.setFillParent(false);
            dialog.cont.table(Tex.button, t -> {
                var style = Styles.cleart;
                t.defaults().size(280f, 64f).pad(2f);

                t.button("@waves.copy", Icon.copy, style, () -> {
                    ui.showInfoFade("@waves.copied");
                    Core.app.setClipboardText(maps.writeWaves(groups));
                    dialog.hide();
                }).disabled(b -> groups == null || groups.isEmpty()).marginLeft(12f).row();

                t.button("@waves.load", Icon.download, style, () -> {
                    try{
                        groups = maps.readWaves(Core.app.getClipboardText());
                        buildGroups();
                    }catch(Exception e){
                        e.printStackTrace();
                        ui.showErrorMessage("@waves.invalid");
                    }
                    dialog.hide();
                }).disabled(Core.app.getClipboardText() == null || !Core.app.getClipboardText().startsWith("[")).marginLeft(12f).row();

                t.button("@clear", Icon.none, style, () -> ui.showConfirm("@confirm", "@settings.clear.confirm", () -> {
                    groups.clear();
                    buildGroups();
                    dialog.hide();
                })).marginLeft(12f).row();

                t.button("@settings.reset", Icon.refresh, style, () -> ui.showConfirm("@confirm", "@settings.clear.confirm", () -> {
                    groups = JsonIO.copy(waves.get());
                    buildGroups();
                    dialog.hide();
                })).marginLeft(12f);
            });

            dialog.show();
        }).size(250f, 64f);

        if(experimental){
            buttons.button(Core.bundle.get("waves.random"), Icon.refresh, () -> {
                groups.clear();
                groups = Waves.generate(1f / 10f);
                buildGroups();
            }).width(200f);
        }
    }
    void setup(){
        groups = JsonIO.copy(state.rules.spawns.isEmpty() ? waves.get() : state.rules.spawns);
        if(groups == null) groups = new Seq<>();

        cont.clear();

        //main chart
        cont.table(main -> {
            main.add();

            main.stack(canvas,
                new Label("@waves.none"){{
                    visible(() -> groups.isEmpty());
                    this.touchable = Touchable.disabled;
                    setWrap(true);
                    setAlignment(Align.center, Align.center);
                }}, new Table(shell -> {
                    shell.table(t -> {
                        config = t;
                        t.setClip(true);
                        t.background(Styles.black6);
                        t.visible(() -> !selectedGroups.isEmpty());
                    });
                })
            ).grow().margin(10f);
        }).grow();

        cont.row();

        cont.table(tools -> {
            tools.defaults().minWidth(120f).height(40f).left();
            tools.button("@add", () -> {
                showAnyContentsYouWant(content.units().copy().removeAll(UnitType::isHidden), "title", type -> groups.add(new SpawnGroup(type)), null);
                buildGroups();
            });
            tools.check("Batch Edit", b -> batchEditing = b);
            tools.table(s -> {
                s.image(Icon.zoom).padRight(8);
                s.field(search < 0 ? "" : (search + 1) + "", TextField.TextFieldFilter.digitsOnly, text -> {
                    search = groups.any() ? Strings.parseInt(text, 0) - 1 : -1;
                    buildGroups();
                }).growX().maxTextLength(8).get().setMessageText("@waves.search");
            });
            tools.add().growX().minWidth(0f);

            tools.button("-", () -> {}).update(t -> {
                if(t.getClickListener().isPressed()){
                    scaleX(-1);
                }
            }).width(60f);
            tools.button("+", () -> {}).update(t -> {
                if(t.getClickListener().isPressed()){
                    scaleX(1);
                }
            }).width(60f);
        }).growX();

        //cont.add(graph = new WaveGraph()).grow();

        buildGroups();
        buildConfig();
    }

    void buildGroups(){
        canvas.clearChildren();
        float marginTop = 4f;
        for(SpawnGroup group : groups){
            canvas.addChild(new Table(){
                {
                    margin(2f);
                    background(Tex.whiteui);
                    touchable = Touchable.enabled;
                    color.set(Color.gray).a(0.2f);
                    clicked(() -> {
                        if(!batchEditing){
                            if(!selectedGroups.remove(group)) selectedGroups.clear().add(group);
                        }else{
                            if(!selectedGroups.remove(group)) selectedGroups.add(group);
                        }
                        buildConfig();
                    });

                    label(() -> String.valueOf(group.begin + 1));
                    add(new Table(t -> {
                        image(group.type.uiIcon).size(32f).scaling(Scaling.fit);
                        if(group.effect != null && group.effect != StatusEffects.none) image(group.effect.uiIcon).size(32f).scaling(Scaling.fit);
                        if(group.items != null){
                            var label = new Label(String.valueOf(group.items.amount));
                            label.setAlignment(Align.bottomRight);
                            label.setFillParent(true);
                            label.setFontScale(0.7f);
                            stack(new Image(group.items.item.uiIcon).setScaling(Scaling.fit), label).size(32f);
                        }
                    }){
                        @Override
                        public float getPrefWidth(){
                            return 1f;
                        }
                    }).grow();
                    label(() -> group.end == SpawnGroup.never ? "∞" : String.valueOf(group.end + 1));
                }

                @Override
                public void act(float delta){
                    super.act(delta);
                    int index = groups.indexOf(group, true);
                    setPosition(canvas.tileX(group.begin), -canvas.camera.y + canvas.tileH * (groups.size - index));
                    setSize(Mathf.clamp(canvas.tileX(group.end + (group.end == never?0:1)) - canvas.tileX(group.begin), getPrefWidth(), canvas.getWidth()), canvas.tileH - marginTop);
                }

                @Override
                protected void drawBackground(float x, float y){
                    color.set(color(group.type)).a(0.7f);
                    super.drawBackground(x, y);

                    int start = canvas.getStartWave();
                    int end = canvas.getEndWave();
                    Draw.color(selectedGroups.contains(group, true) ? Pal.accent : color(group.type));
                    for(int i = group.begin; i <= group.end && i <=end; i += group.spacing){
                        if(i < start) continue;
                        Fill.rect(Tmp.r1.set(canvas.tileX(i), y, canvas.tileX(i + 1) - canvas.tileX(i), canvas.tileH - marginTop));
                    }
                    Draw.reset();
                    color.set(Color.white);
                }
            });
        }
    }

    void buildConfig(){
        if(config == null || selectedGroups.isEmpty()) return;
        var group = selectedGroups.get(selectedGroups.size - 1);
        config.clear();
        config.button("Deselect", () -> selectedGroups.clear()).width(300f).row();
        config.table(b -> {
            b.left();
            b.image(group.type.uiIcon).size(32f).padRight(3).scaling(Scaling.fit);
            b.add(group.type.localizedName).color(Pal.accent);

            b.add().growX();

            b.button(Icon.copySmall, Styles.emptyi, () -> {
                groups.insert(groups.indexOf(group) + 1, group.copy());
                buildGroups();
            }).pad(-6).size(46f).tooltip("@editor.copy");

            b.button(Icon.unitsSmall, Styles.emptyi, () -> showAnyContentsYouWant(content.units().copy().removeAll(UnitType::isHidden), "title", type -> {
                batchEditing(g -> g.type = type);
                buildConfig();
            }, null)).pad(-6).size(46f).tooltip("@stat.unittype");

            b.button(Icon.cancel, Styles.emptyi, () -> {
                batchEditing(g -> groups.remove(g));
                selectedGroups.clear();
                buildGroups();
                buildConfig();
            }).pad(-6).size(46f).padRight(-12f).tooltip("@waves.remove");
            b.clicked(KeyCode.mouseMiddle, () -> {
                groups.insert(groups.indexOf(group) + 1, group.copy());
                buildGroups();
            });
        }).row();

        config.table(t -> {
            t.table(spawns -> {
                spawns.field("" + (group.begin + 1), TextField.TextFieldFilter.digitsOnly, text -> {
                    if(Strings.canParsePositiveInt(text)){
                        batchEditing(g -> g.begin = Strings.parseInt(text) - 1);
                    }
                }).width(100f);
                spawns.add("@waves.to").padLeft(4).padRight(4);
                spawns.field(group.end == never ? "" : (group.end + 1) + "", TextField.TextFieldFilter.digitsOnly, text -> {
                    if(Strings.canParsePositiveInt(text)){
                        batchEditing(g -> g.end = Strings.parseInt(text) - 1);
                    }else if(text.isEmpty()){
                        batchEditing(g -> g.end = never);
                    }
                }).width(100f).get().setMessageText("∞");
            }).row();

            t.table(p -> {
                p.add("@waves.every").padRight(4);
                p.field(group.spacing + "", TextField.TextFieldFilter.digitsOnly, text -> {
                    if(Strings.canParsePositiveInt(text) && Strings.parseInt(text) > 0){
                        batchEditing(g -> g.spacing = Strings.parseInt(text));
                    }
                }).width(100f);
                p.add("@waves.waves").padLeft(4);
            }).row();

            t.table(a -> {
                a.field(group.unitAmount + "", TextField.TextFieldFilter.digitsOnly, text -> {
                    if(Strings.canParsePositiveInt(text)){
                        batchEditing(g -> g.unitAmount = Strings.parseInt(text));
                    }
                }).width(80f);

                a.add(" + ");
                a.field(Strings.fixed(Math.max((Mathf.zero(group.unitScaling) ? 0 : 1f / group.unitScaling), 0), 2), TextField.TextFieldFilter.floatsOnly, text -> {
                    if(Strings.canParsePositiveFloat(text)){
                        batchEditing(g -> g.unitScaling = 1f / Strings.parseFloat(text));
                    }
                }).width(80f);
                a.add("@waves.perspawn").padLeft(4);
            }).row();

            t.table(a -> {
                a.field(group.max + "", TextField.TextFieldFilter.digitsOnly, text -> {
                    if(Strings.canParsePositiveInt(text)){
                        batchEditing(g -> g.max = Strings.parseInt(text));
                    }
                }).width(80f);

                a.add("@waves.max").padLeft(5);
            }).row();

            t.table(a -> {
                a.field((int)group.shields + "", TextField.TextFieldFilter.digitsOnly, text -> {
                    if(Strings.canParsePositiveInt(text)){
                        batchEditing(g -> g.shields = Strings.parseInt(text));
                    }
                }).width(80f);

                a.add(" + ");
                a.field((int)group.shieldScaling + "", TextField.TextFieldFilter.digitsOnly, text -> {
                    if(Strings.canParsePositiveInt(text)){
                        batchEditing(g -> g.shieldScaling = Strings.parseInt(text));
                    }
                }).width(80f);
                a.add("@waves.shields").padLeft(4);
            }).row();

            t.table(a -> {
                a.button(b -> {
                    b.image(group.effect != null && group.effect != StatusEffects.none ? group.effect.uiIcon : Icon.none.getRegion()).scaling(Scaling.fit).size(32f);
                }, () -> {
                    showEffects();
                    buildGroups();
                    buildConfig();
                });

                a.check("@waves.guardian", b -> {
                    batchEditing(g -> g.effect = b ? StatusEffects.boss : StatusEffects.none);
                    buildGroups();
                    buildConfig();
                }).padTop(4).update(b -> b.setChecked(group.effect == StatusEffects.boss)).padBottom(8f);
            }).width(300f).row();

            t.table(a -> {
                a.button(b -> b.image(group.items == null ? Icon.none.getRegion() : group.items.item.uiIcon).scaling(Scaling.fit).size(32f), () -> showAnyContentsYouWant(content.items().removeAll(Item::isHidden), "", item -> {
                    if(group.items == null) group.items = new ItemStack().set(Items.copper, group.type.itemCapacity);
                    group.items.item = item;
                    buildGroups();
                    buildConfig();
                }, () -> group.items = null));

                a.field(group.items == null ? "" : String.valueOf(group.items.amount), TextField.TextFieldFilter.digitsOnly, text -> {
                    if(Strings.canParsePositiveInt(text)){
                        batchEditing(g -> g.items.amount = Strings.parseInt(text));
                        buildGroups();
                    }
                }).width(80f).disabled(tf -> group.items == null);
            }).width(300f).row();

            t.table(a -> {
                a.add("@waves.spawn").padRight(8);

                a.button("", () -> {
                    if(!checkedSpawns){
                        //recalculate waves when changed
                        Vars.spawner.reset();
                        checkedSpawns = true;
                    }

                    BaseDialog dialog = new BaseDialog("@waves.spawn.select");
                    dialog.cont.pane(p -> {
                        p.background(Tex.button).margin(10f);
                        int i = 0;
                        int cols = 4;
                        int max = 20;

                        if(spawner.getSpawns().size >= max){
                            p.add("[lightgray](first " + max + ")").colspan(cols).padBottom(4).row();
                        }

                        for(var spawn : spawner.getSpawns()){
                            p.button(spawn.x + ", " + spawn.y, Styles.flatTogglet, () -> {
                                batchEditing(g -> g.spawn = spawn.pos());
                                dialog.hide();
                            }).size(110f, 45f).checked(spawn.pos() == group.spawn);
                            if(++i % cols == 0){
                                p.row();
                            }

                            //only display first 20 spawns, you don't need to see more.
                            if(i >= 20){
                                break;
                            }
                        }

                        if(spawner.getSpawns().isEmpty()){
                            p.add("@waves.spawn.none");
                        }else{
                            p.button("@waves.spawn.all", Styles.flatTogglet, () -> {
                                batchEditing(g -> g.spawn = -1);
                                dialog.hide();
                            }).size(110f, 45f).checked(-1 == group.spawn);
                        }
                    }).grow();
                    dialog.setFillParent(false);
                    dialog.addCloseButton();
                    dialog.show();
                }).width(160f).height(36f).get().getLabel().setText(() -> group.spawn == -1 ? "@waves.spawn.all" : Point2.x(group.spawn) + ", " + Point2.y(group.spawn));
            }).padBottom(8f).row();
        }).width(300f).row();
    }

    void batchEditing(Cons<SpawnGroup> run){
        selectedGroups.each(run);
    }

    <T extends UnlockableContent> void showAnyContentsYouWant(Seq<T> seq, String title, Cons<T> setter, @Nullable Runnable noneSetter){
        boolean reset = noneSetter != null;
        BaseDialog dialog = new BaseDialog(title);
        dialog.cont.pane(p -> {
            p.defaults().pad(2).fillX();
            if(reset){
                p.button(t -> {
                    t.left();
                    t.image(Icon.none).size(8 * 4).scaling(Scaling.fit).padRight(2f);
                    t.add("@settings.resetKey");
                }, () -> {
                    noneSetter.run();
                    dialog.hide();
                    buildGroups();
                }).margin(12f);
            }
            int i = reset ? 1 : 0;
            for(T type : seq){
                p.button(t -> {
                    t.left();
                    t.image(type.uiIcon).size(8 * 4).scaling(Scaling.fit).padRight(2f);
                    t.add(type.localizedName);
                }, () -> {
                    setter.get(type);
                    dialog.hide();
                    buildGroups();
                }).margin(12f);
                if(++i % 3 == 0) p.row();
            }
        }).growX().scrollX(false);
        dialog.addCloseButton();
        dialog.show();
    }

    void showEffects(){
        showAnyContentsYouWant(content.statusEffects().copy().removeAll(effect -> effect.isHidden() || effect.reactive), "", effect -> batchEditing(g -> g.effect = effect), () -> batchEditing(g -> g.effect = StatusEffects.none));
    }

    void scaleX(int amount){
        updateTimer += Time.delta;
        if(updateTimer >= updatePeriod){
            canvas.tileW += amount;
            updateTimer = 0f;
        }
    }

    Color color(UnitType type){
        return Tmp.c1.fromHsv(type.id / (float)Vars.content.units().size * 60f + 180f, 0.7f, 0.6f).a(1f);
    }

    public class WaveCanvas extends WidgetGroup{
        float tileH, tileW;
        Vec2 vec = new Vec2(), sv = new Vec2();
        /** camera position left bottom. */
        public Vec2 camera = new Vec2();
        public WaveCanvas(){
            this.setTransform(true);
            this.setCullingArea(new Rect());
            tileH = 50f;
            tileW = 25f;

            addListener(new InputListener(){
                float lx, ly;
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                    sv.setZero();
                    vec.setZero();
                    lx = x;
                    ly = y;
                    return true;
                }

                @Override
                public void touchDragged(InputEvent event, float x, float y, int pointer){
                    sv.sub(x - lx, y - ly);
                    camera.sub(x - lx, y - ly);
                    lx = x;
                    ly = y;
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                    vec.set(sv);
                }
            });
        }

        @Override
        public void act(float delta){
            super.act(delta);
            this.getCullingArea().set(0, 0, getWidth(), getHeight());
            camera.add(vec.x, vec.y);
            sv.scl(0.5f);
            vec.scl(0.9f);
            camera.lerpDelta(Mathf.maxZero(camera.x), Mathf.clamp(camera.y, 0f, tileH * (groups.size + 1) - getHeight()), 0.1f);
        }

        public int getStartWave(){
            return Math.max(Mathf.floor(camera.x / tileW), 0);
        }

        public int getEndWave(){
            return Math.max(Mathf.ceil((camera.x + getWidth()) / tileW), 0);
        }

        public float tileX(int wave){
            return Mathf.clamp(-camera.x + wave * tileW, 0, getWidth());
        }

        @Override
        public void draw(){
            int start = getStartWave();
            int end = getEndWave();
            int gap = Mathf.ceil(100f / tileW);
            Lines.stroke(4f);
            Draw.color(Color.gray, 0.3f);
            Font font = Fonts.outline;
            font.setColor(Color.gray);
            for(int i = start / gap * gap; i < end; i += 1){
                float cx = -camera.x + i * tileW;
                if(cx < 0) continue;
                float dx = cx + this.x;
                float dy = this.y;
                Lines.dashLine(dx, dy + 16, dx, dy + getHeight(), 60);
                if(Mathf.mod(i, gap) == 0) font.draw(String.valueOf(i + 1), dx + tileW/2f, dy + 12, Align.center);
            }
            font.setColor(Color.white);
            Draw.reset();

            applyTransform(computeTransform());
            Draw.flush();
            if(clipBegin(0, 16, getWidth(), getHeight())){
                drawChildren();
                Draw.flush();
                clipEnd();
            }
            resetTransform();
        }
    }
}
