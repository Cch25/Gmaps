package com.example.erufe.gmaps;

/**
 * Created by erufe on 22.05.2017.
 */

import android.location.Location;

public class CLocation extends Location {

    private boolean bUseMetricUnits = false;

    public CLocation(Location location)
    {
        this(location, true);
    }

    public CLocation(Location location, boolean bUseMetricUnits) {
        // TODO Auto-generated constructor stub
        super(location);
        this.bUseMetricUnits = bUseMetricUnits;
    }


    public boolean getUseMetricUnits()
    {
        return this.bUseMetricUnits;
    }

    public void setUseMetricunits(boolean bUseMetricUntis)
    {
        this.bUseMetricUnits = bUseMetricUntis;
    }

    @Override
    public float distanceTo(Location dest) {
        // TODO Auto-generated method stub
        float nDistance = super.distanceTo(dest);
        if(!this.getUseMetricUnits())
        {
            nDistance = nDistance*1;
        }
        return nDistance;
    }

    @Override
    public float getAccuracy() {
        // TODO Auto-generated method stub
        float nAccuracy = super.getAccuracy();
        if(!this.getUseMetricUnits())
        {
            nAccuracy = nAccuracy*1;
        }
        return nAccuracy;
    }

    @Override
    public double getAltitude() {
        // TODO Auto-generated method stub
        double nAltitude = super.getAltitude();
        if(!this.getUseMetricUnits())
        {
            nAltitude = nAltitude*1;
        }
        return nAltitude;
    }

    @Override
    public float getSpeed() {
        // TODO Auto-generated method stub
        float nSpeed = super.getSpeed();
        if(!this.getUseMetricUnits())
        {
            //Convert meters/second to km/hour
            nSpeed = nSpeed * (60*60)/1000;
        }
        return nSpeed;
    }



}