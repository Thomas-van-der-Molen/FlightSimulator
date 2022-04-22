import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
public class GameObject 
{
    private List<Triangle> mesh;
    
    public Color color;
    public boolean shading = true;
    public String name;

    private Vector3 globalPosition;
    private EulerAngle orientation;

    public boolean backFaceCull = true;

    public GameObject(Vector3 positionIn, EulerAngle orientationIn, double scaleIn, String modelFileName, Color colorIn)
    {
        System.out.print("Creating gameObject: " + modelFileName + "... ");
        long start = System.nanoTime();
        mesh = new ArrayList<Triangle>();
        orientation = new EulerAngle();
        color = colorIn;
        name = modelFileName.substring(0, modelFileName.indexOf("."));
        globalPosition = new Vector3();
        readObjFile(modelFileName);
        overrideLocalRotation(orientationIn);
        setScale(scaleIn);
        setPosition(positionIn);
        System.out.println("finished in all " + mesh.size() + " triangles in " + (System.nanoTime() - start)/1000000 + "ms");
    }


    public void recalculateLighting(Lighting lighting)
    {
        if (shading)
        {
            for (int i = 0; i < mesh.size(); i++)
            {
                mesh.get(i).calculateLightingColor(lighting);
            }
        }
    }

    public List<Triangle> getMesh()
    {
        return mesh;
    }
    
    public Vector3 getPosition()
    {
        return globalPosition;
    }

    public void forwardControl()
    {
        move(new Vector3(0, 0, 10));
    }

    public void backwardControl()
    {
        move(new Vector3(0, 0, -10));
    }

    public void leftControl()
    {
        move(new Vector3(-10, 0, 0));
    }

    public void rightControl()
    {
        move(new Vector3(10, 0, 0));
    }

    public EulerAngle getOrientation()
    {
        return orientation;
    }

    public void setPosition(Vector3 positionIn)
    {
        Triangle triangle;
        for (int i = 0; i < mesh.size(); i++)
        {
            triangle = mesh.get(i);
            triangle.point1 = Vector3.add(triangle.point1, Vector3.subtract(positionIn, globalPosition));
            triangle.point2 = Vector3.add(triangle.point2, Vector3.subtract(positionIn, globalPosition));
            triangle.point3 = Vector3.add(triangle.point3, Vector3.subtract(positionIn, globalPosition));
        }
        globalPosition = positionIn;
    }

    public void move(Vector3 amount)
    {
        Triangle triangle;
        for (int i = 0; i < mesh.size(); i++)
        {
            triangle = mesh.get(i);
            triangle.point1 = Vector3.add(triangle.point1, amount);
            triangle.point2 = Vector3.add(triangle.point2, amount);
            triangle.point3 = Vector3.add(triangle.point3, amount);
        }
        globalPosition = Vector3.add(globalPosition, amount);
    }

    private void setScale(double scale)
    {
        Triangle triangle;
        for (int i = 0; i < mesh.size(); i++)
        {
            triangle = mesh.get(i);
            triangle.point1 = Vector3.multiply(triangle.point1, scale);
            triangle.point2 = Vector3.multiply(triangle.point2, scale);
            triangle.point3 = Vector3.multiply(triangle.point3, scale);
        }
    }

    public void setGlobalRotation(EulerAngle angle)
    {
        Triangle triangle;
        for (int i = 0; i < mesh.size(); i++)
        {
            triangle = mesh.get(i);
            triangle.point1 = Vector3.rotateAroundXaxis(triangle.point1, angle.x-orientation.x);
            triangle.point1 = Vector3.rotateAroundYaxis(triangle.point1, angle.y-orientation.x);
            triangle.point1 = Vector3.rotateAroundZaxis(triangle.point1, angle.z-orientation.x);
            triangle.point2 = Vector3.rotateAroundXaxis(triangle.point2, angle.x-orientation.x);
            triangle.point2 = Vector3.rotateAroundYaxis(triangle.point2, angle.y-orientation.x);
            triangle.point2 = Vector3.rotateAroundZaxis(triangle.point2, angle.z-orientation.x);
            triangle.point3 = Vector3.rotateAroundXaxis(triangle.point3, angle.x-orientation.x);
            triangle.point3 = Vector3.rotateAroundYaxis(triangle.point3, angle.y-orientation.x);
            triangle.point3 = Vector3.rotateAroundZaxis(triangle.point3, angle.z-orientation.x);
        }
        orientation = angle;
    }

