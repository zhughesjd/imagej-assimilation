package net.joshuahughes.imagej.parameter;

import java.awt.event.ActionListener;

import javax.swing.JComponent;

public interface Parameter<T,C extends JComponent> {
	public String getName();
	public T getCurrentValue();
	public C getComponent();
	public void addActionListener(ActionListener al);
}
