package com.polito.did2017.lampup.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.support.v7.widget.Toolbar;
import android.widget.Switch;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.polito.did2017.lampup.fragments.GyroLampFragment;
import com.polito.did2017.lampup.R;
import com.polito.did2017.lampup.utilities.ColorGridAdapter;
import com.polito.did2017.lampup.utilities.ConnectTask;
import com.polito.did2017.lampup.utilities.Lamp;
import com.polito.did2017.lampup.utilities.LampManager;
import com.polito.did2017.lampup.utilities.TCPClient;
import com.truizlop.fabreveallayout.FABRevealLayout;
import com.truizlop.fabreveallayout.OnRevealChangeListener;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class LampDetailActivity extends AppCompatActivity implements GyroLampFragment.OnGyroLampFragmentInteractionListener {

    private Context context = this;
    private final static String COLORS_PREFS = "MyColors";
    private final static String ANGLE_PREF = "LastAngle";
    private FABRevealLayout fabRevealLayout;
    private GridView color_grid;
    private ColorGridAdapter cga;
    private LinearLayout color_picker;
    private LinearLayout color;
    private SeekBar hue, saturation, brightness;
    private Switch switchOnOff;
    private Fragment fragment;
    private float[] hsv = new float[3];
    private ArrayList<Integer> colors = new ArrayList<>();
    private int lastSize = 0;

    private LampManager lampManager = LampManager.getInstance();
    private String lampIP = "";
    private Lamp selectedLamp;
    private TCPClient tcpClient;
    private ConnectTask connectTask;

    //default commands
    private final String turnOn = "turnOn";
    private final String turnOff = "turnOff";
    private final String switchState = "switchState";
    private final String setLum = "setLum";
    private final String setColor = "setColor";
    private final String setMainServo = "setMainServo";
    //private final int MIN_LUM = 5;
    //private final int MAX_LUM = 255;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lamp_detail);

        final Intent i = getIntent();

        if(i.hasExtra( "lamp_ip" )) {
            lampIP = i.getStringExtra("lamp_ip");
        } else {
            System.out.println("no lamp_ip");
        }

        //trova la lampada che si vuole utilizzare
        for (Lamp lamp : lampManager.getLamps()) {
            if(lamp.getLampIP().equals( lampIP )) {
                selectedLamp = lamp;
                break;
            }
        }

        //CONNECTION TCP
        tcpClient = new TCPClient(new TCPClient.OnMessageReceived() {
            @Override
            //here the messageReceived method is implemented
            public void messageReceived(final String message) {
                //this method calls the onProgressUpdate
                // Get a handler that can be used to post to the main thread
                Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {

                        //Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                        Log.d("message: ", message);

                        String[] cmd_rcv = message.split("$");
                        switch (cmd_rcv[0]) {

                            case turnOn:
                                selectedLamp.turnOn();
                                switchOnOff.setChecked(true);
                                break;

                            case turnOff:
                                selectedLamp.turnOff();
                                switchOnOff.setChecked(false);
                                break;

                            case setLum:
                                if(cmd_rcv.length > 1) {
                                    Toast.makeText(context, cmd_rcv[1], Toast.LENGTH_SHORT).show();
                                    selectedLamp.setBrightness(Integer.parseInt(cmd_rcv[1]));
                                    brightness.setProgress(selectedLamp.getBrightness());
                                }
                                break;
                        }

                    } // This is your code
                };
                mainHandler.post(myRunnable);

            }
        }, selectedLamp.getLampIP());

        //CONNECT TASK, non bloccante
        connectTask = new ConnectTask(getApplicationContext(), this);
        connectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, tcpClient);

        //SWITCH ON-OFF
        switchOnOff = findViewById(R.id.switchOnOff);
        switchOnOff.setChecked( selectedLamp.isOn() );
        switchOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                selectedLamp.setState( switchOnOff.isChecked() );
                if(switchOnOff.isChecked())
                    tcpClient.setMessage(turnOn);
                else
                    tcpClient.setMessage(turnOff);

            }
        });

        if(getColors(COLORS_PREFS)!=null) {
            colors = getColors(COLORS_PREFS);
            lastSize = colors.size();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        int fragId = i.getIntExtra("lamp", 0);
        String frag = i.getStringExtra("lamp_name");

        checkLampId(frag);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment, "gyrolamp");
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        fragmentTransaction.commit();

        ((GyroLampFragment)fragment).setAngle(PreferenceManager.getDefaultSharedPreferences(context).getInt(ANGLE_PREF, 80));

        fabRevealLayout = findViewById(R.id.fab_reveal_layout);
        configureFABReveal(fabRevealLayout);
        color_grid = findViewById(R.id.color_grid);
        color_grid.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                animateColorElement();
            }
        });
        cga = new ColorGridAdapter(context, colors);
        color_grid.setAdapter(cga);
        color_grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Color.colorToHSV(colors.get(i), hsv);
                hsv[2] = (float) brightness.getProgress()/100;
            }
        });
        color_picker = findViewById(R.id.sliders);
        color = findViewById(R.id.secondary_view);
        hue = findViewById(R.id.hue_slider);
        saturation = findViewById(R.id.saturation_slider);
        brightness = findViewById(R.id.brightness_slider);
        brightness.setProgress( selectedLamp.getBrightness() );

        brightness.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //setta la luminosità
                if(progress == 0)
                    progress = 5;
                selectedLamp.setBrightness( progress );
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d("Brightness", "Seekbar luminosità cambiata");
                if(selectedLamp.isOn())
                    tcpClient.setMessage( setLum + "$" + selectedLamp.getBrightness());
            }
        } );
        resetColor(getResources().getColor(R.color.colorAccent));
        initHS();
        ImageButton cancel = color_picker.findViewById(R.id.btn_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fabRevealLayout.revealMainView();
            }
        });
        ImageButton confirm = color_picker.findViewById(R.id.btn_done);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                colors.add(Color.HSVToColor(hsv));
                cga.notifyDataSetChanged();
                fabRevealLayout.revealMainView();
                saveColors(colors, COLORS_PREFS);
            }
        });
    }

    private void checkLampId(String fragId) {
        switch (fragId) {
            case "gyro_lamp":
                fragment = new GyroLampFragment();
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initHS() {

        hue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                hsv[0] = progress;
                color.setBackgroundColor(Color.HSVToColor(hsv));
                GradientDrawable sat = (GradientDrawable) getResources().getDrawable(R.drawable.shape_saturation_gradient);
                sat.setColors(new int[]{getResources().getColor(R.color.white), Color.HSVToColor(new float[]{hsv[0], 1, 1})});
                color_picker.findViewById(R.id.saturation_gradient).setBackground(sat);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        saturation.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                hsv[1] = (float) progress/100;
                color.setBackgroundColor(Color.HSVToColor(hsv));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void configureFABReveal(FABRevealLayout fabRevealLayout) {
        fabRevealLayout.setOnRevealChangeListener(new OnRevealChangeListener() {
            @Override
            public void onMainViewAppeared(FABRevealLayout fabRevealLayout, View mainView) {
                resetColor(getResources().getColor(R.color.colorAccent));
                brightness.setEnabled(true);
                brightness.animate().alpha(1).setDuration(1000).setInterpolator(new OvershootInterpolator()).start();
                color_grid.animate().alpha(1).setDuration(1000).setInterpolator(new OvershootInterpolator()).start();
            }

            @Override
            public void onSecondaryViewAppeared(final FABRevealLayout fabRevealLayout, View secondaryView) {
                animateSlidersView(color_picker);
                brightness.animate().alpha(0).setDuration(1000).setInterpolator(new OvershootInterpolator()).start();
                brightness.setEnabled(false);
                color_grid.animate().alpha(0).setDuration(1000).setInterpolator(new OvershootInterpolator()).start();
            }
        });
    }

    private void animateColorElement() {
        if (colors.size() > lastSize) {
            scale(color_grid.getChildAt(color_grid.getLastVisiblePosition()), 0);
            lastSize = colors.size();
        }
    }

    private void resetColor(int color) {
        Color.colorToHSV(color, hsv);
        hue.setProgress((int) hsv[0]);
        float tmp = hsv[1]*100;
        saturation.setProgress((int) tmp);
    }

    private void animateSlidersView(LinearLayout c) {
        c.setTranslationY(700);
        c.animate()
                .translationY(0)
                .setDuration(500)
                .setStartDelay(100)
                .setInterpolator(new OvershootInterpolator())
                .start();
        scale(c.findViewById(R.id.hue_gradient), 100);
        scale(c.findViewById(R.id.hue_slider), 100);
        scale(c.findViewById(R.id.saturation_gradient), 150);
        scale(c.findViewById(R.id.saturation_slider), 150);
        scale(c.findViewById(R.id.btn_cancel), 100);
        scale(c.findViewById(R.id.btn_done), 150);
    }

    private void scale(View view, long delay) {
        view.setScaleX(0);
        view.setScaleY(0);
        view.animate()
                .scaleX(1)
                .scaleY(1)
                .setDuration(500)
                .setStartDelay(delay+600)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void sendAngleMessage(int progress) {
        tcpClient.setMessage( setMainServo + "$" + progress);
        saveAngle(progress);
    }

    public void saveColors(ArrayList<Integer> colors, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(colors);
        editor.putString(key, json);
        editor.apply();
    }

    public ArrayList<Integer> getColors(String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Gson gson = new Gson();
        String json = prefs.getString(key, null);
        Type type = new TypeToken<ArrayList<Integer>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void saveAngle(int angle) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(ANGLE_PREF, angle);
        editor.commit();
    }
}
