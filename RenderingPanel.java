import javax.swing.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
public class RenderingPanel extends JPanel implements ActionListener
{
    private List<GameObject> gameObjects = new ArrayList<GameObject>();
    private List<Triangle> triangles = new ArrayList<Triangle>();

    private Timer renderUpdater;

    //for rendering:
    private BufferedImage renderImage;
    private Color backgroundColor;
    private Plane renderPlane;        
    private int[] emptyImagePixelColorData;

    //Camera:
    private Camera camera;

    //lighting:
    private Lighting lightingObject; 
    
    //fog:
    private double fogStartDistance;
    private double fullFogDistance;
    private boolean fogEnabled = false;
    private Color fogColor;

    //used for debug:
    private TimingHelper totalFrameTime = new TimingHelper("totalFrameTime");
    private TimingHelper trianglesOrderTime = new TimingHelper("triangleOrderTime");
    private TimingHelper trianglesCalculateTime = new TimingHelper("trianglesCalculateTime");
    private TimingHelper frameDrawTime = new TimingHelper("frameDrawTime");

    public RenderingPanel(int width, int height)
    {
        renderUpdater = new Timer(1, this);
        setPreferredSize(new Dimension(width, height));
        renderImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        emptyImagePixelColorData = new int[width*height];
        backgroundColor = new Color(200, 220, 255);
        Arrays.fill(emptyImagePixelColorData, convertToIntRGB(backgroundColor));
    }

    public void startRenderUpdates()
    {
        totalFrameTime.startTimer();
        renderUpdater.start();
        validate();
    }

    public void paintComponent(Graphics g) 
    {
        frameDrawTime.startTimer();
        g.drawImage(renderImage, 0, 0, this);
        frameDrawTime.stopTimer();
        if (gameObjects.size() > 0 && camera != null)
        {
            drawTriangles(g);
        }
    }

    public void setLighting(Lighting lighting)
    {
        long lightingStartTime = System.nanoTime();
        System.out.print("\tadding lighting... ");
        lightingObject = lighting;
        lightingObject.update(gameObjects);
        System.out.println("finished in " + (System.nanoTime()-lightingStartTime)/1000000.0 + "ms");
    }

    public void addGameObject(GameObject gameObject)
    {
        long gameObjectStartTime = System.nanoTime();
        System.out.print("\tadding gameObject " + gameObject.name + "... ");
        gameObjects.add(gameObject);
        lightingObject.update(gameObjects);
        triangles.addAll(gameObject.mesh);
        if (gameObject.hasPlayerController())
            this.addKeyListener(gameObject.getPlayerController());
        System.out.println("finished in " + (System.nanoTime()-gameObjectStartTime)/1000000.0 + "ms");
    }

    public void setCamera(Camera camIn)
    {
        long camStartTime = System.nanoTime();
        System.out.print("\tadding camera... ");
        camera = camIn;
        renderPlane = camera.getRenderPlane();
        addMouseMotionListener(camIn.getController());
        addMouseListener(camIn.getController());
        addKeyListener(camIn.getController());
        if (camIn.getFocusObj() == null && gameObjects.size() > 0)
        {
            camIn.setFocus(gameObjects.get(0));
        }
        System.out.println("finished in " + (System.nanoTime()-camStartTime)/1000000.0 + "ms");
    }

    public void setFog(double fogStartDistanceIn, double fullFogDistanceIn, Color fogColorIn)
    {
        fogStartDistance = fogStartDistanceIn;
        fullFogDistance = fullFogDistanceIn;
        fogColor = fogColorIn;
        fogEnabled = true;
    }

    public void enableFog()
    {
        fogEnabled = true;
    }

    public void dissableFog()
    {
        fogEnabled = false;
    }
    
    private void drawTriangles(Graphics g)
    {
        renderImage.getRaster().setDataElements(0, 0, renderImage.getWidth(), renderImage.getHeight(), emptyImagePixelColorData);
        renderPlane = camera.getRenderPlane();

        trianglesOrderTime.startTimer();
        if (gameObjects.size() > 0 && triangles.size() > 0)
            orderTriangles();    
        trianglesOrderTime.stopTimer();

        trianglesCalculateTime.startTimer();
        for (int i = 0; i < triangles.size(); i ++)
        {
            renderTriangle(g, triangles.get(i));
        }
        trianglesCalculateTime.stopTimer();
    }

