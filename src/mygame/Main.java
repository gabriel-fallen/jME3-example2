/*
Copyright 2018 Alexander Tchitchigin.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package mygame;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.SpotLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.SpotLightShadowRenderer;

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 * @author normenhansen
 */
public class Main extends SimpleApplication {

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }
    
    private final static int SHADOWMAP_SIZE = 1024;
    
    private BulletAppState bulletAppState;
    private SpotLight flashlight;
    private boolean flashOn = false;
    private Spatial sceneModel;
    private RigidBodyControl landscape;
    private CharacterControl player;
    private final Vector3f walkDirection = new Vector3f();
    private boolean left = false, right = false, up = false, down = false, run = false;

    //Temporary vectors used on each frame.
    //They here to avoid instanciating new vectors on each frame
    private final Vector3f camDir = new Vector3f();
    private final Vector3f camLeft = new Vector3f();

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
//        bulletAppState.setDebugEnabled(true);

        // We re-use the flyby camera for rotation, while positioning is handled by physics
        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        flyCam.setMoveSpeed(10);
        setUpKeys();
        setUpLight();

        // We load the scene from the zip file and adjust its size.
        assetManager.registerLocator("town.zip", ZipLocator.class);
        sceneModel = assetManager.loadModel("main.scene");
        sceneModel.setLocalScale(2f);
        sceneModel.setShadowMode(ShadowMode.CastAndReceive);

        // We set up collision detection for the scene by creating a
        // compound collision shape and a static RigidBodyControl with mass zero.
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
        landscape = new RigidBodyControl(sceneShape, 0);
        sceneModel.addControl(landscape);

        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1.5f, 6f, 1);
        player = new CharacterControl(capsuleShape, 0.5f);
        player.setJumpSpeed(20);
        player.setFallSpeed(40);
