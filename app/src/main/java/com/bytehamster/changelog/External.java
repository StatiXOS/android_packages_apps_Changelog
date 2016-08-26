package com.bytehamster.changelog;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

class External {

    public static void feedbackMail(Context c, String subject, String text) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL, new String[] { "info@bytehamster.com" });
        i.putExtra(Intent.EXTRA_SUBJECT, subject);
        i.putExtra(Intent.EXTRA_TEXT, text);
        try {
            c.startActivity(Intent.createChooser(i, "Send mail..."));
        }
        catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(c, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

}