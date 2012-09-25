package com.n00bware.StaticRomAnalysis;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class Results extends Activity {
    String  TAG   = getClass().getSimpleName();
    String VERSION_NAME;
    int VERSION_CODE;
    boolean DEBUG = true;
    Button mSendEmail;
    TextView mRomName;
    TextView mStaticResults;
    Class   settingsProviderClass      = null;
    Class[] systemClassDeclaredClasses = null;
    Context mContext;
    String mLineEndings = System.getProperty("line.separator");

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        try {
            VERSION_NAME = this.getPackageManager()
                .getPackageInfo(this.getPackageName(), 0).versionName;
            VERSION_CODE = this.getPackageManager()
                .getPackageInfo(this.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        setContentView(R.layout.main);
        LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
        mRomName = (TextView) layout.findViewById(R.id.rom_name);
        mStaticResults = (TextView) layout.findViewById(R.id.results);
        mSendEmail = (Button) layout.findViewById(R.id.send_email_button);
        mSendEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendEmail();
            }
        });
        mRomName.setText(null);
        mStaticResults.setText(null);
        try {
            settingsProviderClass = Class.forName("android.provider.Settings");
            systemClassDeclaredClasses = settingsProviderClass.getDeclaredClasses();
        } catch (ClassNotFoundException e) {
            // shit!!!
        }
        mRomName.setText(getBestName());
        mStaticResults.setText(Integer.toString(getDeclaredFields().size()));
    }

    private List<String> getDeclaredFields() {
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

    private String findBuildPropValueOf(Context mContext, String prop) {
        String mBuildPath = mContext.getString(R.string.buildprop_path);
        String value = null;
        try {
            //create properties construct and load build.prop
            Properties mProps = new Properties();
            mProps.load(new FileInputStream(mBuildPath));
            //get the property
            value = mProps.getProperty(prop, null);
            Log.d(TAG, String.format(mContext.getString(R.string.log_found_build_prop_value), prop, value));
        } catch (IOException ioe) {
            Log.d(TAG, mContext.getString(R.string.inputstream_load_failure));
        } catch (NullPointerException npe) {
            npe.getMessage();
        }

        if (value != null) {
            return value;
        } else {
            return null;
        }
    }

    private String getBestName() {
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

    private String stringWithoutSpaces(String string) {
        return string.replaceAll(" ", "_").replaceAll(":", "").trim();
    }

    private void sendEmail() {
        // from http://stackoverflow.com/a/1279574/873237
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("plain/text");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { "testn00bware.staticromanalysis@gmail.com" });
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getClass().getCanonicalName() + "_TEST_RESULTS");
        emailIntent.putExtra(Intent.EXTRA_TEXT, formulateResultsForEmail());
        Log.v(getClass().getSimpleName(), "message=" + formulateResultsForEmail());
        startActivity(Intent.createChooser(emailIntent, "Send mail..."));
    }

    private String formulateResultsForEmail() {
        StringBuilder stringBuilder = new StringBuilder(0);
        stringBuilder.append("ROM nomenclature values found: " + mLineEndings);
        stringBuilder.append(mLineEndings);
        stringBuilder.append("ro.modversion: " + findBuildPropValueOf(mContext, "ro.modversion")); // most prefered
        stringBuilder.append(mLineEndings);
        stringBuilder.append("ro.goo.rom: " + findBuildPropValueOf(mContext, "ro.goo.rom"));
        stringBuilder.append(mLineEndings);
        stringBuilder.append("ro.goo.developerid: " + findBuildPropValueOf(mContext, "ro.goo.developerid"));
        stringBuilder.append(mLineEndings);
        stringBuilder.append("ro.rommanager.developerid: " + findBuildPropValueOf(mContext, "ro.rommanager.developerid"));
        stringBuilder.append(mLineEndings);
        stringBuilder.append("ro.build.display.id: " + findBuildPropValueOf(mContext, "ro.build.display.id")); // least prefered
        stringBuilder.append(mLineEndings + mLineEndings);

        stringBuilder.append("Fields found: " + getDeclaredFields().size() + mLineEndings);
        stringBuilder.append("Fields list:" + mLineEndings);
            for (String key : getDeclaredFields()) {
                stringBuilder.append(key + mLineEndings);
            }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
        stringBuilder.append(mLineEndings + mLineEndings + "RESULTS SENT: "
            + simpleDateFormat.format(new Date(System.currentTimeMillis())));
        stringBuilder.append(mLineEndings);
        stringBuilder.append("version { " + VERSION_CODE + ", " + VERSION_NAME + " }");
        return stringBuilder.toString();
    }
}