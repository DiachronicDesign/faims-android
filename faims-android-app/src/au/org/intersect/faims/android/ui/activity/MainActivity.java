package au.org.intersect.faims.android.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.util.FAIMSLog;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FAIMSLog.log();
        
        setContentView(R.layout.activity_main);
        
        // Need to set the application to get state information
        ServerDiscovery.getInstance().setApplication(getApplication());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.fetch_project:
				fetchProjectsFromServer();
				return (true);
			default:
				return (super.onOptionsItemSelected(item));
		}
	}
	
	/**
	 * Open a new activity and shows a list of projects from the server
	 */
	private void fetchProjectsFromServer(){
		FAIMSLog.log();
		
		Intent fetchProjectsIntent = new Intent(MainActivity.this, FetchProjectsActivity.class);
		MainActivity.this.startActivityForResult(fetchProjectsIntent,1);
	}
    
}
