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

package keyboard.standard;

import utilities.Point;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.opengl.GL2;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import com.leapmotion.leap.Vector;

import utilities.MyUtilities;
import enums.Attribute;
import enums.Gesture;
import enums.Direction;
import enums.KeyboardType;
import enums.Renderable;
import enums.Key;
import experiment.WordManager;
import experiment.WordObserver;
import experiment.data.DataManager;
import experiment.playback.PlaybackManager;
import experiment.playback.PlaybackObserver;
import keyboard.IKeyboard;
import keyboard.KeyboardGesture;
import keyboard.renderables.KeyboardGestures;
import keyboard.renderables.VirtualKeyboard;

public class StandardKeyboard extends IKeyboard implements PlaybackObserver, WordObserver {
    private static final int AUTO_REPEAT_RATE = 1000 / 31; // Windows default
    private final float HORIZONTAL_GESTURE_LENGTH = 125f;
    private final float VERTICAL_GESTURE_LENGTH;
    private final float HORIZONTAL_GESTURE_OFFSET = 25f;
    private final float VERTICAL_GESTURE_OFFSET;
    private final float CAMERA_DISTANCE;
    private final ReentrantLock STANDARD_LOCK = new ReentrantLock();
    private VirtualKeyboard virtualKeyboard;
    private KeyBindings keyBindings;
    private KeyboardGestures keyboardGestures;
    private MouseGesture mouseGesture;
    private boolean shiftDown = false;
    private boolean isCalibrated = false;
    private ReentrantLock gestureLock;
    private Timer detectedMatchTimer;
    
