package net.joshuahughes.imagej.parameter;

import java.awt.event.ActionListener;

import javax.swing.JLabel;


public class LabelParameter extends JLabel implements Parameter{
	private static final long serialVersionUID = 726104080593868769L;
	public LabelParameter(String name,String text){
		super("");
		setName(name);
		setText(text);
	}
	public String getCurrentValue() {
		return this.getText();
	}

	public JLabel getComponent() {
		return this;
	}
	@Override
	public void addActionListener(ActionListener al) {
		
	}
}