//        player.setGravity(new Vector3f(0, -30f, 0));
        player.setPhysicsLocation(new Vector3f(0, 5.5f, 0));
        
        // should be loaded after player initialization
        Spatial ogre = loadOgre();
        ogre.setShadowMode(ShadowMode.Cast);
        CharacterControl ogre_phy = ogre.getControl(CharacterControl.class);
        ogre_phy.setPhysicsLocation(new Vector3f(-20, 5.5f, -30));
        rootNode.attachChild(ogre);

        rootNode.attachChild(sceneModel);
        bulletAppState.getPhysicsSpace().add(landscape);
        bulletAppState.getPhysicsSpace().add(player);
        bulletAppState.getPhysicsSpace().add(ogre_phy);
    }
    
    private void setUpLight() {
        // We add light so we see the scene
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(0.3f));
        rootNode.addLight(al);

        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.White.mult(0.6f));
        sun.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
        rootNode.addLight(sun);
        
        flashlight = new SpotLight();
        flashlight.setSpotRange(100f);
        flashlight.setSpotInnerAngle((float) (Math.PI/6));
        flashlight.setSpotOuterAngle((float) (Math.PI/3));
        
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, SHADOWMAP_SIZE, 3);
        dlsr.setLight(sun);
        viewPort.addProcessor(dlsr);
        
        SpotLightShadowRenderer flsr = new SpotLightShadowRenderer(assetManager, SHADOWMAP_SIZE);
        flsr.setLight(flashlight);
        viewPort.addProcessor(flsr);
    }
    
    private void setUpKeys() {
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Run", new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Flashlight", new KeyTrigger(KeyInput.KEY_E));
        inputManager.addListener(keysListener, "Left");
        inputManager.addListener(keysListener, "Right");
        inputManager.addListener(keysListener, "Up");
        inputManager.addListener(keysListener, "Down");
        inputManager.addListener(keysListener, "Run");
        inputManager.addListener(keysListener, "Jump");
        inputManager.addListener(keysListener, "Flashlight");
    }
    
    private final ActionListener keysListener = new ActionListener() {
        @Override
        public void onAction(String binding, boolean isPressed, float tpf) {
            switch (binding) {
                case "Left":
                    left = isPressed;
                    break;
                case "Right":
                    right= isPressed;
                    break;
                case "Up":
                    up = isPressed;
                    break;
                case "Down":
                    down = isPressed;
                    break;
                case "Run":
                    run = isPressed;
                    break;
                case "Jump":
                    if (isPressed) player.jump(Vector3f.UNIT_Y);
                    break;
                case "Flashlight":
                    if (!isPressed) {
                        if (flashOn) {
                            rootNode.removeLight(flashlight);
                            flashOn = false;
                        }
                        else {
                            rootNode.addLight(flashlight);
                            flashOn = true;
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };
    
    private Spatial loadOgre() {
        Spatial ogre = assetManager.loadModel("Models/Oto/Oto.mesh.xml");
//        ogre.setLocalScale(2f);
        
//        CollisionShape ogreShape = CollisionShapeFactory.createDynamicMeshShape(ogre);
        CapsuleCollisionShape ogreShape = new CapsuleCollisionShape(3f, 4f, 1);
        CharacterControl phy = new CharacterControl(ogreShape, 0.5f);
//        phy.setGravity(new Vector3f(0, -30f, 0));
        ogre.addControl(phy);
        ogre.addControl(new OgreAI(player));
        
        return ogre;
    }

    @Override
    public void simpleUpdate(float tpf) {
        camDir.set(cam.getDirection()).multLocal(0.6f);
        camLeft.set(cam.getLeft()).multLocal(0.4f);
        walkDirection.set(0, 0, 0);
        
        if (left) walkDirection.addLocal(camLeft);
        if (right) walkDirection.addLocal(camLeft.negate());
        if (up) walkDirection.addLocal(camDir);
        if (down) walkDirection.addLocal(camDir.negate());
        if (run) walkDirection.multLocal(2f);
        
        walkDirection.setY(0);
        if (player.onGround()) player.setWalkDirection(walkDirection);
        cam.setLocation(player.getPhysicsLocation());
        flashlight.setPosition(player.getPhysicsLocation());
        flashlight.setDirection(camDir);
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}

class OgreAI extends AbstractControl {
    private static enum State { IDLE, CHASING, ATTACKING }
    
    private final CharacterControl player;
    private CharacterControl self;
    
    private AnimChannel channel;
    private AnimControl control;
    
    private State state = State.IDLE;
    private final Vector3f walkDirection = new Vector3f();
    private final Vector3f lookDirection = new Vector3f();

    public OgreAI(CharacterControl player) {
        this.player = player;
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);
        self = spatial.getControl(CharacterControl.class);
        control = spatial.getControl(AnimControl.class);
        channel = control.createChannel();
        channel.setAnim("stand");
//        for (String anim : control.getAnimationNames()) {
//            System.out.println(anim);
//        }
//      Walk, pull, Dodge, stand, push
    }

    @Override
    protected void controlUpdate(float tpf) {
        Vector3f player_pos = player.getPhysicsLocation();
        Vector3f self_pos = self.getPhysicsLocation();
        float distance = player_pos.distance(self_pos);
        walkDirection.set(0, 0, 0);
        
        if (distance < 15) state = State.ATTACKING;
        else if (distance > 100) state = State.IDLE;
        else state = State.CHASING;
        
        switch (state) {
            case IDLE:
                channel.setAnim("stand", 0.50f);
                channel.setLoopMode(LoopMode.DontLoop);
                channel.setSpeed(1f);
                break;
            case CHASING:
                if (!channel.getAnimationName().equals("Walk")) {
                    channel.setAnim("Walk", 0.50f);
                    channel.setLoopMode(LoopMode.Loop);
                }
                walkDirection.set(player_pos.subtract(self_pos).normalizeLocal().multLocal(0.5f));
                walkDirection.setY(0);
                lookDirection.set(walkDirection);
                break;
            case ATTACKING:
                if (!channel.getAnimationName().equals("push")) {
                    channel.setAnim("push", 0.50f);
                    channel.setLoopMode(LoopMode.Loop);
                }
                lookDirection.set(player_pos.subtract(self_pos));
                break;
        }
        
        self.setWalkDirection(walkDirection);
        self.setViewDirection(lookDirection);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // not needed
    }
    
}
