/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop 
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
 *****************************************************************/

package ia04.android.gui;

import utc.ia04.filetransfertotable.R;
import jade.util.leap.Properties;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class SettingsActivity extends Activity {
	Properties properties;
	EditText hostField;
	EditText jade_portField;
	EditText web_portField;
	EditText directoryField;
	EditText nicknameField;
	SharedPreferences settings;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		settings = getSharedPreferences("settings", 0);
		
		String host = settings.getString("host", "");
		String jade_port = settings.getString("jade_port", "");
		String directory = settings.getString("directory", "");
		String web_port = settings.getString("web_port", "");
		String nickname = settings.getString("nickname", "");

		hostField = (EditText) findViewById(R.id.edit_host);
		hostField.setText(host);

		jade_portField = (EditText) findViewById(R.id.edit_jade_port);
		jade_portField.setText(jade_port);

		directoryField = (EditText) findViewById(R.id.edit_directory);
		directoryField.setText(directory);

		web_portField = (EditText) findViewById(R.id.edit_web_port);
		web_portField.setText(web_port);

		nicknameField = (EditText) findViewById(R.id.edit_nickname);
		nicknameField.setText(nickname);

		OnClickListener buttonUseListener = new OnClickListener() {
			public void onClick(View v) {
				// TODO: Verify that edited parameters was formally correct
				SharedPreferences.Editor editor = settings.edit();
				editor.putString("host", hostField.getText().toString());
				editor.putString("jade_port", jade_portField.getText().toString());
				editor.putString("directory", directoryField.getText().toString());
				editor.putString("web_port", web_portField.getText().toString());
				editor.putString("nickname", nicknameField.getText().toString());
				editor.apply();
				editor.commit();
				finish();
			}
		};
		Button button = (Button) findViewById(R.id.button_submit);
		button.setOnClickListener(buttonUseListener);
	}
	protected void popDialog(String title, String message) {
    	// 1. Instantiate an AlertDialog.Builder with its constructor
    	AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
    	// 2. Chain together various setter methods to set the dialog characteristics
    	builder.setMessage(message).setTitle(title);
    	// 3. Get the AlertDialog from create()
    	AlertDialog dialog = builder.create();
    	
    	dialog.show();
	}
}
