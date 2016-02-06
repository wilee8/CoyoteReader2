package com.wilee8.coyotereader2;

import android.app.Application;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

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
	@Override
	public void onCreate() {
		super.onCreate();

		ACRA.init(this);
	}
}
