package net.joshuahughes.imagej.parameter;

import java.awt.Component;
import java.awt.event.ActionListener;

public interface Parameter {
	public String getName();
	public Object getCurrentValue();
	public Component getComponent();
	public void addActionListener(ActionListener al);
}