    private void orderTriangles()
    {
        boolean changed = true;
        while (changed == true)
        {
            changed = false;
            for (int i = 0; i < triangles.size()-1; i++)
            {
                if (triangles.get(i).parentGameObject.shading)
                {
                    if (Vector3.subtract(camera.getPosition(), Vector3.centerOfTriangle(triangles.get(i))).getMagnitude() < Vector3.subtract(camera.getPosition(), Vector3.centerOfTriangle(triangles.get(i+1))).getMagnitude())
                    {
                        Triangle closerTriangle = triangles.get(i);
                        triangles.set(i, triangles.get(i+1));
                        triangles.set(i + 1, closerTriangle);
                        changed = true;
                    }
                }
            }
        }
    }

    private void renderTriangle(Graphics g, Triangle triangle)
    {
        Point p1ScreenCoords = new Point();
        Point p2ScreenCoords = new Point();
        Point p3ScreenCoords = new Point();
        boolean shouldDrawTriangle = false;

        Vector3 tempPoint1 = new Vector3(triangle.point1);
        Vector3 tempPoint2 = new Vector3(triangle.point2);
        Vector3 tempPoint3 = new Vector3(triangle.point3);

        Vector3 camPos = camera.getPosition();
        double distanceToTriangle = Vector3.subtract(Vector3.centerOfTriangle(triangle), camPos).getMagnitude();  
        if (distanceToTriangle < camera.getViewDistance())
        {
            double renderPlaneWidth = camera.getRenderPlaneWidth();
            if (Vector3.dotProduct(renderPlane.normal, Vector3.subtract(tempPoint1, renderPlane.pointOnPlane)) > 0 && Vector3.dotProduct(renderPlane.normal, Vector3.subtract(tempPoint2, renderPlane.pointOnPlane)) > 0 && Vector3.dotProduct(renderPlane.normal, Vector3.subtract(tempPoint3, renderPlane.pointOnPlane)) > 0)
            {
                tempPoint1 = Vector3.getIntersectionPoint(Vector3.subtract(tempPoint1, camPos), camPos, renderPlane);
                double pixelsPerUnit = getWidth()/renderPlaneWidth;
                Vector3 camCenterPoint = Vector3.getIntersectionPoint(camera.getDirectionVector(), camPos, renderPlane);
                Vector3 rotatedPoint = Vector3.rotateAroundXaxis(Vector3.rotateAroundYaxis( //rotates the points to only be on the XY plane
                    Vector3.subtract(tempPoint1, camCenterPoint), //moves the point to be centered around 0,0,0
                    -camera.getHorientation()*0.017453292519943295), //amount to be rotated by horizontally
                    camera.getVorientation()*0.017453292519943295); //amount to  be rotated by vertically
                if ((Math.abs(rotatedPoint.x) < renderPlaneWidth/2*1.2 && Math.abs(rotatedPoint.y) < renderPlaneWidth*((double)getHeight()/(double)getWidth())/2*1.2))
                    shouldDrawTriangle = true;
                p1ScreenCoords.x = (int)(getWidth()/2 + rotatedPoint.x*pixelsPerUnit);
                p1ScreenCoords.y = (int)(getHeight()/2 - rotatedPoint.y*pixelsPerUnit);
        
                tempPoint2 = Vector3.getIntersectionPoint(Vector3.subtract(tempPoint2, camPos), camPos, renderPlane);
                rotatedPoint = Vector3.rotateAroundXaxis(Vector3.rotateAroundYaxis( //rotates the points to only be on the XY plane
                    Vector3.subtract(tempPoint2, camCenterPoint), //moves the point to be centered around 0,0,0
                    -camera.getHorientation()*0.017453292519943295), //amount to be rotated by horizontally
                    camera.getVorientation()*0.017453292519943295); //amount to  be rotated by vertically
                if ((Math.abs(rotatedPoint.x) < renderPlaneWidth/2*1.2 && Math.abs(rotatedPoint.y) < renderPlaneWidth*((double)getHeight()/getWidth())/2*1.2))
                    shouldDrawTriangle = true;
                p2ScreenCoords.x = (int)(getWidth()/2 + rotatedPoint.x*pixelsPerUnit);
                p2ScreenCoords.y = (int)(getHeight()/2 - rotatedPoint.y*pixelsPerUnit);
        
                tempPoint3 = new Vector3(triangle.point3);
                tempPoint3 = Vector3.getIntersectionPoint(Vector3.subtract(tempPoint3, camPos), camPos, renderPlane);
                rotatedPoint = Vector3.rotateAroundXaxis(Vector3.rotateAroundYaxis( //rotates the points to only be on the XY plane
                    Vector3.subtract(tempPoint3, camCenterPoint), //moves the point to be centered around 0,0,0
                    -camera.getHorientation()*0.017453292519943295), //amount to be rotated by horizontally
                    camera.getVorientation()*0.017453292519943295); //amount to  be rotated by vertically
                if ((Math.abs(rotatedPoint.x) < renderPlaneWidth/2*1.2 && Math.abs(rotatedPoint.y) < renderPlaneWidth*((double)getHeight()/getWidth())/2*1.2))
                    shouldDrawTriangle = true;
                p3ScreenCoords.x = (int)(getWidth()/2 + rotatedPoint.x*pixelsPerUnit);
                p3ScreenCoords.y = (int)(getHeight()/2 - rotatedPoint.y*pixelsPerUnit);
            }

            if (shouldDrawTriangle)
            {
                Color colorUsed;
                if (triangle.parentGameObject != null && triangle.parentGameObject.shading)
                {
                    Color litColor = triangle.getColorWithLighting();
                    if (fogEnabled && distanceToTriangle > fogStartDistance)
                    {
                        Color triangleColor;
                        if (distanceToTriangle > fullFogDistance)
                            triangleColor = fogColor;
                        else
                        {
                            double fogAmt = (distanceToTriangle-fogStartDistance)/(fullFogDistance-fogStartDistance);
                            int red = litColor.getRed() + (int)((fogColor.getRed()-litColor.getRed())*fogAmt*fogAmt);
                            int green = litColor.getGreen() + (int)((fogColor.getGreen()-litColor.getGreen())*fogAmt*fogAmt);
                            int blue = litColor.getBlue() + (int)((fogColor.getBlue()-litColor.getBlue())*fogAmt*fogAmt);
                            red = Math.max(0, Math.min(255, red));
                            green = Math.max(0, Math.min(255, green));
                            blue = Math.max(0, Math.min(255, blue));
                            triangleColor = new Color(red, green, blue);
                        }
                        colorUsed = triangleColor;
                    }
                    else 
                        colorUsed = litColor;
                }   
                else 
                    colorUsed = triangle.color;
                paintTriangle(p1ScreenCoords, p2ScreenCoords, p3ScreenCoords, colorUsed);
            }
        }
    }

