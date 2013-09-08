package net.joshuahughes.imagej.parameter;

import java.awt.Color;
import java.awt.Component;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import net.joshuahughes.imagej.JHLabsPlugin;

import com.jhlabs.image.Colormap;


public class ColormapBoxParameter extends JComboBox implements Parameter<Colormap,JComboBox>{
	private static final long serialVersionUID = 726104080593868769L;
	public ColormapBoxParameter(String name){
		setName(name);
		setModel(new DefaultComboBoxModel(JHLabsPlugin.getAll("com.jhlabs",Colormap.class)));
		setRenderer(new ListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(JList list,Object value, int index, boolean isSelected, boolean cellHasFocus) {
				JLabel lbl = new JLabel(value.getClass().getSimpleName());
				lbl.setOpaque(true);
				lbl.setBackground(isSelected?Color.gray:lbl.getBackground());
				return lbl;
			}});

	}
	public JComboBox getComponent() {
		return this;
	}
	public Colormap getCurrentValue() {
		return (Colormap) super.getItemAt(super.getSelectedIndex());
	}
}
