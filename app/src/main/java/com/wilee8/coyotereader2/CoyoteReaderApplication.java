package com.wilee8.coyotereader2;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.util.concurrent.locks.ReentrantLock;

@ReportsCrashes(mailTo = "coyote.reader.rss@gmail.com",
	customReportContent = {ReportField.BRAND,
						   ReportField.PHONE_MODEL,
						   ReportField.ANDROID_VERSION,
						   ReportField.USER_COMMENT,
						   ReportField.STACK_TRACE
	},
	mode = ReportingInteractionMode.DIALOG,
	resDialogText = R.string.crash_dialog_text,
	resDialogTitle = R.string.crash_dialog_title,
	resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
	resDialogOkToast = R.string.crash_dialog_ok_toast
)

public class CoyoteReaderApplication extends Application {

	public static Context mContext;

	private static ReentrantLock mutex;

	@Override
	public void onCreate() {
		super.onCreate();

		ACRA.init(this);

		mContext = this;

		mutex = new ReentrantLock();
	}

	public static Context getContext() {
		return mContext;
	}

	public static void headerLock() {
		mutex.lock();
	}

	public static void headerUnlock() {
		mutex.unlock();
	}
}