    private int convertToIntRGB(Color color)
    {
        return 65536 * color.getRed() + 256 * color.getGreen() + color.getBlue();
    }

    private void paintTriangle(Point p1, Point p2, Point p3, Color triangleColor)
    {
        Point tempPoint = new Point();
        int rgb = convertToIntRGB(triangleColor);
        if (p1.getY() > p2.getY())
        {
            tempPoint = p1;
            p1 = p2;
            p2 = tempPoint;
        }
        if (p2.getY() > p3.getY())
        {
            tempPoint = p2;
            p2 = p3;
            p3 = tempPoint;
        }
        if (p1.getY() > p2.getY())
        {
            tempPoint = p1;
            p1 = p2;
            p2 = tempPoint;
        }
        if (p2.getY() > p3.getY())
        {
            tempPoint = p2;
            p2 = p3;
            p3 = tempPoint;
        } 

        int yScanLine;
        int edge1, edge2;
        //Top part of triangle: 
        if (p2.y-p1.y != 0 && p3.y-p1.y != 0)
        {
            if (p2.x - p1.x == 0)
            {
                edge1 = Math.max(0, Math.min(renderImage.getWidth(), p1.x));
                for (yScanLine = p1.y; yScanLine < p2.y && yScanLine < renderImage.getHeight(); yScanLine ++)
                {
                    if (yScanLine >= 0)
                    {
                        edge2 = Math.max(0, Math.min(renderImage.getWidth(), (int)((yScanLine-p1.y)/((double)(p3.y-p1.y)/(p3.x-p1.x)) + p1.x)));
                        drawHorizontalLine(Math.min(edge1, edge2), Math.max(edge1, edge2), yScanLine, rgb);
                    }
                }
            }
            else if (p3.x-p1.x == 0)
            {
                edge2 = Math.max(0, Math.min(renderImage.getWidth(), p1.x));
                for (yScanLine = p1.y; yScanLine < p2.y && yScanLine < renderImage.getHeight(); yScanLine ++)
                {
                    if (yScanLine >= 0)
                    {
                        edge1 = Math.max(0, Math.min(renderImage.getWidth(), (int)((yScanLine-p1.y)/((double)(p2.y-p1.y)/(p2.x-p1.x)) + p1.x)));
                        drawHorizontalLine(Math.min(edge1, edge2), Math.max(edge1, edge2), yScanLine, rgb);
                    }
                }
            }
            else
            {
                for (yScanLine = p1.y; yScanLine < p2.y && yScanLine < renderImage.getHeight(); yScanLine ++)
                {
                    if (yScanLine >= 0)
                    {
                        edge1 = Math.max(0, Math.min(renderImage.getWidth(), (int)((yScanLine-p1.y)/((double)(p2.y-p1.y)/(p2.x-p1.x)) + p1.x)));
                        edge2 = Math.max(0, Math.min(renderImage.getWidth(), (int)((yScanLine-p1.y)/((double)(p3.y-p1.y)/(p3.x-p1.x)) + p1.x)));
                        drawHorizontalLine(Math.min(edge1, edge2), Math.max(edge1, edge2), yScanLine, rgb);
                    }
                }
            }
        }
        

        //bottom part of triangle: 
        if (p3.y-p2.y != 0 && p3.y-p1.y != 0)
        {
            if (p3.x-p2.x == 0)
            {
                edge1 = Math.max(0, Math.min(renderImage.getWidth(), p2.x));
                for (yScanLine = p2.y; yScanLine < p3.y && yScanLine < renderImage.getHeight(); yScanLine ++)
                {
                    if (yScanLine >= 0)
                    {
                        edge2 = Math.max(0, Math.min(renderImage.getWidth(), (int)((yScanLine-p3.y)/((double)(p3.y-p1.y)/(p3.x-p1.x)) + p3.x)));
                        drawHorizontalLine(Math.min(edge1, edge2), Math.max(edge1, edge2), yScanLine, rgb);
                    }
                }
            }
            else if (p3.x - p1.x == 0)
            {
                edge2 = Math.max(0, Math.min(renderImage.getWidth(), p3.x));
                for (yScanLine = p2.y; yScanLine < p3.y && yScanLine < renderImage.getHeight(); yScanLine ++)
                {
                    if (yScanLine >= 0)
                    {
                        edge1 = Math.max(0, Math.min(renderImage.getWidth(), (int)((yScanLine-p3.y)/((double)(p3.y-p2.y)/(p3.x-p2.x)) + p3.x)));
                        drawHorizontalLine(Math.min(edge1, edge2), Math.max(edge1, edge2), yScanLine, rgb);
                    }
                }
            }
            else
            {
                for (yScanLine = p2.y; yScanLine < p3.y && yScanLine < renderImage.getHeight(); yScanLine ++)
                {
                    if (yScanLine >= 0)
                    {
                        edge1 = Math.max(0, Math.min(renderImage.getWidth(), (int)((yScanLine-p3.y)/((double)(p3.y-p2.y)/(p3.x-p2.x)) + p3.x)));
                        edge2 = Math.max(0, Math.min(renderImage.getWidth(), (int)((yScanLine-p3.y)/((double)(p3.y-p1.y)/(p3.x-p1.x)) + p3.x)));
                        drawHorizontalLine(Math.min(edge1, edge2), Math.max(edge1, edge2), yScanLine, rgb);
                    }
                }
            }
        }
    }

    private void drawHorizontalLine(int startOFLineX, int endOfLineX, int levelY, int rgb)
    {
        int[] pixelArray = new int[(Math.abs(endOfLineX-startOFLineX))];
        Arrays.fill(pixelArray, rgb);
        renderImage.getRaster().setDataElements(startOFLineX, levelY, Math.abs(endOfLineX-startOFLineX), 1, pixelArray);
    }

    @Override
    public void actionPerformed(ActionEvent e) 
    {
        totalFrameTime.stopTimer();
        totalFrameTime.startTimer();
        repaint();
        requestFocusInWindow();
    }
}


