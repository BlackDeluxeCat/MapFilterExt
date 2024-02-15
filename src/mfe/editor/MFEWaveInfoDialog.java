package mfe.editor;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;
import static mindustry.game.SpawnGroup.*;

//TODO mobile dragging conflict
public class MFEWaveInfoDialog extends BaseDialog{
    public static MFEWaveInfoDialog mfewave = new MFEWaveInfoDialog();
    Seq<SpawnGroup> groups = new Seq<>();
    Seq<SpawnGroup> selectedGroups = new Seq<>();
    WaveCanvas canvas = new WaveCanvas();
    Table config;
    int search = -1;
    boolean batchEditing;
    boolean checkedSpawns;

    boolean showViewSettings, showWaveGraph;
    Table viewSettings, graphViewSettings, graphViewSettingsColors;
    boolean filterPayloads, filterItems, filterEffects;
    @Nullable
    UnitType filterType;
    int filterSpawn = -1;
    boolean reverseSort = false;
    boolean detailedBar, barFillX;

    public MFEWaveInfoDialog(){
        super("@waves.title");

        shown(this::setup);

        hidden(() -> {
            if(state.isEditor() || state.isMenu()){
                //clean up invalid configs
                groups.each(g -> {
                    if(g.type.payloadCapacity == 0) g.payloads = null;
                    if(g.type.itemCapacity == 0) g.items = null;
                });
                state.rules.spawns = groups;
            }
        });

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
            }).width(150f);
        }
    }

    void setup(){
        groups = JsonIO.copy(state.rules.spawns.isEmpty() ? waves.get() : state.rules.spawns);
        if(groups == null) groups = new Seq<>();
        canvas.camera.setZero();
        checkedSpawns = false;
        batchEditing = false;
        selectedGroups.clear();
        filterSpawn = -1;

        cont.clear();

        //main
        cont.stack(canvas,
            new Label("@waves.none"){{
                visible(() -> groups.isEmpty());
                this.touchable = Touchable.disabled;
                setWrap(true);
                setAlignment(Align.center, Align.center);
            }},

            new Table(shell -> {
                shell.visible(() -> !selectedGroups.isEmpty());
                shell.pane(t -> {
                    config = t;
                    t.background(Tex.button);
                });
            }).left().bottom(),

            new Table(shell -> {
                shell.visible(() -> showViewSettings && !showWaveGraph);
                shell.pane(t -> {
                    viewSettings = t;
                    t.background(Tex.button);
                    t.margin(12f);

                    t.add("@settings.graphics").height(40f).grow().row();

                    t.table(graph -> {
                        graph.defaults().growX().height(40f).pad(4f);

                        graph.button("Detailed Bar", Styles.togglet, () -> {
                            detailedBar = !detailedBar;
                            buildGroups();
                        }).row();

                        graph.button("Bar Expand", Styles.togglet, () -> {
                            barFillX = !barFillX;
                        }).row();

                        graph.button("@keybind.screenshot.name", () -> canvas.screenshot());
                    }).growX().row();

                    t.add("@waveinfo.filters").height(40f).grow().row();
                    //boolean filters
                    t.table(gf -> {
                        gf.defaults().size(40f).pad(4f);

                        gf.button(Icon.effect, Styles.squareTogglei, () -> {
                            filterEffects = !filterEffects;
                            buildGroups();
                            canvas.locateWave(groups.min(g -> checkFilters(g) ? g.begin - 999999 : g.begin).begin);
                        });

                        gf.button(Icon.box, Styles.squareTogglei, () -> {
                            filterItems = !filterItems;
                            buildGroups();
                            canvas.locateWave(groups.min(g -> checkFilters(g) ? g.begin - 999999 : g.begin).begin);
                        });

                        gf.button(Icon.itchioSmall, Styles.squareTogglei, () -> {
                            filterPayloads = !filterPayloads;
                            buildGroups();
                            canvas.locateWave(groups.min(g -> checkFilters(g) ? g.begin - 999999 : g.begin).begin);
                        });
                    }).row();
                    //type filters
                    t.table(gf -> {
                        gf.defaults().height(40f).pad(4f);

                        gf.button(Icon.units, Styles.squarei, () -> showAnyContentsYouWant(groups.map(sg -> sg.type).asSet().toSeq().sort(type -> type.id), "", type -> {
                            filterType = type;
                            buildGroups();
                            canvas.locateWave(groups.min(g -> checkFilters(g) ? g.begin - 999999 : g.begin).begin);
                        })).width(40f).update(b -> b.getStyle().imageUp = filterType != null ? new TextureRegionDrawable(filterType.uiIcon) : Icon.units);

                        gf.button("", () -> {
                            if(!checkedSpawns){
                                //recalculate waves when changed
                                Vars.spawner.reset();
                                checkedSpawns = true;
                            }
                            showSpawns(i -> batchEditing(g -> g.spawn = i), () -> filterSpawn);
                        }).width(160f).get().getLabel().setText(() -> filterSpawn == -1 ? "@waves.spawn.all" : Point2.x(filterSpawn) + ", " + Point2.y(filterSpawn));
                    }).row();

                    t.add("@waveinfo.sorters").height(40f).grow().row();

                    t.table(st -> {
                        st.defaults().pad(4f);
                        st.table(Tex.button, f -> {
                            for(Sort s : Sort.all){
                                f.button("@waves.sort." + s, Styles.flatt, () -> {
                                    sort(s);
                                    buildGroups();
                                    canvas.locateWave(groups.min(g -> checkFilters(g) ? g.begin - 999999 : g.begin).begin);
                                }).size(80, 40f);
                            }
                        }).row();
                        st.check("@waves.sort.reverse", b -> {
                            reverseSort = b;
                            buildGroups();
                            canvas.locateWave(groups.min(g -> checkFilters(g) ? g.begin - 999999 : g.begin).begin);
                        }).height(40f).checked(reverseSort);
                    }).row();
                });
            }).right().bottom(),

            new Table(shell -> {
                shell.visible(() -> showViewSettings && showWaveGraph);
                shell.table(t -> {
                    graphViewSettings = t;
                    t.background(Tex.button);
                    t.margin(16f);
                    ButtonGroup<Button> group = new ButtonGroup<>();

                    for(Mode m : Mode.all){
                        t.button("@wavemode." + m.name(), Styles.fullTogglet, () -> {
                            canvas.mode = m;
                        }).group(group).height(35f).update(b -> b.setChecked(m == canvas.mode)).width(130f);
                    }
                }).row();
                shell.table(col -> {
                    graphViewSettingsColors = col;
                }).growX();
            }).right().bottom()
        ).grow().margin(16f);

        cont.row();

        cont.table(tools -> {
            tools.defaults().minWidth(120f).height(60f).left();
            tools.button("@add", () -> {
                showAnyContentsYouWant(content.units().copy().removeAll(UnitType::isHidden), "title", type -> groups.add(new SpawnGroup(type)), null);
                buildGroups();
            });
            tools.check("@waveinfo.batchedit", b -> batchEditing = b);
            tools.table(s -> {
                s.image(Icon.zoom).padRight(8);
                s.field("", TextField.TextFieldFilter.digitsOnly, text -> {
                    search = Math.max(Strings.parseInt(text, 1) - 1, 0);
                    if(groups.any()) canvas.locateWave(search);
                }).growX().maxTextLength(8).get().setMessageText("@waves.search");
            });
            tools.add().growX().minWidth(0f);

            tools.button(Iconc.left + "", () -> {}).update(t -> {
                if(t.getClickListener().isPressed()){
                    canvas.scrollX(-20);
                }
            }).width(60f);
            tools.button(Iconc.right + "", () -> {}).update(t -> {
                if(t.getClickListener().isPressed()){
                    canvas.scrollX(20);
                }
            }).width(60f);
            tools.button(Iconc.up + "", () -> {}).update(t -> {
                if(t.getClickListener().isPressed()){
                    canvas.scrollY(20);
                }
            }).width(60f);
            tools.button(Iconc.down + "", () -> {}).update(t -> {
                if(t.getClickListener().isPressed()){
                    canvas.scrollY(-20);
                }
            }).width(60f);
            tools.button("-", () -> {}).update(t -> {
                if(t.getClickListener().isPressed()){
                    canvas.scaleX(-1);
                }
            }).width(60f);
            tools.button("+", () -> {}).update(t -> {
                if(t.getClickListener().isPressed()){
                    canvas.scaleX(1);
                }
            }).width(60f);

            tools.button(Icon.filter, Styles.clearTogglei, () -> showViewSettings = !showViewSettings).checked(showViewSettings).width(60f).padLeft(8f);
            tools.button(Icon.chartBar, Styles.clearTogglei, () -> {
                showWaveGraph = !showWaveGraph;
            });
        }).growX();

        buildGroups();
    }

    boolean checkFilters(SpawnGroup group){
        if(filterType != null && group.type != filterType) return false;
        if(filterEffects && group.effect == null) return false;
        if(filterItems && group.items == null) return false;
        if(filterPayloads && group.payloads == null) return false;
        if(filterSpawn != -1 && group.spawn != filterSpawn) return false;
        return true;
    }

    void sort(Sort sort){
        groups.sort(Structs.comps(Structs.comparingFloat(sort.sort), Structs.comparingFloat(sort.secondary)));
        if(reverseSort) groups.reverse();
    }

    /** Rebuild group bars. Also rebuild config table. */
    void buildGroups(){
        canvas.clearChildren();

        if(groups != null){
            for(SpawnGroup group : groups){
                if(group.effect == StatusEffects.none) group.effect = null;
                if(!checkFilters(group)) continue;

                canvas.addGroup(group);
            }
        }

        canvas.addChild(canvas.graphDrawer);

        buildConfig();
    }

    /** Called when spawn group config changed. */
    void buildConfig(){
        if(config == null || selectedGroups.isEmpty()) return;

        canvas.cookStats();

        var group = selectedGroups.get(selectedGroups.size - 1);
        config.clear();
        config.button("@waveinfo.deselect", () -> selectedGroups.clear()).width(300f).row();
        config.table(b -> {
            b.left();
            b.image(group.type.uiIcon).size(32f).padRight(3).scaling(Scaling.fit);
            b.add(group.type.localizedName).color(Pal.accent);

            b.add().growX();

            b.button(Icon.copySmall, Styles.emptyi, () -> {
                groups.insert(groups.indexOf(group) + 1, copy(group));
                buildGroups();
            }).pad(-6).size(46f).tooltip("@editor.copy").disabled(bb -> batchEditing);

            b.button(Icon.unitsSmall, Styles.emptyi, () -> showAnyContentsYouWant(content.units().copy().removeAll(UnitType::isHidden), "", type -> {
                batchEditing(g -> g.type = type);
                buildConfig();
            }, null)).pad(-6).size(46f).tooltip("@stat.unittype");

            b.button(Icon.cancel, Styles.emptyi, () -> {
                batchEditing(g -> groups.remove(g));
                selectedGroups.clear();
                buildGroups();
            }).pad(-6).size(46f).padRight(-12f).tooltip("@waves.remove");
            b.clicked(KeyCode.mouseMiddle, () -> {
                groups.insert(groups.indexOf(group) + 1, copy(group));
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
                    b.image(group.effect != null ? group.effect.uiIcon : Icon.none.getRegion()).scaling(Scaling.fit).size(32f);
                }, () -> {
                    showEffects();
                    buildGroups();
                });

                a.check("@waves.guardian", b -> {
                    batchEditing(g -> g.effect = b ? StatusEffects.boss : null);
                    buildGroups();
                }).padTop(4).update(b -> b.setChecked(group.effect == StatusEffects.boss)).padBottom(8f);
            }).width(300f).row();

            t.table(a -> {
                a.button(b -> b.image(group.items == null ? Icon.none.getRegion() : group.items.item.uiIcon).scaling(Scaling.fit).size(32f), () -> showAnyContentsYouWant(content.items().copy().removeAll(Item::isHidden), "", item -> {
                    batchEditing(g -> {
                        if(g.items == null) g.items = new ItemStack().set(Items.copper, g.type.itemCapacity);
                        g.items.item = item;
                    });
                    buildGroups();
                }, () -> group.items = null));

                a.field(group.items == null ? "" : String.valueOf(group.items.amount), TextField.TextFieldFilter.digitsOnly, text -> {
                    if(Strings.canParsePositiveInt(text)){
                        batchEditing(g -> {
                            if(g.items != null) g.items.amount = Strings.parseInt(text);
                        });
                        buildGroups();
                    }
                }).width(80f).disabled(tf -> group.items == null);
            }).width(300f).row();

            t.table(a -> {
                a.button(b -> b.image(Icon.itchioSmall).scaling(Scaling.fit).size(32f), () -> showPayloads(group)).disabled(b -> batchEditing);
                if(group.payloads == null || group.payloads.isEmpty()) return;
                a.table(ps -> {
                    for(int i = Math.min(8, group.payloads.size); i > 0; i--){
                        ps.image(group.payloads.get(group.payloads.size - i).uiIcon).size(16f).scaling(Scaling.fit);
                    }
                });
            }).width(300f).row();

            t.table(a -> {
                a.add("@waves.spawn").padRight(8);

                a.button("", () -> {
                    if(!checkedSpawns){
                        //recalculate waves when changed
                        Vars.spawner.reset();
                        checkedSpawns = true;
                    }
                    showSpawns(i -> batchEditing(g -> g.spawn = i), () -> group.spawn);
                }).width(160f).height(36f).get().getLabel().setText(() -> group.spawn == -1 ? "@waves.spawn.all" : Point2.x(group.spawn) + ", " + Point2.y(group.spawn));
            }).padBottom(8f).row();
        }).width(300f).row();
    }

    SpawnGroup copy(SpawnGroup group){
        var copy = group.copy();
        copy.items = group.items == null ? null : group.items.copy();
        copy.payloads = group.payloads == null ? null : group.payloads.copy();
        return copy;
    }

    void batchEditing(Cons<SpawnGroup> run){
        selectedGroups.each(run);
    }

    <T extends UnlockableContent> void showAnyContentsYouWant(Seq<T> seq, String title, Cons<T> setter){
        showAnyContentsYouWant(seq, title, setter, () -> setter.get(null));
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
        showAnyContentsYouWant(content.statusEffects().copy().removeAll(effect -> effect.reactive), "", effect -> batchEditing(g -> g.effect = effect));
    }

    void showSpawns(Intc setter, Intp getter){
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
                    setter.get(spawn.pos());
                    dialog.hide();
                }).size(110f, 45f).checked(getter.get() == spawn.pos());
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
                    setter.get(-1);
                    dialog.hide();
                }).size(110f, 45f).checked(getter.get() == -1);
            }
        }).grow();
        dialog.setFillParent(false);
        dialog.addCloseButton();
        dialog.show();
    }

    void showPayloads(SpawnGroup group){
        BaseDialog dialog = new BaseDialog(""){
            Table pane;
            boolean showConfig;

            {
                addCloseButton();
                buttons.button("@add", Styles.togglet, () -> showConfig = !showConfig);
                hidden(() -> {
                    buildGroups();
                });

                cont.stack(new Table(t -> {
                    t.setFillParent(true);
                    t.pane(p -> {
                        pane = p;
                        build();
                    }).grow();
                }), new Table(t -> {
                    t.setFillParent(true);
                    t.table(tt -> {
                        tt.background(Tex.button);
                        tt.pane(p -> {
                            p.defaults().pad(2).fillX();
                            int i = 0;
                            for(var type : content.units().copy().removeAll(u -> u == UnitTypes.block)){
                                p.button(b -> {
                                    b.center();
                                    b.image(type.uiIcon).size(6 * 8).scaling(Scaling.fit).padRight(2f);
                                    b.row();
                                    b.add(type.localizedName);
                                }, () -> {
                                    if(group.payloads == null) group.payloads = new Seq<>();
                                    group.payloads.add(type);
                                    build();
                                }).margin(12f).size(96);
                                if(++i % 8 == 0) p.row();
                            }
                        });
                    }).height(300f).visible(() -> showConfig);
                }).bottom()).grow();
            }

            void build(){
                pane.clear();
                pane.defaults().pad(2).fillX();

                if(group.payloads == null || group.payloads.isEmpty()){
                    pane.add("@waves.none");
                    return;
                }

                for(int i = 0; i < group.payloads.size; i++){
                    if(i % 8 == 0) pane.row();
                    int ii = i;
                    pane.button(t -> {
                        t.center();
                        t.image(group.payloads.get(ii).uiIcon).size(8 * 8).scaling(Scaling.fit);
                        t.row();
                        t.add(group.payloads.get(ii).localizedName);
                    }, () -> {
                        group.payloads.ordered = true;
                        group.payloads.remove(ii);
                        build();
                    }).margin(12f).size(144f);
                }
            }
        };
        dialog.show();
    }

    Color color(UnitType type){
        return Tmp.c1.fromHsv(type.id / (float)Vars.content.units().size * 360f, 0.7f, 0.8f).a(1f);
    }

    public class WaveCanvas extends WidgetGroup{
        float tileH, tileW;
        Vec2 vec = new Vec2(), sv = new Vec2();
        /**
         * camera position left bottom.
         */
        public Vec2 camera = new Vec2();
        int start, end, tail;

        Element graphDrawer;
        Mode mode = Mode.counts;
        int[][] values;
        OrderedSet<UnitType> used = new OrderedSet<>();
        int max, maxTotal;
        float maxHealth;
        ObjectSet<UnitType> hidden = new ObjectSet<>();

        boolean pinching;

        public WaveCanvas(){
            this.setTransform(true);
            this.setCullingArea(new Rect());
            tileH = 50f;
            tileW = 50f;

            hovered(() -> Core.scene.setScrollFocus(this));

            addCaptureListener(new InputListener(){
                float lx, ly;

                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                    pinching = Core.input.isTouched(1);
                    if(pinching || button == KeyCode.mouseRight){
                        event.stop();
                        Core.scene.cancelTouchFocusExcept(this, WaveCanvas.this);//prevent children to drag.
                        sv.setZero();
                        vec.setZero();
                        lx = x;
                        ly = y;
                        return true;
                    }
                    return false;
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

                @Override
                public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY){
                    scrollX(amountY * 100f * (1f + Math.abs(x / getWidth() - 0.5f) * 8f));
                    return super.scrolled(event, x, y, amountX, amountY);
                }
            });

            graphDrawer = new Element(){
                @Override
                public void draw(){
                    super.draw();
                    Styles.black8.draw(x, y, width, height);

                    Lines.stroke(Scl.scl(3f));

                    GlyphLayout lay = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
                    Font font = Fonts.outline;

                    lay.setText(font, "1");

                    int maxY = switch(mode){
                        case counts -> nextStep(max);
                        case health -> nextStep((int)maxHealth);
                        case totals -> nextStep(maxTotal);
                    };

                    float fh = lay.height;
                    float offsetX = Scl.scl(lay.width * (maxY + "").length() * 2), offsetY = Scl.scl(24f) + fh;

                    float graphX = x + offsetX, graphY = y + offsetY, graphW = width - offsetX, graphH = height - offsetY;

                    if(mode == Mode.counts){
                        for(UnitType type : used.orderedItems()){
                            Draw.color(color(type));
                            Draw.alpha(parentAlpha);

                            Lines.beginLine();

                            for(int i = 0; i < values.length; i++){
                                int val = values[i][type.id];
                                float cy = graphY + val * graphH / maxY;
                                Lines.linePoint(tileW / 2f + tileX(start + i), cy);
                            }

                            Lines.endLine();
                        }
                    }else if(mode == Mode.totals){
                        Lines.beginLine();

                        Draw.color(Pal.accent);
                        for(int i = 0; i < values.length; i++){
                            int sum = 0;
                            for(UnitType type : used.orderedItems()){
                                sum += values[i][type.id];
                            }

                            float cy = graphY + sum * graphH / maxY;
                            Lines.linePoint(tileW / 2f + tileX(start + i), cy);
                        }

                        Lines.endLine();
                    }else if(mode == Mode.health){
                        Lines.beginLine();

                        Draw.color(Pal.health);
                        for(int i = 0; i < values.length; i++){
                            float sum = 0;
                            for(UnitType type : used.orderedItems()){
                                sum += (type.health) * values[i][type.id];
                            }

                            float cy = graphY + sum * graphH / maxY;
                            Lines.linePoint(tileW / 2f + tileX(start + i), cy);
                        }

                        Lines.endLine();
                    }

                    //how many numbers can fit here
                    float totalMarks = Mathf.clamp(maxY, 1, 10);

                    int markSpace = Math.max(1, Mathf.ceil(maxY / totalMarks));

                    Draw.color(Color.lightGray);
                    Draw.alpha(0.1f);

                    for(int i = 0; i < maxY; i += markSpace){
                        float cy = graphY + i * graphH / maxY, cx = graphX;

                        Lines.line(cx, cy, cx + graphW, cy);

                        lay.setText(font, "" + i);

                        font.draw("" + i, cx, cy + lay.height / 2f, Align.right);
                    }

                    font.setColor(Color.white);

                    Pools.free(lay);

                    Draw.reset();
                }
            }.visible(() -> showWaveGraph);
            graphDrawer.setFillParent(true);
        }

        @Override
        public void act(float delta){
            this.getCullingArea().set(0, 0, getWidth(), getHeight());

            camera.add(vec.x, vec.y);
            //vec decay
            sv.scl(0.5f);
            vec.scl(0.9f);
            camera.lerpDelta(Math.max(camera.x, -100f), Mathf.clamp(camera.y, -60f, tileH * (groups.size + 1) - getHeight() + 20f), 0.1f);

            super.act(delta);

            int lastStart = start, lastEnd = end;
            start = getStartWave();
            end = getEndWave();
            if(lastStart != start || lastEnd != end) cookStats();
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
            //draw axis
            if(transform) applyTransform(computeTransform());
            drawBackground();

            if(clipBegin(0, Scl.scl(16f), getWidth(), getHeight())){
                drawChildren();
                Draw.flush();
                clipEnd();
            }
            if(transform) resetTransform();
        }

        public void drawBackground(){
            int gap = Mathf.ceil(100f / tileW);
            Lines.stroke(4f);
            Draw.color(Color.gray, 0.35f * parentAlpha);
            Font font = Fonts.outline;
            font.setColor(Color.gray);
            float y1 = Math.max(16, -camera.y);
            float y2 = Math.min(getHeight(), getChildren() == null || getChildren().isEmpty() ? y1 : getChildren().max(e -> e instanceof SpawnGroupBar ? e.getTop() : 0f).getTop());
            for(int i = start / gap * gap; i < end; i += 1){
                float cx = -camera.x + i * tileW;
                if(cx < 0) continue;
                Lines.dashLine(cx, y1, cx, y2, 60);

                if(Mathf.mod(i, gap) == 0) font.draw(String.valueOf(i + 1), cx + tileW / 2f, 16, Align.center);
                if(search == i){
                    Fill.rect(Tmp.r1.set(cx, 0f, tileW, getHeight()));
                }
            }
            font.setColor(Color.white);

            if(tail < end){
                float tx = tileX(tail);
                Draw.color(Color.navy, 0.5f * parentAlpha);
                Fill.rect(Tmp.r1.set(tx, 0f, getWidth() - tx, getHeight()));
            }
            Draw.reset();
        }

        public void locateWave(int wave){
            camera.x = Mathf.maxZero(tileW * wave - getWidth() / 2f);
        }

        public void scrollX(float amount){
            camera.x += amount;
        }

        public void scrollY(float amount){
            camera.y += amount;
        }

        void scaleX(int amount){
            tileW += amount;
        }

        public void cookStats(){
            if(groups.isEmpty()){
                tail = 10;
            }else{
                var g = groups.max(g2 -> g2.end != never ? Math.max(g2.end, g2.begin + g2.spacing) : g2.begin + g2.spacing);
                tail = g.end != never ? Math.max(g.end, g.begin + g.spacing) : g.begin + g.spacing + 1;
            }

            values = new int[end - start + 1][Vars.content.units().size];
            used.clear();
            max = maxTotal = 1;
            maxHealth = 1f;

            for(int i = start; i <= end; i++){
                int index = i - start;
                float healthsum = 0f;
                int sum = 0;

                for(SpawnGroup spawn : groups){
                    int spawned = spawn.getSpawned(i);
                    values[index][spawn.type.id] += spawned;
                    if(spawned > 0){
                        used.add(spawn.type);
                    }
                    max = Math.max(max, values[index][spawn.type.id]);
                    healthsum += spawned * (spawn.type.health);
                    sum += spawned;
                }
                maxTotal = Math.max(maxTotal, sum);
                maxHealth = Math.max(maxHealth, healthsum);
            }

            ObjectSet<UnitType> usedCopy = new ObjectSet<>(used);

            var colors = graphViewSettingsColors;
            colors.clear();
            colors.left();
            colors.button("@waves.units.hide", Styles.flatt, () -> {
                if(hidden.size == usedCopy.size){
                    hidden.clear();
                }else{
                    hidden.addAll(usedCopy);
                }

                used.clear();
                used.addAll(usedCopy);
                for(UnitType o : hidden) used.remove(o);
            }).update(b -> b.setText(hidden.size == usedCopy.size ? "@waves.units.show" : "@waves.units.hide")).height(32f).width(130f);
            colors.pane(t -> {
                t.left();
                for(UnitType type : used.toSeq().sort(u -> u.id)){
                    t.button(b -> {
                        Color tcolor = color(type).cpy();
                        b.image().size(32f).update(i -> i.setColor(b.isChecked() ? Tmp.c1.set(tcolor).mul(0.5f) : tcolor)).get().act(1);
                        b.image(type.uiIcon).size(32f).padRight(20).update(i -> i.setColor(b.isChecked() ? Color.gray : Color.white)).get().act(1);
                        b.margin(0f);
                    }, Styles.fullTogglet, () -> {
                        if(!hidden.add(type)){
                            hidden.remove(type);
                        }

                        used.clear();
                        used.addAll(usedCopy);
                        for(UnitType o : hidden) used.remove(o);
                    }).update(b -> b.setChecked(hidden.contains(type)));
                }
            }).scrollY(false);

            for(UnitType type : hidden){
                used.remove(type);
            }
        }

        int nextStep(float value){
            int order = 1;
            while(order < value){
                if(order * 2 > value){
                    return order * 2;
                }
                if(order * 5 > value){
                    return order * 5;
                }
                if(order * 10 > value){
                    return order * 10;
                }
                order *= 10;
            }
            return order;
        }

        public void addGroup(SpawnGroup group){
            addChild(new SpawnGroupBar(group, children.size));
        }

        public class SpawnGroupBar extends Table{
            static float marginTop = 4f;
            SpawnGroup group;
            int index;
            boolean dragging;

            public SpawnGroupBar(SpawnGroup group, int i){
                this.group = group;
                this.index = i;
                margin(2f);
                background(Tex.whiteui);
                touchable = Touchable.enabled;
                setColor(color(group.type));

                this.addCaptureListener(new InputListener(){
                    float downx, downy;

                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                        downx = x;
                        downy = y;
                        dragging = true;
                        return true;
                    }

                    @Override
                    public void touchDragged(InputEvent event, float x, float y, int pointer){
                        selectedGroups.clear();
                        buildConfig();
                        setTranslation((x - downx) + translation.x, (y - downy) + translation.y);
                        super.touchDragged(event, x, y, pointer);
                    }

                    @Override
                    public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                        super.touchUp(event, x, y, pointer, button);
                        //dragging begin & end
                        if(!pinching){
                            int gap = group.end - group.begin;
                            float dx = x - downx + translation.x;
                            if(Math.abs(dx) > tileW){
                                group.begin += Mathf.round(dx / tileW);
                                if(group.end != never) group.end += Mathf.round(dx / tileW);
                            }
                            group.begin = Math.max(group.begin, 0);
                            group.end = Math.max(group.end, gap);
                            //dragging up down
                            float dy = y - downy + translation.y;
                            if(Math.abs(dy) > tileH){
                                int newIndex = Mathf.clamp(index + Mathf.round(dy / tileH), 0, groups.size - 1);
                                groups.remove(group, true);
                                groups.insert(newIndex, group);
                                buildGroups();
                            }
                        }

                        setTranslation(0f, 0f);
                        dragging = false;
                    }
                });

                clicked(() -> {
                    if(!batchEditing){
                        if(!selectedGroups.remove(group)) selectedGroups.clear().add(group);
                    }else{
                        if(!selectedGroups.remove(group)) selectedGroups.add(group);
                    }
                    buildConfig();
                });

                label(() -> String.valueOf(group.begin + 1));
                table(t -> {
                    Image img;
                    Table lt;
                    img = new Image(group.type.uiIcon);
                    img.setScaling(Scaling.fit).setAlign(Align.left);
                    if(detailedBar){
                        lt = new Table(ls -> {
                            ls.label(() -> "" + group.unitAmount).growX().row();
                            ls.label(() -> "+" + Strings.autoFixed(1f/group.unitScaling, 2)).fontScale(0.75f).growX();
                        });
                        stack(img, lt).height(32f).maxWidth(64f);

                        img = new Image(Icon.defense);
                        img.setColor(Pal.shield);
                        img.setScaling(Scaling.fit).setAlign(Align.left);
                        lt = new Table(ls -> {
                            ls.label(() -> "" + UI.formatAmount((long)group.shields)).growX().labelAlign(Align.right).row();
                            ls.label(() -> "+" + Strings.autoFixed(group.shieldScaling, 1)).growX().labelAlign(Align.right).fontScale(0.75f);
                        });
                        stack(img, lt).height(32f).maxWidth(64f);
                    }else{
                        add(img);
                    }

                    if(group.effect != null) image(group.effect.uiIcon).size(32f).scaling(Scaling.fit);

                    if(group.items != null){
                        var label = new Label(String.valueOf(group.items.amount));
                        label.setAlignment(Align.bottomRight);
                        label.setFillParent(true);
                        label.setFontScale(0.75f);
                        stack(new Image(group.items.item.uiIcon).setScaling(Scaling.fit), label).size(32f);
                    }

                    label(() -> (group.payloads == null || group.payloads.isEmpty() ? "" : Iconc.itchio + "") + (group.spawn == -1 ? "" : Iconc.blockSpawn + ""));
                }).grow();
                label(() -> group.end == SpawnGroup.never ? "∞" : String.valueOf(group.end + 1));
            }

            @Override
            public void act(float delta){
                super.act(delta);
                if(barFillX){
                    setSize(WaveCanvas.this.getWidth(), tileH);
                    setPosition(tileX(start), -camera.y + tileH * index + 16f);
                }else{
                    setSize(Mathf.clamp(tileX(group.end + (group.end == never ? 0 : 1)) - tileX(group.begin), getPrefWidth(), WaveCanvas.this.getWidth()), tileH - marginTop);
                    setPosition(Math.min(tileX(group.begin), WaveCanvas.this.getWidth() - getWidth()), -camera.y + tileH * index + 16f);
                }
            }

            @Override
            protected void drawBackground(float x, float y){
                boolean selected = selectedGroups.contains(group, true);
                Tmp.c2.set(color);
                color.set(Color.white).a(selected ? 0.7f : 0.25f);
                super.drawBackground(x, y);
                color.set(Tmp.c2);

                //spacing block
                Draw.color(selected ? Pal.accent : dragging ? Color.royal : color);
                Draw.alpha(0.6f * parentAlpha);
                Fill.rect(Tmp.r1.set(tileX(group.begin), y, tileX(group.end) + tileW - tileX(group.begin), tileH - marginTop).move(0f, -translation.y));

                //spawning block
                Draw.alpha(this.parentAlpha);
                for(int i = group.begin; i <= group.end && i <= end; i += group.spacing){
                    if(i < start) continue;
                    Fill.rect(Tmp.r1.set(tileX(i), y, tileX(i + 1) - tileX(i), tileH - marginTop).move(0f, -translation.y));
                }
                Draw.reset();
                color.a(1f);
            }
        }

        public void screenshot(){
            float scale = 0.6f;
            int w = (end + 1) * (int)tileW, h = (groups.size + 1) * (int)tileH;
            int memory = w * h / 1024 / 1024;

            if(Vars.checkScreenshotMemory && memory >= (mobile ? 120 : 240)){
                ui.showInfo("@screenshot.invalid");
                return;
            }

            int pixw = (int)(w * scale), pixh = (int)(h * scale);
            FrameBuffer buffer = new FrameBuffer(pixw, pixh);

            float cw = this.width, ch = this.height, cx = camera.x, cy = camera.y;
            setSize(w, h);
            camera.setZero();
            vec.setZero();
            start = 0;
            act(0.1f);

            buffer.begin();
            Draw.proj().setOrtho(0,0, w, h);
            Styles.black.draw(0f, 0f, w, h);
            draw();
            Draw.flush();
            byte[] lines = ScreenUtils.getFrameBufferPixels(0, 0, pixw, pixh, true);
            buffer.end();

            setSize(cw, ch);
            camera.set(cx, cy);
            buffer.dispose();

            Threads.thread(() -> {
                for(int i = 0; i < lines.length; i += 4){
                    lines[i + 3] = (byte)255;
                }
                Pixmap fullPixmap = new Pixmap(pixw, pixh);
                Buffers.copy(lines, 0, fullPixmap.pixels, lines.length);
                Fi file = screenshotDirectory.child("screenshot-" + Time.millis() + ".png");
                PixmapIO.writePng(file, fullPixmap);
                fullPixmap.dispose();
                Core.app.post(() -> ui.showInfoFade(Core.bundle.format("screenshot", file.toString())));
            });
        }
    }

    public enum Mode{
        counts, totals, health;

        static Mode[] all = values();
    }

    public enum Sort{
        begin(g -> g.begin, g -> g.type.id),
        end(g -> g.end, g -> g.type.id),
        type(g -> g.type.id);

        static final MFEWaveInfoDialog.Sort[] all = values();

        final Floatf<SpawnGroup> sort, secondary;

        Sort(Floatf<SpawnGroup> sort){
            this(sort, g -> g.begin);
        }

        Sort(Floatf<SpawnGroup> sort, Floatf<SpawnGroup> secondary){
            this.sort = sort;
            this.secondary = secondary;
        }
    }
}
