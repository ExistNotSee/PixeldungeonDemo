/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.watabou.pixeldungeon.scenes;

import android.opengl.GLES20;

import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.RenderedText;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.ui.Button;
import com.watabou.pixeldungeon.Assets;
import com.watabou.pixeldungeon.PixelDungeon;
import com.watabou.pixeldungeon.effects.BannerSprites;
import com.watabou.pixeldungeon.effects.Fireball;
import com.watabou.pixeldungeon.ui.Archs;
import com.watabou.pixeldungeon.ui.ExitButton;
import com.watabou.pixeldungeon.ui.PrefsButton;

import javax.microedition.khronos.opengles.GL10;

public class TitleScene extends PixelScene {

    private static final String TXT_PLAY = "开始";
    private static final String TXT_HIGHSCORES = "排行榜";
    private static final String TXT_BADGES = "成就";
    private static final String TXT_ABOUT = "关于";

    @Override
    public void create() {
        super.create();
        Music.INSTANCE.play(Assets.THEME, true);
        Music.INSTANCE.volume(1f);
        uiCamera.visible = false;
        int w = Camera.main.width;
        int h = Camera.main.height;

        //游戏菜单界面背景
        Archs archs = new Archs();
        archs.setSize(w, h);
        add(archs);
        //游戏标题
        Image title = BannerSprites.get(BannerSprites.Type.PIXEL_DUNGEON);
        add(title);
        float height = title.height + (PixelDungeon.landscape() ? DashboardItem.SIZE : DashboardItem.SIZE * 2);
        title.x = (w - title.width()) / 2;
        title.y = (h - height) / 2;
        //火炬动画
        placeTorch(title.x + 18, title.y + 20);
        placeTorch(title.x + title.width - 18, title.y + 20);

        //给游戏标题增加一个动画效果
        Image signs = new Image(BannerSprites.get(BannerSprites.Type.PIXEL_DUNGEON_SIGNS)) {
            private float time = 0;

            @Override
            public void update() {
                super.update();
                am = (float) Math.sin(-(time += Game.elapsed));
            }

            @Override
            public void draw() {
                GLES20.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
                super.draw();
                GLES20.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
            }
        };
        signs.x = title.x;
        signs.y = title.y;
        add(signs);
        DashboardItem btnPlay = addDashboardItem(TXT_PLAY, 0, StartScene.class);
        DashboardItem btnAbout = addDashboardItem(TXT_ABOUT, 1, AboutScene.class);
        DashboardItem btnHighScores = addDashboardItem(TXT_HIGHSCORES, 2, RankingsScene.class);
        DashboardItem btnBadges = addDashboardItem(TXT_BADGES, 3, BadgesScene.class);

        if (PixelDungeon.landscape()) {
            float y = (h + height) / 2 - DashboardItem.SIZE;
            btnHighScores.setPos(w / 2f - btnHighScores.width(), y);
            btnBadges.setPos(w / 2f, y);
            btnPlay.setPos(btnHighScores.left() - btnPlay.width(), y);
            btnAbout.setPos(btnBadges.right(), y);
        } else {
            btnBadges.setPos(w / 2f - btnBadges.width(), (h + height) / 2 - DashboardItem.SIZE);
            btnAbout.setPos(w / 2f, (h + height) / 2 - DashboardItem.SIZE);
            btnPlay.setPos(w / 2f - btnPlay.width(), btnAbout.top() - DashboardItem.SIZE);
            btnHighScores.setPos(w / 2f, btnPlay.top());
        }

        BitmapText version = new BitmapText("v " + Game.version, font1x);
        version.measure();
        version.hardlight(0x888888);
        version.x = w - version.width();
        version.y = h - version.height();
        add(version);

        PrefsButton btnPrefs = new PrefsButton();
        btnPrefs.setPos(0, 0);
        add(btnPrefs);

        ExitButton btnExit = new ExitButton();
        btnExit.setPos(w - btnExit.width(), 0);
        add(btnExit);

        fadeIn();
    }

    private DashboardItem addDashboardItem(String txt, int index, Class<? extends PixelScene> c) {
        return (DashboardItem) add(new DashboardItem(txt, index) {
            @Override
            protected void onClick() {
                PixelDungeon.switchNoFade(c);
            }
        });
    }

    private void placeTorch(float x, float y) {
        Fireball fb = new Fireball();
        fb.setPos(x, y);
        add(fb);
    }

    private static class DashboardItem extends Button {

        public static final float SIZE = 48;

        private static final int IMAGE_SIZE = 32;

        private Image image;
        private RenderedText label;

        public DashboardItem(String text, int index) {
            super();

            image.frame(image.texture.uvRect(index * IMAGE_SIZE, 0, (index + 1) * IMAGE_SIZE, IMAGE_SIZE));
            this.label.text(text);

            setSize(SIZE, SIZE);
        }

        @Override
        protected void createChildren() {
            super.createChildren();

            image = new Image(Assets.DASHBOARD);
            add(image);

            label = renderText(9);
            add(label);
        }

        @Override
        protected void layout() {
            super.layout();

            image.x = align(x + (width - image.width()) / 2);
            image.y = align(y);

            label.x = align(x + (width - label.width()) / 2);
            label.y = align(image.y + image.height() + 2);
        }

        @Override
        protected void onTouchDown() {
            image.brightness(1.5f);
            Sample.INSTANCE.play(Assets.SND_CLICK, 1, 1, 0.8f);
        }

        @Override
        protected void onTouchUp() {
            image.resetColor();
        }
    }
}
