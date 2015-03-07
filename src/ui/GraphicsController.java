package ui;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;

import utilities.MyUtilities;
import keyboard.IKeyboard;
import keyboard.KeyboardObserver;
import static javax.media.opengl.GL.*;  // GL constants
import static javax.media.opengl.GL2.*; // GL2 constants

import com.jogamp.opengl.util.gl2.GLUT;

import enums.Color;

public abstract class GraphicsController extends WindowController implements GLEventListener, KeyboardObserver {
    public static GLU GLU;
    public static GLUT GLUT;
	protected static GLCapabilities CAPABILITIES;
	protected GLCanvas canvas;
	protected IKeyboard keyboard;
	
	public static void init() {
	    GLProfile.initSingleton();
	    CAPABILITIES = new GLCapabilities(GLProfile.getDefault());
	}
	
	protected abstract void render(GL2 gl);
	public abstract void update();
	
	public void render() {
	    if(canvas != null) {
	        canvas.display();
	    }
	}

    @Override
    public void display(GLAutoDrawable drawable) {
        render(drawable.getGL().getGL2());
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        // This might not be the right approach. This has the possibility of being an infinite call.
        if(canvas == drawable) {
            canvas.destroy();
            canvas = null;
        } else {
            drawable.destroy();
        }
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        drawable.setAutoSwapBufferMode(true);
        GL2 gl = drawable.getGL().getGL2();  // get the OpenGL graphics context
        GLU = new GLU();  // get GL Utilities
        GLUT = new GLUT();
        gl.setSwapInterval(1);

        gl.glClearColor(1f, 1f, 1f, 0.0f); // set background (clear) color
        gl.glClearDepth(1.0f);      // set clear depth value to farthest
        gl.glEnable(GL_DEPTH_TEST); // enables depth testing
        gl.glDepthFunc(GL_LEQUAL);  // the type of depth test to do
        gl.glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST); // best perspective correction
        gl.glShadeModel(GL_SMOOTH); // blends colors nicely, and smoothes out lighting
        gl.glEnable(GL_LIGHTING);
        gl.glPolygonMode(GL_FRONT_AND_BACK, GL_SMOOTH);
   
        // ----- Init lighting -----
        float globalAmbient[] = {0.2f, 0.2f, 0.2f, 1f};
        float lightPos[] = {0f, 0f, 1f, 0f};

        gl.glLightfv(GL_LIGHT0, GL_AMBIENT, Color.BLACK.getColor(), 0);
        gl.glLightfv(GL_LIGHT0, GL_DIFFUSE, Color.WHITE.getColor(), 0);
        gl.glLightfv(GL_LIGHT0, GL_SPECULAR, Color.WHITE.getColor(), 0);
        gl.glLightfv(GL_LIGHT0, GL_POSITION, lightPos, 0);
        gl.glLightModelfv(GL_AMBIENT, globalAmbient, 0);
        gl.glEnable(GL_LIGHT0);
        gl.glEnable(GL_LIGHTING);
        gl.glEnable(GL_COLOR_MATERIAL);
        gl.glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);
        gl.glMaterialfv(GL_FRONT_AND_BACK, GL_SPECULAR, Color.BLACK.getColor(), 0);
        gl.glMaterialfv(GL_FRONT_AND_BACK, GL_EMISSION, Color.BLACK.getColor(), 0);
        gl.glDisable(GL_LIGHTING);
        
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        MyUtilities.OPEN_GL_UTILITIES.reshape(drawable.getGL().getGL2(), keyboard);
    }
}
