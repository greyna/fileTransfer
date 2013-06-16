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
import android.annotation.SuppressLint;
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
import android.graphics.Bitmap;
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
	BroadcastReceiver pageReceiver=null;
	private ValueCallback<Uri> mUploadMessage;
	private final static int FILECHOOSER_RESULTCODE = 1;
	public final static int SETTINGS_RESULTCODE = 2;

	// jade
	private SendToTableInterface sendToTableInterface;
	private Logger logger = Logger.getJADELogger(this.getClass().getName());
	private MicroRuntimeServiceBinder microRuntimeServiceBinder = null;
	private ServiceConnection serviceConnection= null;
	private RuntimeCallback<AgentController> agentStartupCallback = new RuntimeCallback<AgentController>() {
		@Override
		public void onSuccess(AgentController a) {
			try {
				popDialog("JADE lancé", "agent "+a.getName()+" correctement lancé");
			} catch (StaleProxyException e) {
				e.printStackTrace();
			}
		}
		@Override
		public void onFailure(Throwable t) {
			popDialog("Echec JADE", t.getMessage());
		}
	};

	String host = "";
	String webPath = "";
	String webPort = "8080";
	String jadePort = "1099";
	String nickname = "arthur";

	SharedPreferences settings;

	//	@Override
	//	protected void onStart() {
	//		super.onStart();
	//
	//		logger.log(Level.INFO, "onstart");
	//		settings = getSharedPreferences("settings", 0);
	//		if (!settings.contains("host")) {
	//			Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
	//			startActivity(i);
	//		} else {
	//			init();
	//			
	//			pageReceiver = new BroadcastReceiver() {
	//				@Override
	//				public void onReceive(Context context, Intent intent) {
	//					webView.loadData(intent.getStringExtra("html"), "text/html", null);
	//				}
	//			};
	//			IntentFilter notificationReloadFilter = new IntentFilter();
	//			notificationReloadFilter.addAction("RELOAD_PAGE");
	//			registerReceiver(pageReceiver, notificationReloadFilter);
	//			if (microRuntimeServiceBinder==null)
	//				startJade(agentStartupCallback);
	//		}
	//	}

	//	@Override
	//	protected void onStop() {
	//
	//		logger.log(Level.INFO, "on stop");
	//		if (pageReceiver!=null) {
	//			unregisterReceiver(pageReceiver);
	//			pageReceiver=null;
	//		}
	//		deleteJade();
	//		super.onStop();
	//		logger.log(Level.INFO, "after on stop");
	//	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//logger.log(Level.INFO, "on create");
		//init();
		init2();
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void init() {
		dlManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
		settings = getSharedPreferences("settings", 0);
		host = settings.getString("host", "");
		jadePort = settings.getString("jade_port", "");
		webPath = settings.getString("directory", "");
		webPort = settings.getString("web_port", "");
		nickname = settings.getString("nickname", "");
		webView = new WebView(this);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setHorizontalScrollBarEnabled(true);
		webView.setVerticalScrollBarEnabled(true);
		webView.setWebChromeClient(new MyWebChromeClient());
		webView.setWebViewClient(new MyWebViewClient());
		setContentView(webView);
	}

	private int mReturnCode;
    private int mResultCode;
    private Intent mResultIntent;
    private boolean mUploadFileOnLoad = false;
	private void init2() {

		setContentView(R.layout.activity_main);
		webView = (WebView) findViewById(R.id.webView);

		webView = new WebView(this);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.loadUrl("http://www.script-tutorials.com/demos/199/index.html");
		webView.setWebViewClient(new myWebClient2());
		webView.setWebChromeClient(new WebChromeClient() {  
			//The undocumented magic method override  
			//Eclipse will swear at you if you try to put @Override here  
			// For Android 3.0+

			public void openFileChooser(ValueCallback<Uri> uploadMsg) {  

				mUploadMessage = uploadMsg;  
				Intent i = new Intent(Intent.ACTION_GET_CONTENT);  
				i.addCategory(Intent.CATEGORY_OPENABLE);  
				i.setType("*/*");  
				MainActivity.this.startActivityForResult(Intent.createChooser(i,"File Chooser"), FILECHOOSER_RESULTCODE);  

			}

			// For Android 3.0+
			public void openFileChooser( ValueCallback uploadMsg, String acceptType ) {
				mUploadMessage = uploadMsg;
				Intent i = new Intent(Intent.ACTION_GET_CONTENT);
				i.addCategory(Intent.CATEGORY_OPENABLE);
				i.setType("*/*");
				MainActivity.this.startActivityForResult(
						Intent.createChooser(i, "File Browser"),
						FILECHOOSER_RESULTCODE);
			}

			//For Android 4.1
			public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture){
				mUploadMessage = uploadMsg;  
				Intent i = new Intent(Intent.ACTION_GET_CONTENT);  
				i.addCategory(Intent.CATEGORY_OPENABLE);  
				i.setType("*/*");  
				MainActivity.this.startActivityForResult( Intent.createChooser( i, "File Chooser" ), MainActivity.FILECHOOSER_RESULTCODE );

			}

		});

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
					Throwable t = new Throwable("Failed to start the container...");
					agentStartupCallback.onFailure(t);
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

	public class myWebClient2 extends WebViewClient
	{
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			// TODO Auto-generated method stub
			super.onPageStarted(view, url, favicon);
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			// TODO Auto-generated method stub

			view.loadUrl(url);
			return true;

		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
		}
	}
	public class MyWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			// return false;     usual behavior
			// return true;      my code

			Uri uri = Uri.parse(url);
			// si upload, laisser le comportement du webChromeClient (POST file chosen)
			if (uri.getHost().equals(host) && uri.getPath()=="/fileTransfer/upload-file") {
				logger.log(Level.INFO, "override upload-file");
				return false;
			}

			// si l'adresse va vers le serveur et si ce n'est pas une upload
			// alors c'est une download
			if (uri.getHost().equals(host)) {
				logger.log(Level.INFO, "override download-file");
				dlManager.enqueue(new Request(uri)/*.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)*/);
				return true;
			}

			// Sinon il s'agit de l'adresse d'une table
			try {
				logger.log(Level.INFO, "override open-file on table");
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
	public class MyWebChromeClient extends WebChromeClient {
		public void openFileChooser(ValueCallback<Uri> uploadMsg, String AcceptType, String capture) {
			this.openFileChooser(uploadMsg);
		}
		public void openFileChooser(ValueCallback<Uri> uploadMsg, String AcceptType) {
			this.openFileChooser(uploadMsg);
		}
		public void openFileChooser(ValueCallback<Uri> uploadMsg) {
			mUploadMessage = uploadMsg;
			Intent i = new Intent(Intent.ACTION_GET_CONTENT);
			i.addCategory(Intent.CATEGORY_OPENABLE);
			i.setType("*/*");
			MainActivity.this.startActivityForResult(Intent.createChooser(i, "file chooser"),FILECHOOSER_RESULTCODE);
		}
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		//logger.log(Level.INFO, "on activity result");
		if(null==mUploadMessage)
	    {
	        mReturnCode = requestCode;
	        mResultCode = resultCode;
	        mResultIntent = intent;
	        mUploadFileOnLoad = true;
	        return;
	    }else
	        mUploadFileOnLoad = false;
		if (requestCode == FILECHOOSER_RESULTCODE) {
			if (null == mUploadMessage)
				return;
			Uri result = intent == null || resultCode != RESULT_OK ? null
					: intent.getData();
			mUploadMessage.onReceiveValue(result);
			mUploadMessage = null;
		}
		//	    else if (requestCode == SETTINGS_RESULTCODE)
		//	    	deleteJade();
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
		if (microRuntimeServiceBinder!=null) {
			logger.log(Level.INFO, "Stopping Jade...");
			microRuntimeServiceBinder
			.stopAgentContainer(new RuntimeCallback<Void>() {
				@Override
				public void onSuccess(Void thisIsNull) {
					logger.log(Level.INFO, "JADE stopped and restarting...");
					popDialog("JADE stoppé", "JADE se relance avec vos nouveaux paramètres...");
					//startJade(agentStartupCallback);
				}
				@Override
				public void onFailure(Throwable throwable) {
					logger.log(Level.SEVERE, "Failed to stop the "
							+ AndroidAgent.class.getName()
							+ "...");
					logger.log(Level.INFO, "JADE cannot stop and restarts...");
					popDialog("JADE not running", "JADE se relance avec vos nouveaux paramètres...");
					//startJade(agentStartupCallback);
				}
			});
		}
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
		//		String content =  "<html><body>" +
		//                "<form action=\"http://172.17.1.34:8080/fileTransfer/upload-file\" " +
		//                "method=\"post\" name=\"uploadForm\" enctype=\"multipart/form-data\">" +
		//                "<p><input name=\"uploadfile\" type=\"file\" size=\"50\"></p>" +
		//                "<p></p><input name=\"submit\" type=\"submit\" value=\"Submit\">" +
		//                "</form></body></html>";
		//		webView.loadData(content, "text/html", null);
		//		logger.log(Level.INFO, "after loaddata");
		return true;
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//	    if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
		//	    	webView.goBack();
		//	        return true;
		//	    }
		finish();
		return true;
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
