package net.joshuahughes.imagej.parameter;

import java.awt.Color;
import java.awt.Component;
import java.awt.Paint;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;


public class PaintBoxParameter extends JComboBox implements Parameter<Paint,JComboBox>{
	private static final long serialVersionUID = 726104080593868769L;
	LinkedHashMap<String,Paint> paintMap = new LinkedHashMap<String,Paint>();
	@SuppressWarnings("unchecked")
	public PaintBoxParameter(String name){
		setName(name);
		for(Field field : Color.class.getFields())
			if(field.getDeclaringClass().equals(Color.class))
				try {
					paintMap.put(field.getName(),(Color) field.get(null));
				} catch (Exception e) {
					e.printStackTrace();
				}
		setModel(new DefaultComboBoxModel(new ArrayList<Entry<String,Paint>>(paintMap.entrySet()).toArray(new Entry[]{})));
		setRenderer(new ListCellRenderer(){

			public Component getListCellRendererComponent(JList list,Object value, int index, boolean isSelected,boolean cellHasFocus) {
				JLabel lbl = new JLabel(((Entry<String, Paint>) value).getKey());
				lbl.setOpaque(true);
				lbl.setBackground(isSelected?Color.gray:lbl.getBackground());
				return lbl;
			}});


	}
	public JComboBox getComponent() {
		return this;
	}
	@SuppressWarnings("unchecked")
	public Paint getCurrentValue() {
		return ((Entry<String, Paint>) super.getItemAt(super.getSelectedIndex())).getValue();
	}
}
