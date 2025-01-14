/*
 * Copyright (c) 2015, Garrett Benoit. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

package keyboard.tablet;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import swipe.SwipeKeyboard;
import utilities.Point;

import javax.media.opengl.GL2;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JPanel;

import com.leapmotion.leap.Vector;

import utilities.MyUtilities;
import keyboard.IKeyboard;
import keyboard.renderables.SwipeTrail;
import keyboard.renderables.SwipePoint;
import keyboard.renderables.VirtualKeyboard;
import enums.Attribute;
import enums.Key;
import enums.KeyboardType;
import enums.Renderable;
import experiment.WordManager;
import experiment.data.DataManager;
import experiment.data.TabletDataObserver;
import experiment.playback.PlaybackManager;
import experiment.playback.TabletPlaybackObserver;

public class TabletKeyboard extends IKeyboard implements TabletPlaybackObserver {
    private final float CAMERA_DISTANCE;
    private final ReentrantLock TABLET_LOCK = new ReentrantLock();
    private ArrayList<TabletDataObserver> observers = new ArrayList<TabletDataObserver>();
    private SwipePoint swipePoint;
    private SwipeTrail swipeTrail;
    private SwipeKeyboard swipeKeyboard;
    private VirtualKeyboard virtualKeyboard;
    private TouchScreen touchScreen;
    private boolean isCalibrated = false;
    private boolean shiftOnce = false;
    private boolean shiftTwice = false;
    
    public TabletKeyboard(KeyboardType keyboardType) {
        super(keyboardType);
        keyboardAttributes = new TabletAttributes(this);
        keyboardSettings = new TabletSettings(this);
        this.loadDefaultSettings();
        keyboardRenderables = new TabletRenderables(this);
        keyboardSize = keyboardAttributes.getAttributeAsPoint(Attribute.KEYBOARD_SIZE);
        float borderSize = keyboardAttributes.getAttributeAsFloat(Attribute.BORDER_SIZE) * 2;
        imageSize = new Point(keyboardSize.x + borderSize, keyboardSize.y + borderSize);
        CAMERA_DISTANCE = keyboardAttributes.getAttributeAsFloat(Attribute.CAMERA_DISTANCE);
        virtualKeyboard = (VirtualKeyboard) keyboardRenderables.getRenderable(Renderable.VIRTUAL_KEYBOARD);
        swipePoint = (SwipePoint) keyboardRenderables.getRenderable(Renderable.SWIPE_POINT);
        swipeTrail = (SwipeTrail) keyboardRenderables.getRenderable(Renderable.SWIPE_TRAIL);
        swipeKeyboard = new SwipeKeyboard(this);
        touchScreen = new TouchScreen();
    }
    
    @Override
    public void render(GL2 gl) {
        MyUtilities.OPEN_GL_UTILITIES.switchToPerspective(gl, this, true);
        gl.glPushMatrix();
        gl.glTranslatef(-imageSize.x/2f, -imageSize.y/2f, -CAMERA_DISTANCE);
        keyboardRenderables.render(gl);
        gl.glPopMatrix();
    }
    
    @Override
    public void update() {
        if(isPlayingBack()) {
            playbackManager.update();
            
            boolean isTouching = false;
            // Set add to trail and set location.
            if(!swipePoint.getNormalizedPoint().equals(Vector.zero())) {
                swipeTrail.update(swipePoint.getNormalizedPoint());
                isTouching = true;
            } else {
                swipeTrail.update();
            }
            
            swipeKeyboard.update(isTouching);
        } else {
            // Allow the touchscreen to take over the updates of specific objects that require the screen
            touchScreen.update(swipePoint, swipeTrail);
            
            notifyListenersTouchEvent(swipePoint.getNormalizedPoint()); // record even when zero
            
            swipeKeyboard.update(touchScreen.isTouching());
        }

        Key key;
        if((key = swipeKeyboard.isPressed()) != Key.VK_NULL) {
            if(key != Key.VK_SHIFT) {
                if(shiftOnce) {
                    keyPressed = key.toUpper();
                    shiftOnce = shiftTwice;
                    if(!shiftTwice) {
                        keyboardRenderables.swapToLowerCaseKeyboard();
                    }
                } else {
                    keyPressed = key.getValue();   
                }
                if(!isPlayingBack()) {
                    notifyListenersKeyEvent();
                }
            } else if(!shiftOnce && !shiftTwice) {
                shiftOnce = true;
                keyboardRenderables.swapToUpperCaseKeyboard();
            } else if(shiftOnce && !shiftTwice) {
                shiftTwice = true;
            } else {
                shiftTwice = false;
                shiftOnce = false;
                keyboardRenderables.swapToLowerCaseKeyboard();
            }
        }
        if(shiftTwice) {
            virtualKeyboard.locked(Key.VK_SHIFT);
        } else if(shiftOnce) {
            virtualKeyboard.pressed(Key.VK_SHIFT);
        }
        
        // Since we removed the Enter key, we simulate it on releasing our touch.
        if(swipeKeyboard.isTouchReleased()) {
            keyPressed = Key.VK_ENTER.getValue();
            notifyListenersKeyEvent();
        }
    }
    
    @Override
    public void addToUI(JPanel panel, GLCanvas canvas) {
        WordManager.registerObserver(swipeKeyboard);
        canvas.addMouseListener(touchScreen);
        canvas.addMouseMotionListener(touchScreen);
    }

    @Override
    public void removeFromUI(JPanel panel, GLCanvas canvas) {
        WordManager.removeObserver(swipeKeyboard);
        canvas.removeMouseListener(touchScreen);
        canvas.removeMouseMotionListener(touchScreen);
    }
    
    public void registerObserver(TabletDataObserver observer) {
        if(observers.contains(observer)) {
            return;
        }
        observers.add(observer);
    }
    
    public void removeObserver(TabletDataObserver observer) {
        observers.remove(observer);
    }

    protected void notifyListenersTouchEvent(Vector touchPoint) {
        for(TabletDataObserver observer : observers) {
            observer.tabletDataEventObserved(touchPoint);
        }
    }
    
    @Override
    protected boolean isPlayingBack() {
        TABLET_LOCK.lock();
        try {
            return isPlayback;
        } finally {
            TABLET_LOCK.unlock();
        }
    }
    
    @Override
    public void beginPlayback(PlaybackManager playbackManager) {
        TABLET_LOCK.lock();
        try {
            isPlayback = true;
            playbackManager.registerObserver(this);
            this.playbackManager = playbackManager;
        } finally {
            TABLET_LOCK.unlock();
        }
    }
    
	@Override
	public void pressedEventObserved(Key key) {
        keyPressed = key.getValue();
        notifyListenersKeyEvent();
	}

	@Override
	public void touchEventObserved(Vector touchPoint) {
        // Add the touch point from the screen.
        swipePoint.setNormalizedPoint(touchPoint);
	}
    
    @Override
    public void finishPlayback(PlaybackManager playbackManager) {
        TABLET_LOCK.lock();
        try {
            playbackManager.removeObserver(this);
            isPlayback = false;
            this.playbackManager = null;
        } finally {
            TABLET_LOCK.unlock();
        }
    }
    
    @Override
    public void beginExperiment(DataManager dataManager) {
        TABLET_LOCK.lock();
        try {
            this.registerObserver(dataManager);
        } finally {
            TABLET_LOCK.unlock();
        }
    }
    
    @Override
    public void finishExperiment(DataManager dataManager) {
        TABLET_LOCK.lock();
        try {
            this.removeObserver(dataManager);
        } finally {
            TABLET_LOCK.unlock();
        }
    }

    @Override
    public void beginCalibration(JPanel textPanel) {
        finishCalibration();
    }

    @Override
    protected void finishCalibration() {
        isCalibrated = true;
        notifyListenersCalibrationFinished();
    }

    @Override
    public boolean isCalibrated() {
        return isCalibrated;
    }
    
    private class TouchScreen implements MouseListener, MouseMotionListener {
        private final ReentrantLock TOUCH_LOCK = new ReentrantLock();
        private final Vector touchPoint = new Vector();
        private boolean isTouching = false;
        private boolean isLastPoint = false;
        
        public void update(SwipePoint swipePoint, SwipeTrail swipeTrail) {
            TOUCH_LOCK.lock();
            try {
                // Add the touch point from the screen.
                if(isTouching) {
                    swipePoint.setNormalizedPoint(touchPoint);
                } else {
                    swipePoint.setNormalizedPoint(Vector.zero());
                }
                
                // Set add to trail and set location.
                if(isTouching) {
                    swipeTrail.update(swipePoint.getNormalizedPoint());
                } else {
                    swipeTrail.update();
                }
                
                // Check to see if this is the last point, to set touch to false.
                if(isLastPoint()) {
                    isTouching = false;
                }
            } finally {
                TOUCH_LOCK.unlock();
            }
        }
        
        private boolean isLastPoint() {
            try {
                return isLastPoint;
            } finally {
                // consume released event
                isLastPoint = false;
            }
        }
        
        public boolean isTouching() {
            TOUCH_LOCK.lock();
            try {
                return isTouching;
            } finally {
                TOUCH_LOCK.unlock();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            TOUCH_LOCK.lock();
            try {
                isTouching = true;
                touchPoint.setX(e.getX());
                touchPoint.setY(imageSize.y - e.getY());
            } finally {
                TOUCH_LOCK.unlock();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            TOUCH_LOCK.lock();
            try {
                isLastPoint = true;
                touchPoint.setX(e.getX());
                touchPoint.setY(imageSize.y - e.getY());
            } finally {
                TOUCH_LOCK.unlock();
            }
        }
        
        @Override
        public void mouseDragged(MouseEvent e) {
            TOUCH_LOCK.lock();
            try {
                touchPoint.setX(e.getX());
                touchPoint.setY(imageSize.y - e.getY());
            } finally {
                TOUCH_LOCK.unlock();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            // Do nothing when only moving the mouse.
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
            // Do nothing on quick click.
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            // Do nothing on mouse entered.
        }

        @Override
        public void mouseExited(MouseEvent e) {
            // Do nothing on mouse exited.
        }
    }
}
