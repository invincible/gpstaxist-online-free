package ru.ufalinux.tasp;

import java.util.Vector;

import ru.ufalinux.tasp.dataworks.Data;
import ru.ufalinux.tasp.dataworks.Driverstate;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class StatesListAdapter extends BaseAdapter{

	private Context context;
	private Vector<Driverstate>states=new Vector<Driverstate>();
	
	public void clear(){
		states.clear();
	}
	
	public void add(Driverstate curr){
		states.add(curr);
	}
	
	public void setData(Vector<Driverstate> vec){
		states=vec;
	}
	
	public int getCount() {
		return states.size();
	}

	public StatesListAdapter(Context context) {
		this.context = context;
	}

	public Object getItem(int pos) {
		if (pos >= 0 && pos < states.size())
			return states.get(pos);
		return null;
	}

	public long getItemId(int pos) {
		if (states.isEmpty())
			return 0;
		return states.get(pos).id;
	}

	public View getView(int pos, View convertView, ViewGroup parent) {
		View view = null;
		Log.d(Data.TAG, "Get vIew");
		// Проверяем существуествование объекта для текущеё позиции
		if (convertView != null) {
			// Если существует то, берем текущий объект
			view = convertView;
		} else {
			// Не существует создаем новый =)
			view = newView(context, parent);
		}

		// Отправляем на инициализацию визуальных компонентов
		bindView(pos, view);

		// Отдаем созданное View списку
		return view;
	}

	private View newView(Context context, ViewGroup parent) {
		// Класс позволяющий создавать View основе XML описания
		LayoutInflater layoutInflater = LayoutInflater.from(context);
		// Создаем View из описания list_item.xml, parent - родительский View,
		// false - не добавлять объект в иерархию
		return layoutInflater.inflate(R.layout.order_list_item, parent, false);
	}

	private void bindView(int pos, View view) {
		// Получаем TextView из родительского View по ID для изменения
		// параметров объекта
		Log.d(Data.TAG, "Bind vIew");
		TextView labelView = (TextView) view
				.findViewById(R.id.orderListLabelMain);
		Driverstate curr = states.get(pos);

//		String mainLabel = curr.id+" "+curr.name;
		String mainLabel = curr.name;
		labelView.setText(mainLabel);
	}


}