    public void setLocalRotation(EulerAngle angle)
    {
        Triangle triangle;
        for (int i = 0; i < mesh.size(); i++)
        {
            triangle = mesh.get(i);
            triangle.point1 = Vector3.subtract(triangle.point1, globalPosition);
            triangle.point1 = Vector3.rotateAroundXaxis(triangle.point1, angle.x-orientation.x);
            triangle.point1 = Vector3.rotateAroundYaxis(triangle.point1, angle.y-orientation.y);
            triangle.point1 = Vector3.rotateAroundZaxis(triangle.point1, angle.z-orientation.z);
            triangle.point1 = Vector3.add(triangle.point1, globalPosition);

            triangle.point2 = Vector3.subtract(triangle.point2, globalPosition);
            triangle.point2 = Vector3.rotateAroundXaxis(triangle.point2, angle.x-orientation.x);
            triangle.point2 = Vector3.rotateAroundYaxis(triangle.point2, angle.y-orientation.y);
            triangle.point2 = Vector3.rotateAroundZaxis(triangle.point2, angle.z-orientation.z);
            triangle.point2 = Vector3.add(triangle.point2, globalPosition);

            triangle.point3 = Vector3.subtract(triangle.point3, globalPosition);
            triangle.point3 = Vector3.rotateAroundXaxis(triangle.point3, angle.x-orientation.x);
            triangle.point3 = Vector3.rotateAroundYaxis(triangle.point3, angle.y-orientation.y);
            triangle.point3 = Vector3.rotateAroundZaxis(triangle.point3, angle.z-orientation.z);
            triangle.point3 = Vector3.add(triangle.point3, globalPosition);
        }
        orientation = angle;
    }

    private void overrideLocalRotation(EulerAngle angle)
    {
        Triangle triangle;
        for (int i = 0; i < mesh.size(); i++)
        {
            triangle = mesh.get(i);
            triangle.point1 = Vector3.subtract(triangle.point1, globalPosition);
            triangle.point1 = Vector3.rotateAroundXaxis(triangle.point1, angle.x-orientation.x);
            triangle.point1 = Vector3.rotateAroundYaxis(triangle.point1, angle.y-orientation.y);
            triangle.point1 = Vector3.rotateAroundZaxis(triangle.point1, angle.z-orientation.z);
            triangle.point1 = Vector3.add(triangle.point1, globalPosition);

            triangle.point2 = Vector3.subtract(triangle.point2, globalPosition);
            triangle.point2 = Vector3.rotateAroundXaxis(triangle.point2, angle.x-orientation.x);
            triangle.point2 = Vector3.rotateAroundYaxis(triangle.point2, angle.y-orientation.y);
            triangle.point2 = Vector3.rotateAroundZaxis(triangle.point2, angle.z-orientation.z);
            triangle.point2 = Vector3.add(triangle.point2, globalPosition);

            triangle.point3 = Vector3.subtract(triangle.point3, globalPosition);
            triangle.point3 = Vector3.rotateAroundXaxis(triangle.point3, angle.x-orientation.x);
            triangle.point3 = Vector3.rotateAroundYaxis(triangle.point3, angle.y-orientation.y);
            triangle.point3 = Vector3.rotateAroundZaxis(triangle.point3, angle.z-orientation.z);
            triangle.point3 = Vector3.add(triangle.point3, globalPosition);
        }
    }

    private void readObjFile(String fileName)
    {
        List<Vector3> vertices = new ArrayList<Vector3>();
        Scanner scanner;
        String line = "";
        try 
        {
            scanner = new Scanner(new File(FlightSimulator.RESOURCES_FOLDER, fileName));   
        } 
        catch (FileNotFoundException e) 
        {
            e.printStackTrace();
            return;
        }

        while(scanner.hasNextLine())
        {
            line = scanner.nextLine();

            if (!line.equals(""))
            {
                if (line.startsWith("v "))
                {
                    String[] lineArr = line.substring(1).trim().split(" ");
                    Vector3 vertex = new Vector3(Double.parseDouble(lineArr[0]), Double.parseDouble(lineArr[1]), Double.parseDouble(lineArr[2]));
                    vertices.add(vertex);
                }
                if (line.startsWith("f "))
                {
                    String[] lineArr = line.split(" ");
                    int[] indexArr = new int[lineArr.length-1];
                    for (int i = 1; i < lineArr.length; i ++)
                    {
                        if (lineArr[i].contains("/"))
                            indexArr[i-1] = Integer.parseInt(lineArr[i].substring(0, lineArr[i].indexOf("/")))-1;
                    }
                    if (indexArr.length <= 3)
                    {
                        mesh.add(new Triangle(this, vertices.get(indexArr[0]), vertices.get(indexArr[1]), vertices.get(indexArr[2]), color));
                    }
                    else
                    {
                        mesh.add(new Triangle(this, vertices.get(indexArr[0]), vertices.get(indexArr[1]), vertices.get(indexArr[2]), color));
                        mesh.add(new Triangle(this, vertices.get(indexArr[0]), vertices.get(indexArr[2]), vertices.get(indexArr[3]), color));
                    }
                }
            }
        }
    }
}
