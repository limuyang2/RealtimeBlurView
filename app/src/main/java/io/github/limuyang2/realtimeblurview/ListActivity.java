package io.github.limuyang2.realtimeblurview;

import android.os.Bundle;

/**
 * Created by mmin18 on 02/11/2017.
 */

public class ListActivity extends android.app.ListActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setListAdapter(new MyListAdapter(this, R.layout.list_item_blur));
	}
}
