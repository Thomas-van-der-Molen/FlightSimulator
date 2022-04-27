import java.awt.Color;
import javax.swing.Timer;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

import javax.swing.JPanel;

public class Airplane extends GameObject implements ActionListener
{
    private double groundLevel; 
    private Camera camera;
    private double throttle;
    private AirplaneController airplaneController;
    private AirplanePhysics physics;
    private Timer airplaneUpdater;

    private double maxEnginePower;
    private double pitchSpeed;
    private double yawSpeed;
    private double rollSpeed;
    
    public Airplane(JPanel listenerPanel, Camera camIn)
    {
        super
        (
            "Airplane",
            new Mesh("planeBody.obj", new Vector3(0, 0, 0), new EulerAngle(0, Math.PI/2, 0), 1, new Color(100, 100, 100), true, true),
            new Transform(new Vector3())
        );

        maxEnginePower = 25;
        pitchSpeed = 1;
        yawSpeed = 0.5;
        rollSpeed = 1;

        airplaneController = new AirplaneController();
        groundLevel = -400;
        camera = camIn;
        listenerPanel.addKeyListener(airplaneController);
        throttle = 0;
        physics = new AirplanePhysics();
        airplaneUpdater = new Timer(10, this);
    }

    public void setRenderPanel(RenderingPanel renderingPanel)
    {
        renderingPanel.addMesh(this.getMesh());
    }

    public void startPhysics()
    {
        airplaneUpdater.start();
    }

    public void actionPerformed(ActionEvent e) 
    {
        physics.update();
        physics.applyThrust(throttle*maxEnginePower);
        getMesh().refreshLighting();
    }

    class AirplaneController implements KeyListener, ActionListener
    {
        public Timer keyTimer;

        public boolean throttleUp;
        public boolean throttleDown;
        public boolean pitchUp;
        public boolean pitchDown;
        public boolean rollLeft;
        public boolean rollRight;
        public boolean yawLeft;
        public boolean yawRight;
        public boolean brakes;

        public AirplaneController()
        {
            keyTimer = new Timer(5, this);
            throttleUp = false;
            throttleDown = false;
            pitchUp = false;
            pitchDown = false;
            rollLeft = false;
            rollRight = false;
            yawLeft = false;
            yawRight = false;
            brakes = false;
            keyTimer.start();
        }

        public void keyPressed(KeyEvent e) 
        {
            int key = e.getKeyCode();
            if (key == FlightSimulator.settings.throttleUp)
                throttleUp = true;
            else if (key == FlightSimulator.settings.throttleDown)
                throttleDown = true;
            else if (key == FlightSimulator.settings.pitchUp)
                pitchUp = true;
            else if (key == FlightSimulator.settings.pitchDown)
                pitchDown = true;
            else if (key == FlightSimulator.settings.rollLeft)
                rollLeft = true;
            else if (key == FlightSimulator.settings.rollRight)
                rollRight = true;
            else if (key == FlightSimulator.settings.yawLeft)
                yawLeft = true;
            else if (key == FlightSimulator.settings.yawRight)
                yawRight = true;
            else if (key == FlightSimulator.settings.brakes)
                brakes = true;
        }

        public void keyTyped(KeyEvent e) {}
        public void keyReleased(KeyEvent e) 
        {
            int key = e.getKeyCode();
            if (key == FlightSimulator.settings.throttleUp)
                throttleUp = false;
            else if (key == FlightSimulator.settings.throttleDown)
                throttleDown = false;
            else if (key == FlightSimulator.settings.pitchUp)
                pitchUp = false;
            else if (key == FlightSimulator.settings.pitchDown)
                pitchDown = false;
            else if (key == FlightSimulator.settings.rollLeft)
                rollLeft = false;
            else if (key == FlightSimulator.settings.rollRight)
                rollRight = false;
            else if (key == FlightSimulator.settings.yawLeft)
                yawLeft = false;
            else if (key == FlightSimulator.settings.yawRight)
                yawRight = false;
            else if (key == FlightSimulator.settings.brakes)
                brakes = false;
        }