    public StandardKeyboard(KeyboardType keyboardType) {
        super(keyboardType);
        keyboardAttributes = new StandardAttributes(this);
        keyboardSettings = new StandardSettings(this);
        this.loadDefaultSettings();
        keyboardRenderables = new StandardRenderables(this);
        keyboardSize = keyboardAttributes.getAttributeAsPoint(Attribute.KEYBOARD_SIZE);
        float borderSize = keyboardAttributes.getAttributeAsFloat(Attribute.BORDER_SIZE) * 2;
        imageSize = new Point(keyboardSize.x + borderSize, keyboardSize.y + borderSize);
        CAMERA_DISTANCE = keyboardAttributes.getAttributeAsFloat(Attribute.CAMERA_DISTANCE);
        virtualKeyboard = (VirtualKeyboard) keyboardRenderables.getRenderable(Renderable.VIRTUAL_KEYBOARD);
        keyBindings = new KeyBindings();
        if(Gesture.ENABLED) {
            gestureLock = new ReentrantLock();
            keyboardGestures = (KeyboardGestures) keyboardRenderables.getRenderable(Renderable.KEYBOARD_GESTURES);
            mouseGesture = new MouseGesture();
        }
        VERTICAL_GESTURE_LENGTH = HORIZONTAL_GESTURE_LENGTH * (imageSize.y/(float)imageSize.x);
        VERTICAL_GESTURE_OFFSET = HORIZONTAL_GESTURE_OFFSET * (imageSize.y/(float)imageSize.x);
        
        detectedMatchTimer = new Timer(AUTO_REPEAT_RATE, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                detectedMatchTimer.stop();
                keyPressed = Key.VK_ENTER.getValue();
                notifyListenersKeyEvent();
            }
        });
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
        } else {
            if(Gesture.ENABLED) {
                // Remove completed gestures, update the others.
                gestureLock.lock();
                try {
                    keyboardGestures.removeAndUpdateGestures();
                } finally {
                    gestureLock.unlock();
                }
            }
            
            if(shiftDown) {
                virtualKeyboard.pressed(Key.VK_SHIFT_LEFT);
                virtualKeyboard.pressed(Key.VK_SHIFT_RIGHT);
            }
        }
    }
    
    @Override
    public void addToUI(JPanel panel, GLCanvas canvas) {
        WordManager.registerObserver(this);
        panel.add(keyBindings);
        if(Gesture.ENABLED) {
            canvas.addMouseListener(mouseGesture);
            canvas.addMouseMotionListener(mouseGesture);
        }
    }

    @Override
    public void removeFromUI(JPanel panel, GLCanvas canvas) {
        WordManager.removeObserver(this);
        panel.remove(keyBindings);
        if(Gesture.ENABLED) {
            canvas.removeMouseListener(mouseGesture);
            canvas.removeMouseMotionListener(mouseGesture);
            keyboardGestures.deleteQuadric();
        }
    }
    
    @Override
    protected boolean isPlayingBack() {
        STANDARD_LOCK.lock();
        try {
            return isPlayback;
        } finally {
            STANDARD_LOCK.unlock();
        }
    }
    
    @Override
    public void beginPlayback(PlaybackManager playbackManager) {
        STANDARD_LOCK.lock();
        try {
            isPlayback = true;
            playbackManager.registerObserver(this);
            this.playbackManager = playbackManager;
        } finally {
            STANDARD_LOCK.unlock();
        }
    }
    
	@Override
	public void pressedEventObserved(Key key) {
	    keyPressed = key.getValue();
	    notifyListenersKeyEvent();
	    virtualKeyboard.pressed(key);
	}
    
    @Override
    public void finishPlayback(PlaybackManager playbackManager) {
        STANDARD_LOCK.lock();
        try {
            playbackManager.removeObserver(this);
            isPlayback = false;
            this.playbackManager = null;
        } finally {
            STANDARD_LOCK.unlock();
        }
    }
    
    @Override
    public void beginExperiment(DataManager dataManager) {
        // No special data needs to be recorded for Standard Keyboard.
    }
    
    @Override
    public void finishExperiment(DataManager dataManager) {
        // No special data needs to be recorded for Standard Keyboard.
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
    
    @Override
    public void wordSetEventObserved(String word) {
        // Do nothing
    }

    @Override
    public void currentLetterIndexChangedEventObservered(int letterIndex, Key key) {
        // Do nothing
    }

    @Override
    public void matchEventObserved() {
        if(!isPlayingBack()) {
            // Start timer for matched event
            detectedMatchTimer.start();
        }
    }
    
    public KeyboardGesture createSwipeGesture(Direction direction) {
        if(Gesture.ENABLED) {
            KeyboardGesture gesture = null;
            switch(direction) {
                case UP:
                    gesture = new KeyboardGesture(new Vector(imageSize.x/2f, imageSize.y/2f + VERTICAL_GESTURE_OFFSET, CAMERA_DISTANCE * 0.5f), Gesture.SWIPE);
                    gesture.update(new Vector(imageSize.x/2f, imageSize.y/2f + VERTICAL_GESTURE_LENGTH, CAMERA_DISTANCE * 0.5f));
                    gesture.gestureFinshed();
                    break;
                case DOWN:
                    gesture = new KeyboardGesture(new Vector(imageSize.x/2f, imageSize.y/2f - VERTICAL_GESTURE_OFFSET, CAMERA_DISTANCE * 0.5f), Gesture.SWIPE);
                    gesture.update(new Vector(imageSize.x/2f, imageSize.y/2f - VERTICAL_GESTURE_LENGTH, CAMERA_DISTANCE * 0.5f));
                    gesture.gestureFinshed();
                    break;
                case LEFT:
                    gesture = new KeyboardGesture(new Vector(imageSize.x/2f - HORIZONTAL_GESTURE_OFFSET, imageSize.y/2f, CAMERA_DISTANCE * 0.5f), Gesture.SWIPE);
                    gesture.update(new Vector(imageSize.x/2f - HORIZONTAL_GESTURE_LENGTH, imageSize.y/2f, CAMERA_DISTANCE * 0.5f));
                    gesture.gestureFinshed();
                    break;
                case RIGHT:
                    gesture = new KeyboardGesture(new Vector(imageSize.x/2f + HORIZONTAL_GESTURE_OFFSET, imageSize.y/2f, CAMERA_DISTANCE * 0.5f), Gesture.SWIPE);
                    gesture.update(new Vector(imageSize.x/2f + HORIZONTAL_GESTURE_LENGTH, imageSize.y/2f, CAMERA_DISTANCE * 0.5f));
                    gesture.gestureFinshed();
                    break;
                default: break;
            }
            return gesture;
        }
        return null;
    }
    
    public KeyboardGesture findClosestGesture(Vector direction) {
        return null;
    }
    
    @SuppressWarnings("serial")
    private class KeyBindings extends JPanel {
        
        public KeyBindings() {
            setKeyBindings();
        }
        
        private void setKeyBindings() {
            ActionMap actionMap = getActionMap();
            InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            
            for(Key key: Key.values()) {
                if((key.isPrintable() || (key.isArrow() && Gesture.ENABLED)) && /* Removing Special keys */ (!key.isSpecial() || key == Key.VK_BACK_SPACE)) {
                    // Add normal keys to input map
                    inputMap.put(KeyStroke.getKeyStroke(key.getCode(), 0), key.getName());
                    
                    // Add shifted keys to input map
                    //inputMap.put(KeyStroke.getKeyStroke(key.getCode(), KeyEvent.SHIFT_DOWN_MASK, false), key.getName() + Key.VK_SHIFT.getName());
                    
                    // Add normal keys to action map
                    actionMap.put(key.getName(), new KeyAction(key.getValue()));
                    
                    // Add shifted keys to action map
                    //actionMap.put(key.getName() + Key.VK_SHIFT.getName(), new KeyAction(key.toUpper()));
                }
            }
            /*inputMap.put(KeyStroke.getKeyStroke(Key.VK_SHIFT.getCode(), KeyEvent.SHIFT_DOWN_MASK, false), Key.VK_SHIFT.getName());
            actionMap.put(Key.VK_SHIFT.getName(), new KeyAction(Key.VK_SHIFT.toUpper()));
            inputMap.put(KeyStroke.getKeyStroke(Key.VK_SHIFT.getCode(), 0, true), Key.VK_SHIFT_RELEASED.getName());
            actionMap.put(Key.VK_SHIFT_RELEASED.getName(), new KeyAction(Key.VK_SHIFT_RELEASED.getValue()));*/
        }
        
        private class KeyAction extends AbstractAction {
            public KeyAction(char actionCommand) {
                putValue(ACTION_COMMAND_KEY, actionCommand);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if(!isPlayingBack()) {
                    detectedMatchTimer.stop();
                    keyPressed = e.getActionCommand().charAt(0);
                    Key key = null;
                    if((key = Key.getByCode(keyPressed)) == null) {
                        key = Key.getByValue(keyPressed);
                    }
                    if((e.getModifiers() & KeyEvent.SHIFT_MASK) != 0 || shiftDown) {
                        shiftDown = true;
                        //keyboardRenderables.swapToUpperCaseKeyboard();
                    }
                    virtualKeyboard.pressed(key);
                    if(key.isPrintable()) {
                        notifyListenersKeyEvent();
                    } else {
                        if(Gesture.ENABLED) {
                            gestureLock.lock();
                            try {
                                switch(key) {
                                    case VK_UP:
                                        keyboardGestures.addGesture(createSwipeGesture(Direction.UP));
                                        break;
                                    case VK_DOWN:
                                        keyboardGestures.addGesture(createSwipeGesture(Direction.DOWN));
                                        break;
                                    case VK_LEFT:
                                        keyboardGestures.addGesture(createSwipeGesture(Direction.LEFT));
                                        break;
                                    case VK_RIGHT:
                                        keyboardGestures.addGesture(createSwipeGesture(Direction.RIGHT));
                                        break;
                                    case VK_SHIFT_RELEASED:
                                        shiftDown = false;
                                        //keyboardRenderables.swapToLowerCaseKeyboard();
                                        break;
                                    default: break;
                                }
                            } finally {
                                gestureLock.unlock();
                            }
                        } else if(key == Key.VK_SHIFT_RELEASED) {
                            shiftDown = false;
                            //keyboardRenderables.swapToLowerCaseKeyboard();
                        }
                    }
                }
            }
        }
    }
    
    private class MouseGesture implements MouseListener, MouseMotionListener {
        private KeyboardGesture gesture;
        private float velocity = 0;
        private long previousTime;
        private long elapsedTime;
        
        @SuppressWarnings("unused")
        public float getVelocity() {
            return velocity;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if(Gesture.ENABLED) {
                Vector start = new Vector(e.getX() + imageSize.x*0.5f, -e.getY() + imageSize.y*1.5f, CAMERA_DISTANCE);
                gesture = new KeyboardGesture(start.times(0.5f), Gesture.SWIPE);
                gestureLock.lock();
                try {
                    keyboardGestures.addGesture(gesture);
                } finally {
                    gestureLock.unlock();
                }
                velocity = 0;
                previousTime = System.currentTimeMillis();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if(Gesture.ENABLED) {
                gesture.gestureFinshed();
                gesture = null;
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if(Gesture.ENABLED) {
                Vector dest = new Vector(e.getX() + imageSize.x*0.5f, -e.getY() + imageSize.y*1.5f, CAMERA_DISTANCE);
                
                dest = dest.times(0.5f);
                long now = System.currentTimeMillis();
                elapsedTime = now - previousTime;
                previousTime = now;
    
                if(elapsedTime != 0) {
                    velocity = gesture.getDestination().minus(dest).magnitude() / (elapsedTime/1000f);
                } else {
                    velocity = 0;
                }
                
                gestureLock.lock();
                try {
                    gesture.update(dest);
                } finally {
                    gestureLock.unlock();
                }
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
            // Do nothing on mouse enter
        }

        @Override
        public void mouseExited(MouseEvent e) {
            // Do nothing on mouse exit.
        }
    }
}
