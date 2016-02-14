package ai.wit.eval.wit_eval;

import android.app.SearchManager;
import android.content.Entity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.telephony.SmsManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.util.ArrayList;

import ai.wit.sdk.IWitListener;
import ai.wit.sdk.Wit;
import ai.wit.sdk.model.WitOutcome;


public class MainActivity extends ActionBarActivity implements IWitListener {

    // Declarations
    Wit _wit;
    SmsManager smsManager;
    String _accessToken = "P45E2RB5MMMLWGBSBNXBPP5TTJX2ALDF";
    String phoneNo = "9730195442";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _wit = new Wit(_accessToken, this);
        _wit.enableContextLocation(getApplicationContext());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Change mic UI on listening
    public void toggle(View v) {
        try {
            _wit.toggleListening();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


//Send message without android screen : automatic
    protected void sendSMSMessage(String message) {

        try {
            smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, message, null, null);
            Toast.makeText(getApplicationContext(), "SMS sent.", Toast.LENGTH_LONG).show();
        }
        catch (Exception e) {
            Toast.makeText(getApplicationContext(), "SMS faild, please try again.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    //
    protected void googleSearch(String searchtext) {

        try {
            Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
            intent.putExtra(SearchManager.QUERY, searchtext);
            startActivity(intent);
        } catch (Exception e) {
            Log.d("Tag", "search google intent error");
        }
    }

    // send sms through android message sending screen
    protected void sendSMS(String messagebody) {
        Intent smsIntent = new Intent(Intent.ACTION_VIEW);
        smsIntent.setData(Uri.parse("smsto:"));
        smsIntent.setType("vnd.android-dir/mms-sms");
        smsIntent.putExtra("address", "9730195442");
        smsIntent.putExtra("sms_body", messagebody);

        try {
            startActivity(smsIntent);
            Log.i("Finished sending SMS...", "");
        }
        catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this,
                    "SMS faild, please try again later.", Toast.LENGTH_SHORT).show();
        }
    }

    // Wit.ai overridden functions
    @Override
    public void witDidStartListening() {
        ((TextView) findViewById(R.id.txtText)).setText("Witting...");
    }

    @Override
    public void witDidStopListening() {
        ((TextView) findViewById(R.id.txtText)).setText("Processing...");
    }

    @Override
    public void witActivityDetectorStarted() {
        ((TextView) findViewById(R.id.txtText)).setText("Listening");
    }

    @Override
    public String witGenerateMessageId() {
        return null;
    }

    // On result from wit.ai server parse the intent
    @Override
    public void witDidGraspIntent(ArrayList<WitOutcome> witOutcomes, String messageId, Error error) {
        String spokentext = "";
        String messagebody = "";
        TextView jsonView = (TextView) findViewById(R.id.jsonView);
        TextView speechText = (TextView) findViewById(R.id.txtText);
        jsonView.setMovementMethod(new ScrollingMovementMethod());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (error != null) {
            jsonView.setText(error.getLocalizedMessage());
            return;
        }
        String jsonOutput = gson.toJson(witOutcomes);
        // Looping WitOutcome
        for (WitOutcome witoutput : witOutcomes) {
            spokentext = witoutput.get_text();
            if (witoutput.get_intent().equals("search_google")) {
                if (witoutput.get_entities().containsKey("search_query")) {
                    JsonElement enitities = witoutput.get_entities().get("search_query");
                    String search_text = Entityparse(enitities);
                    googleSearch(search_text);
                }
            }else if (witoutput.get_intent().equals("send")) {
                if (witoutput.get_entities().containsKey("message_body")) {
                    JsonElement enitities = witoutput.get_entities().get("message_body");
                    messagebody = Entityparse(enitities);
                    sendSMS(messagebody);
                }
            } else {
                Toast.makeText(MainActivity.this,
                        "More training required", Toast.LENGTH_SHORT).show();
            }

            jsonView.setText(jsonOutput);
            ((TextView) findViewById(R.id.txtText)).setText(spokentext);
        }
    }

    // Parsing the entities in the witoutcome json
    public String Entityparse(JsonElement entities) {
        JsonArray array = entities.getAsJsonArray();
        String body = "";
        for (JsonElement entity : array) {
            body = entity.getAsJsonObject().get("value").toString();
        }
        return body;
    }

    public static class PlaceholderFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            return inflater.inflate(R.layout.wit_button, container, false);
        }
    }

}
