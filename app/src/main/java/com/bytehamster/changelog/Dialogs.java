package com.bytehamster.changelog;

import java.util.Map;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;

import com.bytehamster.lib.MaterialDialog.MaterialDialog;

class Dialogs {
    
    public static void usingCacheAlert(final Activity activity, final String gerrit_url){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MaterialDialog b = new MaterialDialog(activity, R.color.color_primary);
                b.setTitle(R.string.network_error);
                b.setMessage(R.string.using_cache);
                b.setCancelable(true);

                b.setNegativeButton(R.string.open_website, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(gerrit_url));
                        activity.startActivity(browserIntent);
                    }
                });
                b.setPositiveButton(R.string.ok, null);
                b.show();
            }
        });
    }
	
	public static void changeDetails(final Activity a, final Map<String, Object> change, final String gerrit_url){
		String msg = a.getResources().getString(R.string.change_content);
		msg = msg.replace("%title", (String)change.get("title"));
		msg = msg.replace("%owner", (String)change.get("owner"));
    	msg = msg.replace("%date", (String)change.get("dateFull"));
        msg = msg.replace("%project", (String)change.get("project"));
        msg = msg.replace("%branch", (String)change.get("branch"));
    	
    	String message = (String)change.get("message");
    	message = message.substring(message.indexOf('\n')+1);
        message = message.replaceAll("(\\A|\\s)((http|https|ftp|mailto):\\S+)(?=\\s|\\z)","$1<a href=\"$2\">$2</a>");
    	message = message.trim();
    	
    	if(message.startsWith("Change-Id"))
    		message = "";
    	else if(message.lastIndexOf("\nChange-Id") != -1)
	    	message = message.substring(0,message.lastIndexOf("\nChange-Id"));
    	message = message.trim();
    	
    	if(message.startsWith("Signed-off-by"))
    		message = "";
    	else if(message.lastIndexOf("\nSigned-off-by") != -1)
	    	message = message.substring(0,message.lastIndexOf("\nSigned-off-by"));
    	message = message.trim();
    	
    	if(message.equals(""))
    		message = a.getResources().getString(R.string.no_message);
    	
    	msg = msg.replace("%message", message.replace("\n","<br />"));


        MaterialDialog d = new MaterialDialog(a, R.color.color_primary);
        d.setCancelable(true);
        d.setCanceledOnTouchOutside(true);
        d.setTitle(R.string.change);
        d.setMessage(Html.fromHtml(msg));
        d.setNegativeButton("Gerrit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Uri uri = Uri.parse(gerrit_url + "#/c/" + change.get("number"));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                a.startActivity(intent);
            }
        });
        d.setButton1AutoClose(false);
        d.setPositiveButton(R.string.ok, null);
        d.enableLinks();
        d.show();
	}
}