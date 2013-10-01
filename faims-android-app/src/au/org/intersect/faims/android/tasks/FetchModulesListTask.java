package au.org.intersect.faims.android.tasks;

import android.os.AsyncTask;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.FetchResult;


public class FetchModulesListTask extends AsyncTask<Void, Void, Void> {

	private FAIMSClient faimsClient;
	private ITaskListener listener;
	
	private FetchResult result;
	
	public FetchModulesListTask(FAIMSClient faimsClient, ITaskListener listener) {
		this.faimsClient = faimsClient;
		this.listener = listener;
	}
	
	@Override
	protected Void doInBackground(Void... values) {
		
		result = faimsClient.fetchModuleList();
	
		return null;
	}
	
	@Override
	protected void onPostExecute(Void v) {
		
		if (result.resultCode == FAIMSClientResultCode.FAILURE) {
			faimsClient.invalidate();
		}
		
		listener.handleTaskCompleted(result);
	}
	
}
