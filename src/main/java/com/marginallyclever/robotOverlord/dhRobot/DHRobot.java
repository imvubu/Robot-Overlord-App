package com.marginallyclever.robotOverlord.dhRobot;

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JPanel;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3f;

import com.jogamp.opengl.GL2;
import com.marginallyclever.convenience.MatrixHelper;
import com.marginallyclever.robotOverlord.RobotOverlord;
import com.marginallyclever.robotOverlord.material.Material;
import com.marginallyclever.robotOverlord.model.ModelFactory;
import com.marginallyclever.robotOverlord.robot.Robot;
import com.marginallyclever.robotOverlord.robot.RobotKeyframe;

/**
 * A robot designed using D-H parameters.
 * @author Dan Royer
 *
 */
public class DHRobot extends Robot {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	LinkedList<DHLink> links;
	DHKeyframe poseNow;
	DHPanel panel;
	Matrix4d endMatrix;
	
	public DHRobot() {
		super();
		setDisplayName("DH Robot");
		links = new LinkedList<DHLink>();
		endMatrix = new Matrix4d();
		
		// setup sixi2 as default.
		setNumLinks(8);
		// roll
		links.get(0).d=13.44;
		links.get(0).theta=-90;
		links.get(0).flags = DHLink.READ_ONLY_D | DHLink.READ_ONLY_R | DHLink.READ_ONLY_ALPHA;
		links.get(0).angleMin=-160;
		links.get(0).angleMax=160;
		// tilt
		links.get(1).alpha=-20;
		links.get(1).flags = DHLink.READ_ONLY_D | DHLink.READ_ONLY_THETA | DHLink.READ_ONLY_R;
		links.get(2).angleMin=-72;
		// tilt
		links.get(2).d=44.55;
		links.get(2).alpha=30;
		links.get(2).flags = DHLink.READ_ONLY_D | DHLink.READ_ONLY_THETA | DHLink.READ_ONLY_R;
		links.get(2).angleMin=83.369;
		links.get(2).angleMax=86;
		// interim point
		links.get(3).d=4.7201;
		links.get(3).alpha=90;
		links.get(3).flags = DHLink.READ_ONLY_D | DHLink.READ_ONLY_THETA | DHLink.READ_ONLY_R | DHLink.READ_ONLY_ALPHA;
		// roll
		links.get(4).d=28.805;
		links.get(4).theta=30;
		links.get(4).flags = DHLink.READ_ONLY_D | DHLink.READ_ONLY_R | DHLink.READ_ONLY_ALPHA;
		// tilt
		links.get(5).d=11.8;
		links.get(5).alpha=50;
		links.get(5).flags = DHLink.READ_ONLY_D | DHLink.READ_ONLY_THETA | DHLink.READ_ONLY_R;
		// roll
		links.get(6).flags = DHLink.READ_ONLY_D | DHLink.READ_ONLY_R | DHLink.READ_ONLY_ALPHA;
		
		links.get(7).d=3.9527;
		links.get(7).flags = DHLink.READ_ONLY_D | DHLink.READ_ONLY_THETA | DHLink.READ_ONLY_R | DHLink.READ_ONLY_ALPHA;

		try {
			links.get(0).model = ModelFactory.createModelFromFilename("/Sixi2/anchor.stl",0.1f);
			links.get(1).model = ModelFactory.createModelFromFilename("/Sixi2/shoulder.stl",0.1f);
			links.get(2).model = ModelFactory.createModelFromFilename("/Sixi2/bicep.stl",0.1f);
			links.get(3).model = ModelFactory.createModelFromFilename("/Sixi2/forearm.stl",0.1f);
			links.get(5).model = ModelFactory.createModelFromFilename("/Sixi2/tuningFork.stl",0.1f);
			links.get(6).model = ModelFactory.createModelFromFilename("/Sixi2/picassoBox.stl",0.1f);
			links.get(7).model = ModelFactory.createModelFromFilename("/Sixi2/hand.stl",0.1f);

			double ELBOW_TO_ULNA_Y = -28.805f;
			double ELBOW_TO_ULNA_Z = 4.7201f;
			double ULNA_TO_WRIST_Y = -11.800f;
			double ULNA_TO_WRIST_Z = 0;
			double ELBOW_TO_WRIST_Y = ELBOW_TO_ULNA_Y + ULNA_TO_WRIST_Y;
			double ELBOW_TO_WRIST_Z = ELBOW_TO_ULNA_Z + ULNA_TO_WRIST_Z;
			//double WRIST_TO_HAND = 8.9527;

			links.get(0).model.adjustOrigin(new Vector3f(0, 5.150f, 0));
			links.get(1).model.adjustOrigin(new Vector3f(0, 8.140f-13.44f, 0));
			links.get(2).model.adjustOrigin(new Vector3f(-1.82f, 9, 0));
			links.get(3).model.adjustOrigin(new Vector3f(0, (float)ELBOW_TO_WRIST_Z, (float)ELBOW_TO_WRIST_Y));
			links.get(5).model.adjustOrigin(new Vector3f(0, 0, (float)ULNA_TO_WRIST_Y));
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		refreshPose();
	}
	
	@Override
	public RobotKeyframe createKeyframe() {
		return new DHKeyframe();
	}

	@Override
	public ArrayList<JPanel> getContextPanel(RobotOverlord gui) {
		ArrayList<JPanel> list = super.getContextPanel(gui);
		
		panel = new DHPanel(gui,this);
		list.add(panel);
		
		return list;
	}
	
	@Override
	public void render(GL2 gl2) {
		gl2.glPushMatrix();
		Vector3f position = this.getPosition();
		gl2.glTranslatef(position.x, position.y, position.z);
		
		Iterator<DHLink> i = links.iterator();

		// draw models
		float r=1;
		float g=217f/255f;
		float b=33f/255f;
		Material mat = new Material();
		mat.setDiffuseColor(r,g,b,1);
		mat.render(gl2);
		
		gl2.glPushMatrix();
		while(i.hasNext()) {
			DHLink link = i.next();
			renderLinkModel(gl2,link);
		}
		gl2.glPopMatrix();

		// Draw FK
		gl2.glPushMatrix();
		boolean isDepth = gl2.glIsEnabled(GL2.GL_DEPTH_TEST);
		boolean isLit = gl2.glIsEnabled(GL2.GL_LIGHTING);
		gl2.glDisable(GL2.GL_DEPTH_TEST);
		gl2.glDisable(GL2.GL_LIGHTING);

		i = links.iterator();
		while(i.hasNext()) {
			DHLink link = i.next();
			renderLinkPose(gl2,link);
		}
		gl2.glPopMatrix();
		MatrixHelper.drawMatrix(gl2, 
				new Vector3f((float)endMatrix.m03,(float)endMatrix.m13,(float)endMatrix.m23),
				new Vector3f((float)endMatrix.m00,(float)endMatrix.m10,(float)endMatrix.m20),
				new Vector3f((float)endMatrix.m01,(float)endMatrix.m11,(float)endMatrix.m21),
				new Vector3f((float)endMatrix.m02,(float)endMatrix.m12,(float)endMatrix.m22)
				);
		
		if(isDepth) gl2.glEnable(GL2.GL_DEPTH_TEST);
		if(isLit) gl2.glEnable(GL2.GL_LIGHTING);

		gl2.glPopMatrix();
	}
	
	/**
	 * Update the pose matrix of each DH link and also use forward kinematics to find the {@end} position.
	 */
	public void refreshPose() {
		endMatrix.setIdentity();
		
		Iterator<DHLink> i = links.iterator();
		while(i.hasNext()) {
			DHLink link = i.next();
			// update matrix
			link.refreshPoseMatrix();
			// find cumulative matrix
			endMatrix.mul(link.pose);
			link.poseCumulative.set(endMatrix);
		}
	}
	
	/**
	 * Render the model in a D-H chain.  
	 * Changes the current render matrix!  Clean up after yourself!  
	 * @param gl2 the render context
	 * @param link the link to render
	 */
	public void renderLinkModel(GL2 gl2,DHLink link) {
		// swap between Java's Matrix4d and OpenGL's matrix.
		Matrix4d pose = link.pose;
		
		double[] mat = new double[16];
		mat[ 0] = pose.m00;
		mat[ 1] = pose.m10;
		mat[ 2] = pose.m20;
		mat[ 3] = pose.m30;
		mat[ 4] = pose.m01;
		mat[ 5] = pose.m11;
		mat[ 6] = pose.m21;
		mat[ 7] = pose.m31;
		mat[ 8] = pose.m02;
		mat[ 9] = pose.m12;
		mat[10] = pose.m22;
		mat[11] = pose.m32;
		mat[12] = pose.m03;
		mat[13] = pose.m13;
		mat[14] = pose.m23;
		mat[15] = pose.m33;
		
		gl2.glPushMatrix();
		// TODO all of these rotations should be applied to the model once on load, or pre-processed before application start.
		if(link==links.get(0)) {
			gl2.glRotated(90, 1, 0, 0);
		}
		if(link==links.get(1)) {
			gl2.glRotated(90, 1, 0, 0);
		}
		if(link==links.get(2)) {
			gl2.glRotated(90, 1, 0, 0);
		}
		if(link==links.get(3)) {
			gl2.glRotated(90, 1, 0, 0);
			gl2.glRotated(180, 0, 1, 0);
		}
		if(link==links.get(5)) {
			gl2.glRotated(180, 0, 1, 0);
		}
		if(link==links.get(6)) {
			gl2.glRotated(180, 0, 1, 0);
		}
		if(link==links.get(7)) {
			gl2.glRotated(180, 0, 1, 0);
		}
		
		if(link.model!=null) {
			link.model.render(gl2);
		}
		gl2.glPopMatrix();
		
		// inverse camera matrix has already been applied, multiply by that to position the link in the world.
		gl2.glMultMatrixd(mat, 0);
	}
	
	/**
	 * Render one link in a D-H chain.  
	 * Changes the current render matrix!  Clean up after yourself!  
	 * @param gl2 the render context
	 * @param link the link to render
	 */
	public void renderLinkPose(GL2 gl2,DHLink link) {
		// swap between Java's Matrix4d and OpenGL's matrix.
		Matrix4d pose = link.pose;
		
		double[] mat = new double[16];
		mat[ 0] = pose.m00;
		mat[ 1] = pose.m10;
		mat[ 2] = pose.m20;
		mat[ 3] = pose.m30;
		mat[ 4] = pose.m01;
		mat[ 5] = pose.m11;
		mat[ 6] = pose.m21;
		mat[ 7] = pose.m31;
		mat[ 8] = pose.m02;
		mat[ 9] = pose.m12;
		mat[10] = pose.m22;
		mat[11] = pose.m32;
		mat[12] = pose.m03;
		mat[13] = pose.m13;
		mat[14] = pose.m23;
		mat[15] = pose.m33;

		MatrixHelper.drawMatrix(gl2, 
				new Vector3f(0,0,0),
				new Vector3f(1,0,0),
				new Vector3f(0,1,0),
				new Vector3f(0,0,1));
		
		// draw the bone for this joint
		gl2.glPushMatrix();
			gl2.glRotated(link.theta,0,0,1);
			gl2.glColor3f(1, 0, 0);
			gl2.glBegin(GL2.GL_LINE_STRIP);
			gl2.glVertex3d(0, 0, 0);
			gl2.glVertex3d(0, 0, link.d);
			gl2.glVertex3d(link.r, 0, link.d);
			gl2.glEnd();
		gl2.glPopMatrix();

		// inverse camera matrix has already been applied, multiply by that to position the link in the world.
		gl2.glMultMatrixd(mat, 0);
	}
	
	/**
	 * Adjust the number of links in this robot
	 * @param newSize must be greater than 0
	 */
	public void setNumLinks(int newSize) {
		if(newSize<1) newSize=1;
		
		int oldSize = links.size();
		while(oldSize>newSize) {
			oldSize--;
			links.pop();
		}
		while(oldSize<newSize) {
			oldSize++;
			links.push(new DHLink());
		}
	}

	/**
	 * Adjust the world transform of the robot
	 * @param pos the new world position for the local origin of the robot.
	 */
	@Override
	public void setPosition(Vector3f pos) {
		super.setPosition(pos);
		refreshPose();
		if(panel!=null) panel.updateEnd();
	}
}
