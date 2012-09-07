package ru.ufalinux.tasp;

import java.util.Vector;

import ru.ufalinux.tasp.dataworks.Order;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class OrderListAdapter extends BaseAdapter{

	private Context context;
	private Vector<Order>orders=new Vector<Order>();
	
	public void clear(){
		orders.clear();
	}
	
	public void add(Order curr){
		orders.add(curr);
	}
	
	public void setData(Vector<Order> vec){
		orders=vec;
	}
	
	public int getCount() {
		return orders.size();
	}

	public OrderListAdapter(Context context) {
		this.context = context;
	}

	public Object getItem(int pos) {
		if (pos >= 0 && pos < orders.size())
			return orders.get(pos);
		return null;
	}

	public long getItemId(int pos) {
		if (orders.isEmpty())
			return 0;
		return orders.get(pos).id;
	}

	public View getView(int pos, View convertView, ViewGroup parent) {
		View view = null;

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
		TextView labelView = (TextView) view
				.findViewById(R.id.orderListLabelMain);
		try{
		Order curr = orders.get(pos);

		String mainLabel = curr.addressfrom;
		if(!curr.time.equals("99-99"))
			mainLabel+="\nна "+curr.time;
		labelView.setText(mainLabel);
		}catch(Exception e){
			e.printStackTrace();
		}
	}


}
