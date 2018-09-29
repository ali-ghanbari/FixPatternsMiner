package test;



import geogebra3D.euclidian3D.opengl.Animator;
import geogebra3D.euclidian3D.opengl.Component3D;
import geogebra3D.euclidian3D.opengl.RendererJogl;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Test implements GLEventListener {
	
	static {
		RendererJogl.initCaps();
	}



	private Component3D canvas;
	
	public Test(){
		
		canvas = new Component3D();

		canvas.addGLEventListener(this);


		Animator animator = new Animator( canvas, 60 );
		animator.start();



		JFrame frame = new JFrame("Test"); 
		frame.setSize(600, 600); 
		
		
		JPanel jp = new JPanel();
		jp.setLayout(new BorderLayout());
		jp.add(BorderLayout.CENTER, canvas);
		frame.add(jp);
		//frame.add(canvas); 
		
		
		frame.setVisible(true); 

		frame.addWindowListener(new WindowAdapter() { 
			public void windowClosing(WindowEvent e) { 
				System.exit(0); 
			} 
		}); 
		
	}

	public static void main(String[] args) { 

		new Test();

	}
	
	

	@Override
	public void display(GLAutoDrawable drawable) {
		update(); 
		render(drawable); 

		//canvas.repaint(); 		
	}

	@Override
	public void dispose(GLAutoDrawable arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		
		GL gl = drawable.getGL(); 
		
		System.out.println("Init on "+Thread.currentThread()
				+"\nChosen GLCapabilities: " + drawable.getChosenGLCapabilities()
				+"\nINIT GL IS: " + gl.getClass().getName()
				+"\nGL_VENDOR: " + gl.glGetString(GL.GL_VENDOR)
				+"\nGL_RENDERER: " + gl.glGetString(GL.GL_RENDERER)
				+"\nGL_VERSION: " + gl.glGetString(GL.GL_VERSION)); 
	}

	@Override
	public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3,
			int arg4) {
		// TODO Auto-generated method stub
		
	}
	
	
    private double theta = 0; 
    private double s = 0; 
    private double c = 0; 
    
    private void update() { 
        theta += 0.01; 
        s = Math.sin(theta); 
        c = Math.cos(theta); 
    } 

    private void render(GLAutoDrawable drawable) { 
        GL2 gl = drawable.getGL().getGL2(); 

        gl.glClear(GL.GL_COLOR_BUFFER_BIT); 

        // draw a triangle filling the window 
        gl.glBegin(GL.GL_TRIANGLES); 
        gl.glColor3f(1, 0, 0); 
        gl.glVertex2d(-c, -c); 
        gl.glColor3f(0, 1, 0); 
        gl.glVertex2d(0, c); 
        gl.glColor3f(0, 0, 1); 
        gl.glVertex2d(s, -s); 
        gl.glEnd(); 
    } 
	
}
