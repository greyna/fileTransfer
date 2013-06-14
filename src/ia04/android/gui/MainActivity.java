package ia04.android.gui;

import ia04.android.agent.AndroidAgent;
import ia04.android.agent.SendToTableInterface;
import jade.android.AndroidHelper;
import jade.android.MicroRuntimeService;
import jade.android.MicroRuntimeServiceBinder;
import jade.android.RuntimeCallback;
import jade.core.MicroRuntime;
import jade.core.Profile;
import jade.util.Logger;
import jade.util.leap.Properties;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

import java.util.logging.Level;


import utc.ia04.filetransfertotable.R;

import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
	final Activity activity = this;
	WebView webView = null;
	DownloadManager dlManager;
	private ValueCallback<Uri> mUploadMessage;
	private final static int FILECHOOSER_RESULTCODE = 1;
	public final static int SETTINGS_RESULTCODE = 2;
	
	// jade
	private SendToTableInterface sendToTableInterface;
	private Logger logger = Logger.getJADELogger(this.getClass().getName());
	private MicroRuntimeServiceBinder microRuntimeServiceBinder;
	private ServiceConnection serviceConnection;
	private RuntimeCallback<AgentController> agentStartupCallback = new RuntimeCallback<AgentController>() {
		@Override
		public void onSuccess(AgentController arg0) {
			// TODO Auto-generated method stub
		}
		@Override
		public void onFailure(Throwable arg0) {
			// TODO Auto-generated method stub
		}
	};

	String host = "";
	String webPath = "";
	String webPort = "8080";
	String jadePort = "1099";
	String nickname = "arthur";

	SharedPreferences settings;
	@Override
	protected void onStart() {
		super.onStart();
		settings = getSharedPreferences("settings", 0);
		if (!settings.contains("host")) {
			Intent i = new Intent(this, SettingsActivity.class);
			startActivity(i);
		} else {
			host = settings.getString("host", "");
			jadePort = settings.getString("jade_port", "");
			webPath = settings.getString("directory", "");
			webPort = settings.getString("web_port", "");
			nickname = settings.getString("nickname", "");
			webView.loadUrl("http://" + host + ":" + webPort + webPath);
			
			BroadcastReceiver reloadReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					webView.reload();
				}
			};
			IntentFilter notificationReloadFilter = new IntentFilter();
			notificationReloadFilter.addAction("RELOAD_PAGE");
			registerReceiver(reloadReceiver, notificationReloadFilter);
			startJade(agentStartupCallback);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);setContentView(R.layout.activity_main);
		dlManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
		settings = getSharedPreferences("settings", 0);
		host = settings.getString("host", "");
		jadePort = settings.getString("jade_port", "");
		webPath = settings.getString("directory", "");
		webPort = settings.getString("web_port", "");
		nickname = settings.getString("nickname", "");
		webView = new WebView(this);
		webView.setHorizontalScrollBarEnabled(true);
		webView.setVerticalScrollBarEnabled(true);
		webView.setWebChromeClient(new MyWebChromeClient());
		webView.setWebViewClient(new MyWebViewClient());
		webView.loadUrl("http://" + host + ":" + webPort + webPath);
		setContentView(webView);
	}
	
	
	
	public void startJade(final RuntimeCallback<AgentController> agentStartupCallback) {

		final Properties profile = new Properties();
		profile.setProperty(Profile.MAIN_HOST, host);
		profile.setProperty(Profile.MAIN_PORT, jadePort);
		profile.setProperty(Profile.MAIN, Boolean.FALSE.toString());
		profile.setProperty(Profile.JVM, Profile.ANDROID);

		if (AndroidHelper.isEmulator()) {
			// Emulator: this is needed to work with emulated devices
			profile.setProperty(Profile.LOCAL_HOST, AndroidHelper.LOOPBACK);
		} else {
			profile.setProperty(Profile.LOCAL_HOST,
					AndroidHelper.getLocalIPAddress());
		}
		// Emulator: this is not really needed on a real device
		profile.setProperty(Profile.LOCAL_PORT, "2000");

		if (microRuntimeServiceBinder == null) {
			serviceConnection = new ServiceConnection() {
				public void onServiceConnected(ComponentName className,
						IBinder service) {
					microRuntimeServiceBinder = (MicroRuntimeServiceBinder) service;
					logger.log(Level.INFO, "Gateway successfully bound to MicroRuntimeService");
					startContainer(nickname, profile, agentStartupCallback);
				};

				public void onServiceDisconnected(ComponentName className) {
					microRuntimeServiceBinder = null;
					logger.log(Level.INFO, "Gateway unbound from MicroRuntimeService");
				}
			};
			logger.log(Level.INFO, "Binding Gateway to MicroRuntimeService...");
			bindService(new Intent(getApplicationContext(),
					MicroRuntimeService.class), serviceConnection,
					Context.BIND_AUTO_CREATE);
		} else {
			logger.log(Level.INFO, "MicroRumtimeGateway already binded to service");
			startContainer(nickname, profile, agentStartupCallback);
		}
	}

	private void startContainer(final String nickname, Properties profile,
			final RuntimeCallback<AgentController> agentStartupCallback) {
		if (!MicroRuntime.isRunning()) {
			microRuntimeServiceBinder.startAgentContainer(profile,
					new RuntimeCallback<Void>() {
				@Override
				public void onSuccess(Void thisIsNull) {
					logger.log(Level.INFO, "Successfully start of the container...");
					startAgent(nickname, agentStartupCallback);
				}

				@Override
				public void onFailure(Throwable throwable) {
					logger.log(Level.SEVERE, "Failed to start the container...");
				}
			});
		} else {
			startAgent(nickname, agentStartupCallback);
		}
	}

	private void startAgent(final String nickname, final RuntimeCallback<AgentController> agentStartupCallback) {
		microRuntimeServiceBinder.startAgent(nickname,
				AndroidAgent.class.getName(),
				new Object[] { getApplicationContext() },
				new RuntimeCallback<Void>() {
			@Override
			public void onSuccess(Void thisIsNull) {
				logger.log(Level.INFO, "Successfully start of the "
						+ AndroidAgent.class.getName() + "...");
				try {
					agentStartupCallback.onSuccess(MicroRuntime
							.getAgent(nickname));
				} catch (ControllerException e) {
					// Should never happen
					agentStartupCallback.onFailure(e);
				}
			}

			@Override
			public void onFailure(Throwable throwable) {
				logger.log(Level.SEVERE, "Failed to start the "
						+ AndroidAgent.class.getName() + "...");
				agentStartupCallback.onFailure(throwable);
			}
		});
	}
	
	private class MyWebViewClient extends WebViewClient {
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    	// return false;     usual behavior
	    	// return true;      my code

	    	Uri uri = Uri.parse(url);
	    	// si upload, laisser le comportement du webChromeClient (fileChooser)
	    	if (uri.getHost().equals(host) && uri.getPath()=="/tomcat_test/upload-file") {
	    		return false;
	        }
	    	
	    	// si l'adresse va vers le serveur et si ce n'est pas une upload
	    	// alors c'est une download
	    	if (uri.getHost().equals(host)) {
		    	dlManager.enqueue(new Request(Uri.parse(url)).setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED));
		        return true;
	    	}

	    	// Sinon il s'agit de l'adresse d'une table
	    	try {
	    		sendToTableInterface = MicroRuntime.getAgent(nickname)
	    				.getO2AInterface(SendToTableInterface.class);
	    		sendToTableInterface.sendToTable(uri.getPath().substring(1), uri.getHost());
	    	} catch (StaleProxyException e) {
	    		e.printStackTrace();
	    	} catch (ControllerException e) {
	    		e.printStackTrace();
	    	}
	    	return true;
	    }
	}
	protected class MyWebChromeClient extends WebChromeClient {
		public void openFileChooser(ValueCallback<Uri> uploadMsg, String AcceptType, String capture) {
			this.openFileChooser(uploadMsg);
		}
		public void openFileChooser(ValueCallback<Uri> uploadMsg, String AcceptType) {
			this.openFileChooser(uploadMsg);
		}
		public void openFileChooser(ValueCallback<Uri> uploadMsg) {
	        mUploadMessage = uploadMsg;
	        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
	        i.setType("*/*");
	        startActivityForResult(Intent.createChooser(i, "file chooser"),FILECHOOSER_RESULTCODE);
	    }
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
	        Intent intent) {
	    if (requestCode == FILECHOOSER_RESULTCODE) {
	        if (null == mUploadMessage)
	            return;
	        Uri result = intent == null || resultCode != RESULT_OK ? null
	                : intent.getData();
	        mUploadMessage.onReceiveValue(result);
	        mUploadMessage = null;
	    }
	    else if (requestCode == SETTINGS_RESULTCODE)
	    	deleteJade();
	}

	
	protected void popDialog(String title, String message) {
    	// 1. Instantiate an AlertDialog.Builder with its constructor
    	AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
    	// 2. Chain together various setter methods to set the dialog characteristics
    	builder.setMessage(message).setTitle(title);
    	// 3. Get the AlertDialog from create()
    	AlertDialog dialog = builder.create();
    	
    	dialog.show();
	}
	public void deleteJade() {
			logger.log(Level.INFO, "Stopping Jade...");
			microRuntimeServiceBinder
			.stopAgentContainer(new RuntimeCallback<Void>() {
				@Override
				public void onSuccess(Void thisIsNull) {
					//startJade(agentStartupCallback);
				}

				@Override
				public void onFailure(Throwable throwable) {
					logger.log(Level.SEVERE, "Failed to stop the "
							+ AndroidAgent.class.getName()
							+ "...");
				}
			});
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i = new Intent(this, SettingsActivity.class);
		startActivity(i);
//		try {
//    		sendToTableInterface = MicroRuntime.getAgent(nickname)
//    				.getO2AInterface(SendToTableInterface.class);
//    		sendToTableInterface.sendToTable("lol.jpg", "Table");
//    	} catch (StaleProxyException e) {
//    		e.printStackTrace();
//    	} catch (ControllerException e) {
//    		e.printStackTrace();
//    	}
		return true;
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
	    	webView.goBack();
	        return true;
	    }
	    finish();
	    return true;
	}
	@Override
	protected void onDestroy() {
		deleteJade();
		super.onDestroy();
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getWebPath() {
		return webPath;
	}
	public void setWebPath(String webPath) {
		this.webPath = webPath;
	}
	public String getWebPort() {
		return webPort;
	}
	public void setWebPort(String webPort) {
		this.webPort = webPort;
	}
	public String getJadePort() {
		return jadePort;
	}
	public void setJadePort(String jadePort) {
		this.jadePort = jadePort;
	}
	public String getNickname() {
		return nickname;
	}
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
}