        public void actionPerformed(ActionEvent e) 
        {
            if (throttleUp && throttle < 100)
                throttle += 1;
            if (throttleDown && throttle > 0)
                throttle -= 1;
            if (pitchUp)
                physics.addPitchTorque(-pitchSpeed);
            if (pitchDown)
                physics.addPitchTorque(pitchSpeed);
            if (rollLeft)
                physics.addRollTorque(rollSpeed);
            if (rollRight)
                physics.addRollTorque(-rollSpeed);
            if (yawLeft)
                physics.addYawTorque(-yawSpeed);
            if (yawRight)
                physics.addYawTorque(yawSpeed);
            //TODO: implement landing gear and brakes
        }
    }

    class AirplanePhysics
    {
        //#region not changed by the program: 
        private double gravity;
        private double mass;
        private double liftCoefficient;
        private double dragCoefficient;
        private double angularDragCoefficient;
        //#endregion

        private Vector3 physicsPosition;
        private EulerAngle physicsRotation;
        private Vector3 velocity;
        private double velocityPitch;
        private double velocityYaw;
        private double velocityRoll;

        public AirplanePhysics()
        {
            gravity = 3;
            mass = 500;
            liftCoefficient = 1.1;
            dragCoefficient = 0.1;
            angularDragCoefficient = 0.01;

            physicsRotation = new EulerAngle();
            physicsPosition = new Vector3();
            velocity = new Vector3();
            velocityPitch = 0;
            velocityRoll = 0;
            velocityYaw = 0;
        }

        public void addForce(Vector3 force)
        {
            velocity.add(Vector3.multiply(force, 1/mass));
        }

        public void addPitchTorque(double amt)
        {
            velocityPitch = velocityPitch + amt/mass;
        }

        public void addYawTorque(double amt)
        {
            velocityYaw = velocityYaw + amt/mass;
        }

        public void addRollTorque(double amt)
        {
            velocityRoll = velocityRoll + amt/mass;
        }

        public void applyDrag()
        {
            Vector3 dragNormalToWings = Vector3.projectToVector(getTransform().getUp(), velocity);
            Vector3 totalDrag = Vector3.add(Vector3.negate(velocity), Vector3.negate(dragNormalToWings));
            
        }

        public void applyAngularDrag()
        {
            velocityPitch = velocityPitch - velocityPitch*angularDragCoefficient;
            velocityYaw = velocityYaw - velocityYaw*angularDragCoefficient;
            velocityRoll = velocityRoll - velocityRoll*angularDragCoefficient;
        }

        public void applyGravity()
        {
            velocity.add(new Vector3(0, -gravity, 0));
        }

        public void applyLift()
        {
            Vector3 wingsHorizontalMotion = Vector3.projectToPlane(velocity, getTransform().getUp());
            addForce(Vector3.multiply(getTransform().getUp(), wingsHorizontalMotion.getSqrMagnitude()*liftCoefficient));
            //System.out.println(wingsHorizontalMotion.getSqrMagnitude()*liftCoefficient);
        }

        public void updatePosition()
        {
            if (Vector3.add(physicsPosition, velocity).y > groundLevel)
                physicsPosition.add(velocity);
            else
            {
                physicsPosition.y = groundLevel;
                velocity = new Vector3(velocity.x, 0 , velocity.z);
                physicsPosition.add(velocity);
            }
            getTransform().setPosition(Vector3.add(getTransform().getPosition(), velocity));
            if (camera.getOrbitCamController() != null)
            {
                camera.getOrbitCamController().updatePosition();
            }
        }

        public void updateOrientation()
        {
            physicsRotation.x += velocityPitch;
            physicsRotation.y += velocityYaw;
            physicsRotation.z += velocityRoll;

            getTransform().setPitch(physicsRotation.x);
            getTransform().setYaw(physicsRotation.y);
            getTransform().setRoll(physicsRotation.z);

        }

        public void applyThrust(double amount)
        {
            addForce(Vector3.multiply(getTransform().getForward(), amount));
        }

        public Vector3 getPosition()
        {
            return physicsPosition;
        }

        public void update()
        {
            applyLift();
            applyGravity();
            applyDrag();
            applyAngularDrag();
            updateOrientation();
            updatePosition();
        }
    }

    
}


