package com.bytehamster.changelog;

import java.util.Map;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;

import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.w3c.dom.Text;

class Dialogs {
    
    public static void usingCacheAlert(final Activity activity, final String gerrit_url,
                                       final DialogInterface.OnClickListener retryPressedListener){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder b = new AlertDialog.Builder(activity);
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
                b.setNeutralButton(R.string.retry, retryPressedListener);
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


        final TextView messageView = new TextView(a);
        final SpannableString s = new SpannableString(Html.fromHtml(msg));
        messageView.setText(s);

        final float scale = a.getResources().getDisplayMetrics().density;
        int padding_in_px = (int) (24 * scale + 0.5f);

        messageView.setPadding(padding_in_px, padding_in_px, padding_in_px, padding_in_px);
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        messageView.setTextColor(0xff000000);
        messageView.setMovementMethod(LinkMovementMethod.getInstance());


        AlertDialog.Builder d = new AlertDialog.Builder(a);
        d.setCancelable(true);
        d.setTitle(R.string.change);
        d.setView(messageView);
        d.setNegativeButton("Gerrit", null);
        d.setPositiveButton(R.string.ok, null);
        Dialog dlg = d.create();
        dlg.setCanceledOnTouchOutside(true);

        dlg.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Uri uri = Uri.parse(gerrit_url + "#/c/" + change.get("number"));
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        a.startActivity(intent);
                    }
                });
            }
        });

        dlg.show();
	}
}