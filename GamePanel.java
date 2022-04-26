import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.BorderLayout;

public class GamePanel extends JPanel
{
    private RenderingPanel renderingPanel;
    private Airplane airplane;
    private Lighting lighting; 
    private Camera gameCamera;

    private Color skyColor = new Color(200, 220, 255);

    public GamePanel()
    {
        setLayout(new BorderLayout());
        
        lighting = new Lighting(new Vector3(1, -1, 0.5), 30, 60);
        gameCamera = new Camera(new Vector3(0, 0, -1000), 6000, 10, 60);
        airplane = new Airplane(this, gameCamera);
        gameCamera.setOrbitControls(this, airplane, 1000, 10);
    }

    public void paintComponent(Graphics g)
    {
        requestFocusInWindow();
        renderingPanel = new RenderingPanel(FlightSimulator.DEFAULT_WIDTH, FlightSimulator.DEFAULT_HEIGHT);
        airplane.setRenderPanel(renderingPanel);
        airplane.startPhysics();
        renderingPanel.setLighting(lighting);
        renderingPanel.setCamera(gameCamera);
        renderingPanel.setLighting(lighting);
        renderingPanel.setFog(3000, 6000, skyColor);
        renderingPanel.startRenderUpdates();
        add(renderingPanel);
        validate();
    }

    public String getName()
    {
        return "GamePanel";
    }
}
