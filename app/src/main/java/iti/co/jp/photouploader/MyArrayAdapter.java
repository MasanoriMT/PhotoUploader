package iti.co.jp.photouploader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by matoh on 2015/04/16.
 */
public class MyArrayAdapter extends ArrayAdapter<ListItem> {

    private LayoutInflater inflater;

    public MyArrayAdapter(Context context, int resourceId, List<ListItem> items) {
        super(context, resourceId, items);

        this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ListItem item = getItem(position);

        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            view = this.inflater.inflate(R.layout.list_item, null);
        }

        // テキストをセット
        TextView appInfoText = (TextView)view.findViewById(R.id.item_text);
        appInfoText.setText(item.getText());

        // アイコンをセット
        ImageView appInfoImage = (ImageView)view.findViewById(R.id.item_image);
        appInfoImage.setImageResource(item.getImageId());

        return view;
    }
}

