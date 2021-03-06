package com.mtbs3d.minecrift.render;

import com.mtbs3d.minecrift.utils.Angle;
import com.mtbs3d.minecrift.utils.Quaternion;
import com.mtbs3d.minecrift.utils.Utils;
import com.mtbs3d.minecrift.utils.Vector3;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Hendrik on 07-Aug-16.
 */
public class PlayerModelController {
	
	private Map<UUID, RotInfo> vivePlayers = new HashMap<UUID, RotInfo>();
	private Map<UUID, RotInfo> vivePlayersLast = new HashMap<UUID, RotInfo>();
	private Map<UUID, RotInfo> vivePlayersReceived = Collections.synchronizedMap(new HashMap<UUID, RotInfo>());
	

	static PlayerModelController instance;
	public static PlayerModelController getInstance(){
		if(instance==null)
			instance=new PlayerModelController();
		return instance;
	}

	public static class RotInfo{ 
		public RotInfo(){
			
		}
		public boolean seated;
		public Quaternion leftArmQuat, rightArmQuat, headQuat; 
		public Vec3d leftArmRot, rightArmRot, headRot; 
		public Vec3d leftArmPos, rightArmPos, Headpos;
	}

	public void Update(UUID uuid, byte[] hmddata, byte[] c0data, byte[] c1data){
	
		Vec3d hmdpos = null, c0pos = null, c1pos = null;
		Quaternion hmdq = null, c0q = null, c1q = null;
		boolean seated = false;
		for (int i = 0; i <= 2; i++) {
			try {
				byte[]arr = null;
				switch(i){
				case 0:	arr = hmddata;
				break;
				case 1: arr = c0data;
				break;
				case 2: arr = c1data;
				break;
				}

				ByteArrayInputStream by = new ByteArrayInputStream(arr);
				DataInputStream da = new DataInputStream(by);

				boolean bool = false;
				if(arr.length >=29)
					bool = da.readBoolean();		

				float posx = da.readFloat();
				float posy = da.readFloat();
				float posz = da.readFloat();
				float rotw = da.readFloat();
				float rotx = da.readFloat();
				float roty = da.readFloat();
				float rotz = da.readFloat();

				da.close();
				
				switch(i){
				case 0:	
					if(bool){ //seated
						seated = true;
					}
					hmdpos = new Vec3d(posx, posy, posz);
					hmdq = new Quaternion(rotw, rotx, roty, rotz);
					break;
				case 1: 
					c0pos = new Vec3d(posx, posy, posz);
					c0q = new Quaternion(rotw, rotx, roty, rotz);
					break;
				case 2: 
					c1pos = new Vec3d(posx, posy, posz);
					c1q = new Quaternion(rotw, rotx, roty, rotz);
					break;
				}
				
			} catch (IOException e) {

			}
		}

		
		Vector3 shoulderR=new Vector3(0,-0.0f,0);

		//Vector3f sLV3f=MCOpenVR.hmdRotation.transform(new Vector3f((float) shoulderL.xCoord,(float) shoulderL.yCoord,(float) shoulderL.zCoord));
		//Vector3f sRV3f=MCOpenVR.hmdRotation.transform(new Vector3f((float) shoulderR.xCoord,(float) shoulderR.yCoord,(float) shoulderR.zCoord));

		Vector3 forward = new Vector3(0,0,-1);
		Vector3 dir = hmdq.multiply(forward);
		Vector3 dir2 = c0q.multiply(forward);
		Vector3 dir3 = c1q.multiply(forward);


				 
		//Quaternion qua=new Quaternion(Vector3.up(),yaw1);

		//shoulderL=shoulderL.multiply(qua.getMatrix());
		//shoulderR=shoulderR.multiply(qua.getMatrix());

//		Vec2f[] vecs=new Vec2f[2];
//
//		for (int i = 0; i <= 1; i++) {
//			Vec3d ctr= i == 0 ? c0pos : c1pos;
//
//			Vec3d offset= i==0 ? shoulderR.toVec3d() : shoulderR.toVec3d();
//			Vec3d vecCtr = ctr.subtract(hmdpos.add(offset)).normalize();
//			Vec3d def = new Vec3d(0,0,-1);
//			
//			Angle euler=Quaternion.createFromToVector(Utils.convertVector(def),Utils.convertVector(vecCtr)).toEuler();
//
//			double pitch = -euler.getPitch();
//			double yaw = euler.getYaw();
//			pitch-=90;
//			yaw=-yaw;
//
//			vecs[i] = new Vec2f((float)Math.toRadians(pitch), (float)Math.toRadians(yaw));
//		}
		
		RotInfo out = new RotInfo();
		out.seated = seated;
		out.leftArmRot=new Vec3d(dir3.getX(), dir3.getY(), dir3.getZ());
		out.rightArmRot=new Vec3d(dir2.getX(), dir2.getY(), dir2.getZ());
		out.headRot = new Vec3d(dir.getX(), dir.getY(), dir.getZ());
		out.Headpos = hmdpos;
		out.leftArmPos = c1pos;
		out.rightArmPos = c0pos;
		out.leftArmQuat = c1q;
		out.rightArmQuat = c0q;
		out.headQuat = hmdq;
		
		if (!uuid.equals(Minecraft.getMinecraft().player.getGameProfile().getId()) || debug) {
			vivePlayersReceived.put(uuid, out);
		}

	}
	
	public void tick() {
		for (Map.Entry<UUID, RotInfo> entry : vivePlayers.entrySet()) {
			vivePlayersLast.put(entry.getKey(), entry.getValue());
		}
		for (Map.Entry<UUID, RotInfo> entry : vivePlayersReceived.entrySet()) {
			vivePlayers.put(entry.getKey(), entry.getValue());
		}
	}
	

	public RotInfo getRotationsForPlayer(UUID uuid){
		if (debug) uuid = Minecraft.getMinecraft().player.getUniqueID();
		RotInfo rot = vivePlayers.get(uuid);
		if (rot != null && vivePlayersLast.containsKey(uuid)) {
			RotInfo rotLast = vivePlayersLast.get(uuid);
			RotInfo rotLerp = new RotInfo();
			float pt = Minecraft.getMinecraft().timer.renderPartialTicks;
			rotLerp.seated = rot.seated;
			rotLerp.leftArmPos = Utils.vecLerp(rotLast.leftArmPos, rot.leftArmPos, pt);
			rotLerp.rightArmPos = Utils.vecLerp(rotLast.rightArmPos, rot.rightArmPos, pt);
			rotLerp.Headpos = Utils.vecLerp(rotLast.Headpos, rot.Headpos, pt);
			rotLerp.leftArmQuat = Utils.quatLerp(rotLast.leftArmQuat, rot.leftArmQuat, pt);
			rotLerp.rightArmQuat = Utils.quatLerp(rotLast.rightArmQuat, rot.rightArmQuat, pt);
			rotLerp.headQuat = Utils.quatLerp(rotLast.headQuat, rot.headQuat, pt);
			Vector3 forward = new Vector3(0,0,-1);
			rotLerp.leftArmRot = Utils.convertToVec3d(rotLerp.leftArmQuat.multiply(forward));
			rotLerp.rightArmRot = Utils.convertToVec3d(rotLerp.rightArmQuat.multiply(forward));
			rotLerp.headRot = Utils.convertToVec3d(rotLerp.headQuat.multiply(forward));
			return rotLerp;
		}
		return rot;
	}

	public boolean debug = false;

	public boolean isTracked(UUID uuid){
		this.debug = false;
		if(debug) return true;
		return vivePlayers.containsKey(uuid);
	}
}
