package io.evolution;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import static io.evolution.Constants.*;
import static java.lang.Integer.parseInt;

/**
 * Created by michael on 4/3/2016.
 */
public class AreaPredictor {

    private float[] initialCoordinates = new float[2];
    private float[] secondaryCoordinates = new float[2];
    private ArrayList<float[]> outerBoundaryCoordinates;

    /**
     * The Need two.
     */
    ArrayList<ResultSet> needTwo = new ArrayList<>();
    private float initialLat;
    private float initialLong;
    private float primaryBoundaryLat;
    private float primaryBoundaryLong;
    private final float PI = Float.parseFloat(Double.toString(Math.PI));
    private ResultSet backup;


    private Connection c;
    private int travelTime;
    private float vesselSpeed;
    private String lastContactTime;

    /**
     * Instantiates a new Area predictor.
     *
     * @param c          the c
     * @param mmsi       the mmsi
     * @param date       the date
     * @param travelTime the travel time
     * @throws SQLException the sql exception
     */
    AreaPredictor(Connection c, String mmsi, String date, String travelTime) throws SQLException {
        this.travelTime = parseInt((travelTime));
        this.c = c;
        PreparedStatement get = c.prepareStatement("SELECT * FROM aisData WHERE (MMSI='"
                + mmsi + "' AND DATETIME LIKE '%" + date + "%') ORDER BY " + DATETIME + " DESC LIMIT 2;");
        ResultSet resultSet = get.executeQuery();
        // backup = resultSet.
        //System.out.println("show pulled data: ");

        while (resultSet.next()) {
            needTwo.add(resultSet);
            if (needTwo.size() == 1) {
                initialCoordinates[0] = resultSet.getFloat(LAT);
                initialCoordinates[1] = resultSet.getFloat(LONG);
            } else if (needTwo.size() == 2) {
                String[] dateSplit = needTwo.get(1).getString(DATETIME).split(" ");
                lastContactTime = dateSplit[1];
                System.out.println("lastcontact: " + lastContactTime);
                secondaryCoordinates[0] = resultSet.getFloat(LAT);
                secondaryCoordinates[1] = resultSet.getFloat(LONG);
                //System.out.println("needTwo: " + needTwo);
            }
        }
        insertCoord(0, initialCoordinates[0], initialCoordinates[1]);
        vesselSpeed = 5.0f;

        System.out.println("Initial C :" + initialCoordinates[0]);
        System.out.println("Initial C :" + initialCoordinates[1]);

        System.out.println("Secondary Coord :" + secondaryCoordinates[0]);
        System.out.println("Secondary Coord :" + secondaryCoordinates[1]);

        System.out.println("show pulled data <end>");
        //execute();


    }

    /**
     * Insert coord boolean.
     *
     * @param time      the time
     * @param latitude  the latitude
     * @param longitude the longitude
     * @return the boolean
     */
    public boolean insertCoord(int time, float latitude, float longitude) {
        try {
            System.out.println("INSERT INTO PUBLIC.KMLPOINTS VALUES ('" + time + "'," + latitude + "," + longitude + ");");
            PreparedStatement insertCoord = c.prepareStatement("INSERT INTO PUBLIC.KMLPOINTS VALUES ('" + time + "'," + latitude + "," + longitude + ");");
            insertCoord.execute();
        } catch (SQLException e) {
            return false;
        }
        return true;
    }

    /**
     * Execute boolean.
     *
     * @return the boolean
     */
    public boolean execute() {

        //Generates primary boundary.
        setPrimaryBoundary();

        //Generates secondary boundary.
        setOuterBoundaryCoordinates();

        return true;
    }

