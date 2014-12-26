package keyboard.standard;

import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.media.opengl.GL2;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import enums.AttributeName;
import enums.FilePath;
import enums.RenderableName;
import enums.Key;
import keyboard.IKeyboard;
import keyboard.KeyboardAttribute;
import keyboard.renderables.VirtualKeyboard;

public class StandardKeyboard extends IKeyboard {
    public static final int KEYBOARD_ID = 0;
    private static final String KEYBOARD_FILE_PATH = FilePath.STANDARD_PATH.getPath();
    private VirtualKeyboard virtualKeyboard;
    private KeyBindings keyBindings;
    private boolean shiftDown = false;
    
    public StandardKeyboard() {
        super(KEYBOARD_ID, KEYBOARD_FILE_PATH);
        keyboardAttributes = new StandardAttributes(this);
        keyboardSettings = new StandardSettings(this);
        keyboardRenderables = new StandardRenderables(this);
        keyboardWidth = keyboardAttributes.getAttributeByName(AttributeName.KEYBOARD_WIDTH.toString());
        keyboardHeight = keyboardAttributes.getAttributeByName(AttributeName.KEYBOARD_HEIGHT.toString());
        virtualKeyboard = (VirtualKeyboard) keyboardRenderables.getRenderableByName(RenderableName.VIRTUAL_KEYS.toString());
        keyBindings = new KeyBindings();
        keyboardAttributes.addAttribute(new KeyboardAttribute(AttributeName.KEY_BINDINGS.toString(), keyBindings));
    }
    
    @Override
    public void render(GL2 gl) {
        // Setup ortho projection, with aspect ratio matches viewport
        gl.glMatrixMode(GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0, keyboardWidth.getValueAsInteger(), 0, keyboardHeight.getValueAsInteger(), 0.1, 1000);
   
        // Enable the model-view transform
        gl.glMatrixMode(GL_MODELVIEW);
        gl.glLoadIdentity();
        
        gl.glPushMatrix();
        gl.glTranslatef(0.0f, 0.0f, -0.1f);
        // TODO: Figure out what order is best for drawing. Image on top of colors or colors on top of image etc.
        //drawBackground(); // convert to drawing the leap plane in order to determine if leap plane is correct
        keyboardRenderables.render(gl);
        gl.glPopMatrix();
        
        //gl.glTranslatef(-323.5f, -192.5f, -1000.0f); // figure out what to do here in order to do perspective if we use texture
        //gl.GL_TEXTURE_RECTANGLE_ARB --- use this for exact texturing if imaging attempt fails.
    }
    
    @Override
    public void update() {
        // do nothing for standard keyboard (possibly add settings later such as enabling the shift/enter/backspace keys)
        if(shiftDown) {
            virtualKeyboard.pressed(Key.VK_SHIFT);
        }
    }
    
    @SuppressWarnings("serial")
    private class KeyBindings extends JPanel {
        
        public KeyBindings() {
            setKeyBindings();
        }
        
        private void setKeyBindings() {
            ActionMap actionMap = getActionMap();
            int condition = JComponent.WHEN_IN_FOCUSED_WINDOW;
            InputMap inputMap = getInputMap(condition);
            
            for(int i = 0; i < Key.getSize(); i++) {
                Key k = Key.getByIndex(i);
                if(k != Key.VK_NULL || k != Key.VK_SHIFT_RELEASED) {
                    // Add normal keys to input map
                    if(k != Key.VK_SHIFT) {
                        inputMap.put(KeyStroke.getKeyStroke(k.getKeyCode(), 0), k.getKeyName());
                    }
                    
                    // Add shifted keys to input map
                    //Shift might act funny --- test this... remove it if need be
                    inputMap.put(KeyStroke.getKeyStroke(k.getKeyCode(), KeyEvent.SHIFT_DOWN_MASK, false), k.getKeyName() + Key.VK_SHIFT.getKeyName());
                    
                    // Add normal keys to action map
                    if(k != Key.VK_SHIFT) {
                        actionMap.put(k.getKeyName(), new KeyAction(k.getKeyValue()));
                    }
                    
                    // Add shifted keys to action map
                    if(Key.VK_A.getKeyCode() <= k.getKeyCode() && k.getKeyCode() <= Key.VK_Z.getKeyCode()) {
                        actionMap.put(k.getKeyName() + Key.VK_SHIFT.getKeyName(), new KeyAction(Character.toUpperCase(k.getKeyValue())));
                    } else {
                        actionMap.put(k.getKeyName() + Key.VK_SHIFT.getKeyName(), new KeyAction(k.getKeyValue()));
                    }
                }
            }
            inputMap.put(KeyStroke.getKeyStroke(Key.VK_SHIFT.getKeyCode(), 0, true), Key.VK_SHIFT_RELEASED.getKeyName());
            actionMap.put(Key.VK_SHIFT_RELEASED.getKeyName(), new KeyAction(Key.VK_SHIFT_RELEASED.getKeyValue()));
        }
        
        private class KeyAction extends AbstractAction {
            public KeyAction(char actionCommand) {
                putValue(ACTION_COMMAND_KEY, actionCommand);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                keyPressed = e.getActionCommand().charAt(0);
                Key key = Key.getByCode(keyPressed) == null ? Key.getByValue(keyPressed) : Key.getByCode(keyPressed);
                if((e.getModifiers() & KeyEvent.SHIFT_MASK) != 0 || shiftDown) {
                    shiftDown = true;
                    virtualKeyboard.pressed(key);
                } else {
                    virtualKeyboard.pressed(key);
                }
                if(key != Key.VK_SHIFT && key != Key.VK_SHIFT_RELEASED) {
                    notifyListeners();
                }
                if(key == Key.VK_SHIFT_RELEASED) {
                    shiftDown = false;
                }
            }
       }
    }
}
