package de.mannodermaus.transformtest;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.List;

public class MyActivity extends Activity {

    private static final List<Object> OBJECT_LIST = List.of(
            new Object(), new Object(), new Object(), new Object(),
            new Object(), new Object(), new Object(), new Object(),
            new Object(), new Object(), new Object(), new Object(),
            new Object(), new Object(), new Object(), new Object());

    private final List<Float> emptyList = List.of();
    private final List<Integer> intList = List.of(1, 2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        List<String> items = List.of("a", "b", "c");
        List<String> kotlinList = MyKotlinFileKt.listFromKotlin();

        TextView textView = findViewById(R.id.textView);
        appendListInfo(textView, "items", String.class, items);
        appendListInfo(textView, "emptyList", Float.class, emptyList);
        appendListInfo(textView, "intList", Integer.class, intList);
        appendListInfo(textView, "kotlinList", String.class, kotlinList);
        appendListInfo(textView, "OBJECT_LIST", Object.class, OBJECT_LIST);

    }

    private void appendListInfo(TextView textView, String name, Class expectedClass, List list) {
        if (!list.isEmpty()) {
            Object item = list.get(0);
            if (!expectedClass.equals(item.getClass())) {
                textView.append("Type Mismatch for " + name + ": Expected " + expectedClass + ", but was " + item.getClass() + "\n");
                return;
            }
        }

        textView.append(name + "=" + list + " (size=" + list.size() + ")\n");
    }
}
