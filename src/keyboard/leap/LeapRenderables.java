package keyboard.leap;

import javax.media.opengl.GL2;

import enums.FileExt;
import enums.FileName;
import keyboard.KeyboardRenderable;
import keyboard.KeyboardRenderables;
import keyboard.renderables.KeyboardImage;
import keyboard.renderables.KeyboardGestures;
import keyboard.renderables.LeapPlane;
import keyboard.renderables.LeapPoint;
import keyboard.renderables.LeapTool;
import keyboard.renderables.LeapTrail;
import keyboard.renderables.VirtualKeyboard;

public class LeapRenderables extends KeyboardRenderables {
    //private KeyboardImage keyboardImage;
    //private VirtualKeyboard virtualKeyboard;
    //private LeapPlane leapPlane;
    //private LeapGestures leapGestures;
    //private LeapTool leapTool;
    //private LeapPoint leapPoint;

    LeapRenderables(LeapKeyboard keyboard) {
        // order here determines render order
        this.addRenderable(new LeapPlane(keyboard));
        this.addRenderable(new KeyboardImage(keyboard.getFileName() + FileName.KEYBOARD_IMAGE.getName() + FileExt.PNG.getExt()));
        this.addRenderable(new KeyboardImage(keyboard.getFileName() + FileName.KEYBOARD_IMAGE_UPPER.getName() + FileExt.PNG.getExt()));
        this.addRenderable(new VirtualKeyboard(keyboard.getAttributes()));
        this.addRenderable(new KeyboardGestures(keyboard.getAttributes()));
        this.addRenderable(new LeapPoint(keyboard.getAttributes()));
        this.addRenderable(new LeapTool(keyboard.getAttributes()));
        this.addRenderable(new LeapTrail(keyboard.getAttributes()));
    }

    @Override
    public void render(GL2 gl) {
        for(KeyboardRenderable renderable: this.getAllRenderables()) {
            renderable.render(gl);
        }
    }
}
