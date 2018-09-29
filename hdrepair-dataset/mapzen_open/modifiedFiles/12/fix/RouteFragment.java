package com.mapzen.fragment;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.mapzen.R;
import com.mapzen.activity.BaseActivity;
import com.mapzen.adapters.RoutesAdapter;
import com.mapzen.osrm.Instruction;
import com.mapzen.osrm.Route;
import com.mapzen.util.Logger;

import org.json.JSONObject;
import org.oscim.core.GeoPoint;
import org.oscim.layers.PathLayer;

import java.util.ArrayList;

import static com.mapzen.activity.BaseActivity.ROUTE_STACK;
import static com.mapzen.activity.BaseActivity.SEARCH_RESULTS_STACK;
import static com.mapzen.util.ApiHelper.getRouteUrlForFoot;

public class RouteFragment extends BaseFragment implements DirectionListFragment.DirectionListener, LocationListener {
    private ArrayList<Instruction> instructions;
    private GeoPoint from, destination;
    private ViewPager pager;
    private Button button;
    private RoutesAdapter adapter;
    private Route route;
    private LocationRequest locationRequest;
    public static final int WALKING_THRESH_HOLD = 10;

    public void setInstructions(ArrayList<Instruction> instructions) {
        Logger.d("instructions: " + instructions.toString());
        this.instructions = instructions;
    }

    @Override
    public void onResume() {
        super.onResume();
        initLocationClient();
        act.hideActionBar();
        drawRoute();
    }

    private void initLocationClient() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(BaseActivity.LOCATION_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        act.getLocationClient().requestLocationUpdates(locationRequest, this);
    }

    private Location getNextTurnTo(Instruction nextInstruction) {
        Location nextTurn = new Location(getResources().getString(R.string.application_name));
        nextTurn.setLatitude(nextInstruction.getPoint()[0]);
        nextTurn.setLongitude(nextInstruction.getPoint()[1]);
        return nextTurn;
    }

    @Override
    public void onLocationChanged(Location location) {
        Logger.d("RouteFragment::onLocationChangeLocation" + instructions.toString());
        for (Instruction instruction : instructions) {
            Location nextTurn = getNextTurnTo(instruction);
            if (location != null && nextTurn != null) {
                int distanceToNextTurn = (int) Math.floor(location.distanceTo(nextTurn));
                if (distanceToNextTurn > WALKING_THRESH_HOLD) {
                    Logger.d("RouteFragment::onLocationChangeLocation: " +
                            "outside defined radius");
                    Toast.makeText(act, "outside", Toast.LENGTH_SHORT).show();
                } else {
                    Logger.d("RouteFragment::onLocationChangeLocation: " +
                            "inside defined radius advancing");
                    advanceTo(instructions.indexOf(instruction));
                }
                Logger.d("RouteFragment::onLocationChangeLocation: " +
                        "new current location: " + location.toString());
                Logger.d("RouteFragment::onLocationChangeLocation: " +
                        "next turn: " + nextTurn.toString());
                Logger.d("RouteFragment::onLocationChangeLocation: " +
                        "distance to next turn: " + String.valueOf(distanceToNextTurn));
                Logger.d("RouteFragment::onLocationChangeLocation: " +
                        "threshold: " + String.valueOf(WALKING_THRESH_HOLD));
            } else {
                if (location == null) {
                    Logger.d("RouteFragment::onLocationChangeLocation: **next turn** is null screw it");
                }
                if (nextTurn == null) {
                    Logger.d("RouteFragment::onLocationChangeLocation: **location** is null screw it");
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        act.getLocationClient().removeLocationUpdates(this);
        clearRoute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.route_widget, container, false);
        FrameLayout frame = (FrameLayout) container;
        if (frame != null) {
            frame.setVisibility(View.VISIBLE);
        }
        pager = (ViewPager) rootView.findViewById(R.id.routes);
        button = (Button) rootView.findViewById(R.id.view_steps);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDirectionListFragment();
            }
        });
        adapter = new RoutesAdapter(getFragmentManager());
        adapter.setMap(mapFragment.getMap());
        adapter.setInstructions(instructions);
        adapter.setParent(this);
        pager.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        return rootView;
    }

    private void showDirectionListFragment() {
        final Fragment fragment = DirectionListFragment.newInstance(instructions, this);
        getActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.full_list, fragment, DirectionListFragment.TAG)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        act.showActionBar();
        clearRoute();
    }

    public void setFrom(GeoPoint from) {
        this.from = from;
    }

    public void setDestination(GeoPoint destination) {
        this.destination = destination;
    }

    public void next() {
        pager.setCurrentItem(pager.getCurrentItem() + 1);
    }

    public void advanceTo(int i) {
        pager.setCurrentItem(i);
    }

    public Instruction getNextInstruction() {
        if (instructions.size() > pager.getCurrentItem() + 1) {
            return instructions.get(pager.getCurrentItem() + 1);
        }
        return null;
    }

    public void prev() {
        pager.setCurrentItem(pager.getCurrentItem() - 1);
    }

    public int getCurrentItem() {
        return pager.getCurrentItem();
    }

    public void attachToActivity() {
        act.hideActionBar();
        act.getPagerResultsFragment().clearMap();
        act.showProgressDialog();
        popSearchResultsStack();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(getRouteUrlForFoot(
                app.getStoredZoomLevel(), from, destination), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        setRouteFromResponse(response);
                        if (route.foundRoute()) {
                            setInstructions(route.getRouteInstructions());
                            drawRoute();
                            act.dismissProgressDialog();
                            displayRoute();
                        } else {
                            Toast.makeText(act, act.getString(R.string.no_route_found), Toast.LENGTH_LONG).show();
                            act.dismissProgressDialog();
                            act.showActionBar();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                onServerError(volleyError);
            }
        }
        );
        app.enqueueApiRequest(jsonObjectRequest);
    }

    private void popSearchResultsStack() {
        act.getSupportFragmentManager()
                .popBackStack(SEARCH_RESULTS_STACK, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    private void drawRoute() {
        PathLayer layer = mapFragment.getPathLayer();
        layer.clearPath();
        if (route != null) {
            for (double[] pair : route.getGeometry()) {
                layer.addPoint(new GeoPoint(pair[0], pair[1]));
            }
        }
    }

    private void setRouteFromResponse(JSONObject response) {
        this.route = new Route(response);
    }

    private void displayRoute() {
        act.getSupportFragmentManager().beginTransaction()
                .addToBackStack(ROUTE_STACK)
                .add(R.id.routes_container, this, "route")
                .commit();
    }

    private void clearRoute() {
        PathLayer layer = mapFragment.getPathLayer();
        layer.clearPath();
        mapFragment.updateMap();
    }

    @Override
    public void onInstructionSelected(int index) {
        pager.setCurrentItem(index, true);
    }
}
