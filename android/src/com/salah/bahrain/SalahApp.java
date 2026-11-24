package com.salah.bahrain;

import android.app.Application;
import android.os.Build;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

/** Saves the stack trace of any crash so the next launch can show it (and the user can send it). */
public class SalahApp extends Application {
    static File crashFile(android.content.Context c) { return new File(c.getFilesDir(), "crash.txt"); }

    @Override public void onCreate() {
        super.onCreate();
        final Thread.UncaughtExceptionHandler old = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                try (FileWriter w = new FileWriter(crashFile(this))) {
                    w.write("Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\n"
                            + Build.MANUFACTURER + " " + Build.MODEL + "\n"
                            + "Thread: " + t.getName() + "\n\n" + sw);
                }
            } catch (Throwable ignored) { }
            if (old != null) old.uncaughtException(t, e);
        });
    }
}
