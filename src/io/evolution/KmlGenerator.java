package io.evolution;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by eliakah on 4/3/2016.
 */
public class KmlGenerator {

    ArrayList<Point> points = new ArrayList<Point>(); //polygon points
    ArrayList<Point> placemarks = new ArrayList<Point>(); //points where pins are dropped.


    //pull from database
    void pull(){


    }

    void generate() throws IOException {
        //creates file
        String filename = (getFileName());
        File outPutFile = new File(filename);

        if (outPutFile.createNewFile()) {
            String text = "";
            PrintWriter writer = new PrintWriter(filename, "UTF-8");
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n <kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
            writer.write(" <Document>\n<name>"+filename+"</name> \n");

            //writting polygon
            writer.write(createPolygon());

            //writting placemarks
            for (int i = 0; i < placemarks.size(); i++) {
                writer.write(createPlacemark(placemarks.get(i)));
            }


            writer.write("</Document>\n</kml>");
            writer.close();
        } else {
            System.out.println("File Creation Unsuccessful!.");
        }
    }



    public String createPlacemark(Point point) {
        String tag = "";
        tag += "<Placemark>\n<name>" + point.getLatitude() + ", " + point.getLongitude() + "</name>\n";
        tag += "<description>sample description</description>\n<Point>\n<coordinates>" + point.getLatitude() + "," + point.getLongitude();
        tag += "</coordinates>\n</Point>\n</Placemark>\n";

        return tag;
    }

    public String createPolygon() {
        String tag = "";
        tag += " <Placemark>\n" +
                "<name>Area of Prediction</name>\n" +
                "<Polygon>\n" +
                "<extrude>1</extrude>\n" +
                "<altitudeMode>relativeToGround</altitudeMode>\n" +
                "<outerBoundaryIs>\n" +
                "<LinearRing>\n" +
                "<coordinates>\n";

        for (int i = 0; i < points.size(); i++) {
            tag += points.get(i).getLatitude() + "," + points.get(i).getLongitude()+"\n";
        }

        tag += "</coordinates>\n" +
                "</LinearRing>\n" +
                "</innerBoundaryIs>\n" +
                "</Polygon>\n" +
                "</Placemark>\n";

        return tag;
    }

    public void addPlacemark(Point p) {
        placemarks.add(p);
    }

    public void addPolygonPoints(Point p) {
        points.add(p);
    }

    private String getFileName(){
        Date date = new Date();
        String timeStamp = String.format(""+date);
        timeStamp = timeStamp.replaceAll(" ", "_").toLowerCase();
        timeStamp = timeStamp.replaceAll(":", "_").toLowerCase();
        timeStamp += timeStamp+".kml";
       return timeStamp;
    }



    public class Point {
        double latitude, longitude;

        Point(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public String getCoordinate() {
            return ("" + longitude + latitude);
        }
    }

}