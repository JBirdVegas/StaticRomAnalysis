package com.n00bware.StaticRomAnalysis;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Results extends Activity {
    String  TAG   = getClass().getSimpleName();
    boolean DEBUG = true;
    TextView mRomName;
    TextView mStaticResults;
    Class   settingsProviderClass      = null;
    Class[] systemClassDeclaredClasses = null;
    Context mContext;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        setContentView(R.layout.main);
        LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
        mRomName = (TextView) layout.findViewById(R.id.rom_name);
        mStaticResults = (TextView) layout.findViewById(R.id.results);
        mRomName.setText(null);
        mStaticResults.setText(null);
        try {
            settingsProviderClass = Class.forName("android.provider.Settings");
            systemClassDeclaredClasses = settingsProviderClass.getDeclaredClasses();
        }
        catch (ClassNotFoundException e) {
            // shit!!!
        }
        mRomName.setText(getBestName());
        mStaticResults.setText(Integer.toString(getDeclaredFields().size()));

    }

    public List<String> getDeclaredFields()
    {
        List<String> returnList = new ArrayList<String>(0);
        for (Class foundClasses : systemClassDeclaredClasses) {
            // get all the fields
            Field[] fields = foundClasses.getDeclaredFields();
            for (Field field : fields) {
                // name of field
                String fieldName = field.getName();
                String name = "NAME";
                String value = "VALUE";

                // all wanted values should have UPPERCASE letters
                char firstLetter = fieldName.charAt(0);
                if (!Character.isLowerCase(firstLetter)) {
                    // populate ArrayList with valid entries
                    returnList.add(field.getName());
                    if (DEBUG)
                        Log.d(TAG, "found field: " + field.getName());
                }

                // don't include some constants we don't need
                if (returnList.contains("NAME"))
                    returnList.remove("NAME");
                if (returnList.contains("VALUE"))
                    returnList.remove("VALUE");
                if (returnList.contains("SELECT_VALUE"))
                    returnList.remove("SELECT_VALUE");
                if (returnList.contains("NAME_EQ_PLACEHOLDER"))
                    returnList.remove("NAME_EQ_PLACEHOLDER");
                if (returnList.contains("SYS_PROP_SETTING_VERSION"))
                    returnList.remove("SYS_PROP_SETTING_VERSION");
                if (returnList.contains("MOVED_TO_SECURE"))
                    returnList.remove("MOVED_TO_SECURE");
            }
        }
        return returnList;
    }

    public String findBuildPropValueOf(Context mContext, String prop)
    {
        String mBuildPath = mContext.getString(R.string.buildprop_path);
        String value = null;
        try {
            //create properties construct and load build.prop
            Properties mProps = new Properties();
            mProps.load(new FileInputStream(mBuildPath));
            //get the property
            value = mProps.getProperty(prop, null);
            Log.d(TAG, String.format(mContext.getString(R.string.log_found_build_prop_value), prop, value));
        }
        catch (IOException ioe) {
            Log.d(TAG, mContext.getString(R.string.inputstream_load_failure));
        }
        catch (NullPointerException npe) {
            npe.getMessage();
        }

        if (value != null) {
            return value;
        }
        else {
            return null;
        }
    }

    public String getBestName()
    {
        ArrayList<String> checkOptions = new ArrayList<String>(0);
        checkOptions.add(findBuildPropValueOf(mContext, "ro.modversion")); // most prefered
        checkOptions.add(findBuildPropValueOf(mContext, "ro.goo.rom"));
        checkOptions.add(findBuildPropValueOf(mContext, "ro.goo.developerid"));
        checkOptions.add(findBuildPropValueOf(mContext, "ro.rommanager.developerid"));
        checkOptions.add(findBuildPropValueOf(mContext, "ro.build.display.id")); // least prefered
        for (String s : checkOptions) {
            if (s != null) {
                return stringWithoutSpaces(s);
            }
        }
        return "Failed to find ROM name";
    }

    public String stringWithoutSpaces(String string)
    {
        return string.replaceAll(" ", "_").replaceAll(":", "").trim();
    }
}
