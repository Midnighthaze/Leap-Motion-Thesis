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

public abstract class GraphicsController implements GLEventListener, KeyboardObserver {
	private static GLProfile profile;
	protected static GLCapabilities capabilities;
	protected GLCanvas canvas;
	public static GL2 gl;
	protected IKeyboard keyboard;
	public static GLU glu;
	public static GLUT glut;
	protected boolean enabled = false;
	
	public static void init() {
	    GLProfile.initSingleton();
		profile = GLProfile.getDefault();
		capabilities = new GLCapabilities(profile);
	}
	
	public abstract void enable();
	public abstract void disable();
	public abstract void update();
	public abstract void render(GLAutoDrawable drawable);
	
	public boolean isEnabled() {
	    return enabled;
	}
	
	public void display() {
	    if(canvas != null) {
	        canvas.display();
	    }
	}

    @Override
    public void display(GLAutoDrawable drawable) {
        render(drawable);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        // TODO Determine if there is anything we need to do here.
        System.out.println("I'm being disposed -- GL");
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GraphicsController.gl = drawable.getGL().getGL2();  // get the OpenGL graphics context
        glu = new GLU();  // get GL Utilities
        glut = new GLUT();
        gl.setSwapInterval(1);

        gl.glClearColor(1f, 1f, 1f, 0.0f); // set background (clear) color
        gl.glClearDepth(1.0f);      // set clear depth value to farthest
        gl.glEnable(GL_DEPTH_TEST); // enables depth testing
        gl.glDepthFunc(GL_LEQUAL);  // the type of depth test to do
        gl.glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST); // best perspective correction
        gl.glShadeModel(GL_SMOOTH); // blends colors nicely, and smoothes out lighting
        gl.glEnable(GL_LIGHTING);
        gl.glPolygonMode(GL_FRONT_AND_BACK, GL_SMOOTH);
   
        // ----- Your OpenGL initialization code here -----
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
        MyUtilities.OPEN_GL_UTILITIES.reshape(gl, keyboard);
    }
}
