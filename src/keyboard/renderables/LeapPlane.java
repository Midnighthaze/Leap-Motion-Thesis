package keyboard.renderables;

import javax.media.opengl.GL2;

import enums.RenderableName;
import keyboard.KeyboardRenderable;
import leap.LeapPlaneData;

public class LeapPlane extends KeyboardRenderable {
    private static final String RENDER_NAME = RenderableName.LEAP_PLANE.toString();
    private LeapPlaneData leapPlaneData;
    
    public LeapPlane() {
        super(RENDER_NAME);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void render(GL2 gl) {
        // TODO Auto-generated method stub
    }
}
