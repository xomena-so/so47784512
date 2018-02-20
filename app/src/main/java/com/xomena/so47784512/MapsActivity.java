package com.xomena.so47784512;

import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.android.SphericalUtil;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.EncodedPolyline;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String TAG = "so47784512";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        String origin = "Avinguda Diagonal, 101, 08005 Barcelona, Spain";
        String destination = "Carrer de París, 67, 08029 Barcelona, Spain";

        LatLng center = new LatLng(41.391942,2.179413);

        //Define list to get all latlng for the route
        List<LatLng> path = this.getDirectionsPathFromWebService(origin, destination);

        //Draw the polyline
        if (path.size() > 0) {
            PolylineOptions opts = new PolylineOptions().addAll(path).color(Color.BLUE).width(5);
            mMap.addPolyline(opts);
        }

        List<LatLng> markers = this.getMarkersEveryNMeters(path, 500.0);

        if (markers.size() > 0) {
            for (LatLng m : markers) {
                MarkerOptions mopts = new MarkerOptions().position(m);
                mMap.addMarker(mopts);
            }
        }

        mMap.getUiSettings().setZoomControlsEnabled(true);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 13));
    }

    private List<LatLng> getDirectionsPathFromWebService(String origin, String destination) {
        List<LatLng> path = new ArrayList();


        //Execute Directions API request
        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey("AIzaSyBrPt88vvoPDDn_imh-RzCXl5Ha2F2LYig")
                .build();
        DirectionsApiRequest req = DirectionsApi.getDirections(context, origin, destination);
        try {
            DirectionsResult res = req.await();

            //Loop through legs and steps to get encoded polylines of each step
            if (res.routes != null && res.routes.length > 0) {
                DirectionsRoute route = res.routes[0];

                if (route.legs !=null) {
                    for(int i=0; i<route.legs.length; i++) {
                        DirectionsLeg leg = route.legs[i];
                        if (leg.steps != null) {
                            for (int j=0; j<leg.steps.length;j++){
                                DirectionsStep step = leg.steps[j];
                                if (step.steps != null && step.steps.length >0) {
                                    for (int k=0; k<step.steps.length;k++){
                                        DirectionsStep step1 = step.steps[k];
                                        EncodedPolyline points1 = step1.polyline;
                                        if (points1 != null) {
                                            //Decode polyline and add points to list of route coordinates
                                            List<com.google.maps.model.LatLng> coords1 = points1.decodePath();
                                            for (com.google.maps.model.LatLng coord1 : coords1) {
                                                path.add(new LatLng(coord1.lat, coord1.lng));
                                            }
                                        }
                                    }
                                } else {
                                    EncodedPolyline points = step.polyline;
                                    if (points != null) {
                                        //Decode polyline and add points to list of route coordinates
                                        List<com.google.maps.model.LatLng> coords = points.decodePath();
                                        for (com.google.maps.model.LatLng coord : coords) {
                                            path.add(new LatLng(coord.lat, coord.lng));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch(Exception ex) {
            Log.e(TAG, ex.getLocalizedMessage());
        }

        return path;
    }

    private List<LatLng> getMarkersEveryNMeters(List<LatLng> path, double distance) {
        List<LatLng> res = new ArrayList();

        LatLng p0 = path.get(0);
        res.add(p0);
        if (path.size() > 2) {
            //Initialize temp variables for sum distance between points and
            //and save the previous point
            double tmp = 0;
            LatLng prev = p0;
            for (LatLng p : path) {
                //Sum the distance
                tmp += SphericalUtil.computeDistanceBetween(prev, p);
                if (tmp < distance) {
                    //If it is less than certain value continue sum
                    prev = p;
                    continue;
                } else {
                    //If distance is greater than certain value lets calculate
                    //how many meters over desired value we have and find position of point
                    //that will be at exact distance value
                    double diff = tmp - distance;
                    double heading = SphericalUtil.computeHeading(prev, p);

                    LatLng pp = SphericalUtil.computeOffsetOrigin(p, diff, heading);

                    //Reset sum set calculated origin as last point and add it to list
                    tmp = 0;
                    prev = pp;
                    res.add(pp);
                    continue;
                }
            }

            //Add the last point of route
            LatLng plast = path.get(path.size()-1);
            res.add(plast);
        }

        return res;
    }
}