    private float getHeading() {

        //Retrieves the second-to-last known coordinates of the vessel.
        float lat1 = initialCoordinates[0];
        float long1 = initialCoordinates[1];
        float lat2 = secondaryCoordinates[0];
        float long2 = secondaryCoordinates[1];

        //Constant used to convert degrees to radians.
        float degreeToRadians = PI / 180.0f;

        //converts each latitude and longitude to radians to be used in heading calculation.
        float lat1Rads = lat1 * degreeToRadians;
        float lat2Rads = lat2 * degreeToRadians;
        float long1Rads = long1 * degreeToRadians;
        float long2Rads = long2 * degreeToRadians;


        //Calculates and returns the heading.
        float result = (float) Math.atan2(Math.sin(long2Rads - long1Rads) * Math.cos(lat2Rads),
                Math.cos(lat1Rads) * Math.sin(lat2Rads) - Math.sin(lat1Rads)
                        * Math.cos(lat2Rads) * Math.cos(long2Rads - long1Rads)
        ) * 180 / PI;

        return result;
    }


    private float getDistance(int time, float knots) {

        //Converts given knots to kilometers per second.
        float knotsToKps = (knots * 0.000514444f);

        //Converts given minutes to seconds.
        float timeToSeconds = time * 60;

        //The distance traveled by the vessel, in meters.
        float distance = (knotsToKps * timeToSeconds);
        return distance;
    }


    private void setPrimaryBoundary() {

        //Get last known coordinates and heading of vessel.
        float distance = getDistance(travelTime, vesselSpeed);
        float heading = getHeading();


        //Calculates the primary boundary coordinates.
        float[] primaryBoundary = calculateCoordinates(initialCoordinates[0], initialCoordinates[1], heading, distance);
        insertCoord(travelTime, primaryBoundary[0], primaryBoundary[1]);
    }


    /**
     * Sets outer boundary coordinates.
     */
    public void setOuterBoundaryCoordinates() {

        //The amount of time simulated to far.
        int currentTime = 0;

        //Initializes the coordinates at the last known signal location.
        float[] currentCoordinates = initialCoordinates;
        float initialHeading = getHeading();
        float currentHeading = getHeading();
        float incrementDistance = getDistance(1, vesselSpeed);
        float lat = currentCoordinates[0];
        float lon = currentCoordinates[1];


        //Creates outer boundary of the polygon minute by minute until the specified time is reached.
        while (currentTime <= travelTime) {
            currentCoordinates = calculateCoordinates(lat, lon, currentHeading, incrementDistance);
            //outerBoundaryCoordinates.add(currentCoordinates);
            lat = currentCoordinates[0];
            lon = currentCoordinates[1];
            //insertCoord(currentTime, lat, lon);
            currentTime++;
            currentHeading += initialHeading;
        }

        insertCoord(currentTime, lat, lon);
    }

    /**
     * Calculate coordinates float [ ].
     *
     * @param lat      the lat
     * @param lon      the lon
     * @param heading  the heading
     * @param distance the distance
     * @return the float [ ]
     */
    public float[] calculateCoordinates(float lat, float lon, float heading, float distance) {

        //Calculates the destination coordinates given the initial coordinates, heading, and time traveled.

        float R = 6378.1f; //Radius of the Earth

        //Bearing is 90 degrees converted to radians.
        //Distance in km

        //lat2  52.20444 - the lat result I'm hoping for
        //lon2  0.36056 - the long result I'm hoping for.

        float lat1 = (float) Math.toRadians(lat); //Current lat point converted to radians
        float lon1 = (float) Math.toRadians(lon); //Current long point converted to radians

        float lat2 = (float) Math.asin(Math.sin(lat1) * Math.cos(distance / R) +
                Math.cos(lat1) * Math.sin(distance / R) * Math.cos(heading));

        float lon2 = lon1 + (float) Math.atan2(Math.sin(heading) * Math.sin(distance / R) * Math.cos(lat1),
                Math.cos(distance / R) - Math.sin(lat1) * Math.sin(lat2));

        lat2 = (float) Math.toDegrees(lat2);
        lon2 = (float) Math.toDegrees(lon2);

        float[] calculatedCoordinates = {lat2, lon2};

        //System.out.println(lat2);
        // System.out.println(lon2);
        return calculatedCoordinates;
    }

}
