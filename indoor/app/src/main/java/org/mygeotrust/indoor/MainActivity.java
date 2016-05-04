package org.mygeotrust.indoor;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.kml.Style;
import org.osmdroid.bonuspack.location.OverpassAPIProvider;
import org.osmdroid.bonuspack.overlays.FolderOverlay;
import org.osmdroid.bonuspack.overlays.MapEventsOverlay;
import org.osmdroid.bonuspack.overlays.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class MainActivity extends AppCompatActivity implements MapEventsReceiver {

    private MapView map;
    private IMapController mapController;

    private BoundingBoxE6 bbFgi;

    private OverpassAPIProvider provider;
    private KmlDocument mKmlDocument;
    private KmlDocument mKmlBuildingDocument;

    private FolderOverlay kmlBuildingOverlay;
    private FolderOverlay kmlIndoorOverlay;

    private boolean indoorOverlayDisplayed;

    private RadioGroup radioFloorGroup;
    private RadioButton radioFloor3;
    private RadioButton radioFloor2;
    private RadioButton radioFloor1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        map = (MapView) findViewById(R.id.map_view_mygt);
        initateOSMap();

        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this, this);
        map.getOverlays().add(0, mapEventsOverlay);

        radioFloorGroup = (RadioGroup) findViewById(R.id.radio_floor_group);
        radioFloor1 = (RadioButton) findViewById(R.id.radio_floor_1);
        radioFloor2 = (RadioButton) findViewById(R.id.radio_floor_2);
        radioFloor3 = (RadioButton) findViewById(R.id.radio_floor_3);

        radioFloorGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                clearIndoorOverlay();

                switch (checkedId){
                    case R.id.radio_floor_1:

                        if(!indoorOverlayDisplayed && radioFloor1.isChecked()){
                            Log.d("Radio", "Checked Floor 1");
                            AsyncIndoorReq req = new AsyncIndoorReq();
                            req.execute("http://92.222.2.38/apks/fgi/1.kml");
                        }
                        break;
                    case R.id.radio_floor_2:
                        if(!indoorOverlayDisplayed && radioFloor2.isChecked()){
                            Log.d("Radio", "Checked Floor 2");
                            AsyncIndoorReq req = new AsyncIndoorReq();
                            req.execute("http://92.222.2.38/apks/fgi/2.kml");
                        }
                        break;
                    case R.id.radio_floor_3:
                        if(!indoorOverlayDisplayed && radioFloor3.isChecked()){
                            Log.d("Radio", "Checked Floor 3");
                            AsyncIndoorReq req = new AsyncIndoorReq();
                            req.execute("http://92.222.2.38/apks/fgi/3.kml");
                        }
                        break;
                }
            }
        });

        bbFgi = new BoundingBoxE6(60.161840, 24.548068, 60.160538, 24.541974); // north, east, south, west

        provider = new OverpassAPIProvider();
        String val = provider.urlForTagSearchKml("building", bbFgi, 10, 1000);
        Log.d("MyGT", val);

        AsyncBuilding buildingReq = new AsyncBuilding();
        buildingReq.execute(val);


        map.setMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                if(event.getZoomLevel() > 18){
                    radioFloorGroup.setVisibility(View.VISIBLE);
                    radioFloor1.setChecked(true);
                }
                else {
                    radioFloorGroup.setVisibility(View.INVISIBLE);
                    radioFloor1.setChecked(false);
                    clearIndoorOverlay();
                }

                return true;
            }
        });
    }

    /**
     *
     */
    private void initateOSMap() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        mapController = map.getController();

        map.setMaxZoomLevel(24);
        mapController.setZoom(18);

        GeoPoint startPoint = new GeoPoint(60.160991, 24.545530);

        mapController.setCenter(startPoint);

        map.invalidate();
    }

    /**
     *
     */
    private void clearIndoorOverlay(){
        boolean contains = map.getOverlays().contains(kmlIndoorOverlay);
        if(contains){
            map.getOverlays().remove(kmlIndoorOverlay);
            indoorOverlayDisplayed = false;
            map.invalidate();
        }
    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        return false;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;
    }

    /**
     *
     */
    private class AsyncBuilding extends AsyncTask<String, Void, KmlDocument> {

        @Override
        protected KmlDocument doInBackground(String... data) {
            mKmlBuildingDocument = new KmlDocument();
            boolean ok = provider.addInKmlFolder(mKmlBuildingDocument.mKmlRoot, data[0]);

            if(ok){

            }

            return mKmlBuildingDocument;
        }

        @Override
        protected void onPostExecute(KmlDocument kmlDocument) {
            super.onPostExecute(kmlDocument);

            Drawable defaultMarker = getResources().getDrawable(R.drawable.marker_default);
            Bitmap defaultBitmap = ((BitmapDrawable)defaultMarker).getBitmap();
            Style defaultStyle = new Style(defaultBitmap, R.color.colorPrimary, 5.0f, 0x20AA1010);

            kmlBuildingOverlay = (FolderOverlay)kmlDocument.mKmlRoot.buildOverlay(map, defaultStyle, null, kmlDocument);
            map.getOverlays().add(kmlBuildingOverlay);

            map.invalidate();

        }


    }

    /**
     *
     */
    private class AsyncIndoorReq extends AsyncTask<String, Void, KmlDocument> {

        @Override
        protected KmlDocument doInBackground(String... params) {
            mKmlDocument = new KmlDocument();
            boolean okKml = mKmlDocument.parseKMLUrl(params[0]);

            return mKmlDocument;
        }

        @Override
        protected void onPostExecute(KmlDocument kmlDocument) {
            super.onPostExecute(kmlDocument);

            Drawable defaultMarker = getResources().getDrawable(R.drawable.marker_default);
            Bitmap defaultBitmap = ((BitmapDrawable)defaultMarker).getBitmap();
            Style defaultStyle = new Style(defaultBitmap, 0x901010AA, 3.0f, 0x20AA1010);

            kmlIndoorOverlay = (FolderOverlay)kmlDocument.mKmlRoot.buildOverlay(map, defaultStyle, null, kmlDocument);
            map.getOverlays().add(kmlIndoorOverlay);

            indoorOverlayDisplayed = true;

            map.invalidate();

        }
    }
}
